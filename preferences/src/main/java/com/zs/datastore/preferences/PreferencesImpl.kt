package com.zs.datastore.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import androidx.datastore.preferences.core.Preferences as StorePrefs

internal class PreferencesImpl(
    context: Context,
    name: String,
    private val scope: CoroutineScope
) : Preferences {
    companion {
        private const val TAG = "PreferencesImpl"
    }

    // ---- Storage ----------------------------------------------------------------------------
    // One DataStore per file. This class must be the only owner of the file: a second
    // DataStore on the same file crashes with "multiple DataStores active for the same file".

    private val file = context.applicationContext.preferencesDataStoreFile(name)

    private val store = PreferenceDataStoreFactory.create(
        // Runs only when the file can't be parsed (CorruptionException). DataStore writes the
        // returned value over the file, then carries on as if it had been empty all along.
        corruptionHandler = ReplaceFileCorruptionHandler { exception ->
            // Keep one copy of the bad file for diagnosis. Each reset overwrites it.
            runCatching { file.copyTo(File(file.path + ".corrupt"), overwrite = true) }
            Log.e(TAG, "Preferences file '$name' was corrupt and has been reset", exception)
            emptyPreferences()
        },
        scope = scope,
        produceFile = { file },
    )

    // ---- Write queue ------------------------------------------------------------------------
    // Writes return immediately and are persisted in the background. Each write waits for the
    // one before it, so the order on disk is exactly the order of the calls, whatever the
    // dispatcher does. `lastWrite` is the tail of that chain; reads wait for it (see `flow`).

    private val lock = Any()
    private var lastWrite: Job? = null   // guarded by lock

    /**
     * Queues [change] behind every earlier write.
     *
     * Inline, so the caller's lambda is compiled into the launched block instead of being
     * allocated as a separate object per write. `crossinline` is required because the lambda
     * runs inside that block.
     */
    private inline fun enqueue(crossinline change: (MutablePreferences) -> Unit) {
        synchronized(lock) {
            val previous = lastWrite
            lastWrite = scope.launch {
                previous?.join()
                try {
                    store.edit { change(it) }
                } catch (e: IOException) {
                    // Without this the exception reaches the scope's crash handler. The queue
                    // keeps going; this one change is lost.
                    Log.e(TAG, "Writing preferences failed", e)
                }
            }
        }
    }

    /** Suspends until every write queued before this call has been persisted. */
    private suspend fun await() {
        // Snapshot of the chain's tail (see enqueue). Joining it covers all earlier writes,
        // because each write waits for its predecessor. Writes queued later are not awaited.
        val tail = synchronized(lock) { lastWrite }
        tail?.join()
    }


    // ---- Read path --------------------------------------------------------------------------
    // Shared by every read (get, contains, observe).
    //
    // Corruption is not handled here. An unparsable file is repaired earlier, by the
    // ReplaceFileCorruptionHandler passed to the DataStore, so it never reaches these operators.
    // What is left is a temporary failure to read the file, which surfaces as an IOException.
    //
    // Strategy: retry briefly, then fall back to defaults. At no point is the file on disk
    // modified, so the stored data is intact once the problem clears.
    private val flow: Flow<StorePrefs> = store.data
        // A transient read error often clears on its own, so retry before giving up: 2 retries,
        // 3 attempts in total, 200 ms apart. Only IOException is retried. Anything else is a
        // real bug and goes straight to the catch below, which rethrows it.
        .retry(retries = 2) { e ->
            (e is IOException).also { if (it) delay(200) }
        }
        // Still failing after the retries: serve "everything is absent" instead of crashing.
        // The emission is required, not cosmetic:
        // - get uses first(), which would throw NoSuchElementException on a flow with no values;
        // - observe would never emit, leaving collectors waiting forever for a first value.
        // emptyPreferences() is only a stand-in handed to readers. It is not written back.
        //
        // Note: a flow ends after the catch emits, so a collector that hits this path sees the
        // defaults and then receives no further updates.
        .catch { e ->
            if (e is IOException) {
                Log.e(TAG, "Reading preferences failed, serving defaults", e)
                emit(emptyPreferences())
            } else throw e   // not an I/O problem: don't hide a bug behind defaults
        }
        // Skips a snapshot equal to the previous one (a map comparison). DataStore already
        // avoids re-emitting for no-op edits, so this is a cheap safeguard, mostly for the
        // fallback emission above. It does NOT stop a write to one key from waking collectors
        // of another key: those are different snapshots. Per-key filtering would have to
        // happen after the key is picked, in `observe`.
        .distinctUntilChanged()
        // Runs once per collector, before it receives anything. Waiting for the queued writes
        // here means the first value of every read, including get and contains, already
        // includes all writes made before it. The Preferences interface relies on this.
        .onStart { await() }

    // Warm up DataStore's in-memory cache as soon as the instance exists, so the first
    // blocking read (get, contains) doesn't wait on disk. Create the instance at app start.
    init {
        scope.launch { flow.first() }
    }

    // ---- Public API -------------------------------------------------------------------------

    override fun <T> observe(key: Preferences.Key<T>): Flow<T?> =
        flow.map { prefs -> prefs[key.actual] }

    override fun <T> set(key: Preferences.Key<T>, value: T) {
        enqueue { it[key.actual] = value }
    }

    override fun <T> remove(key: Preferences.Key<T>) {
        enqueue { it.remove(key.actual) }
    }

    override fun <T> contains(key: Preferences.Key<T>): Boolean = runBlocking {
        flow.map { prefs -> key.actual in prefs }.first()
    }

    override fun clear() {
        enqueue { it.clear() }
    }
}
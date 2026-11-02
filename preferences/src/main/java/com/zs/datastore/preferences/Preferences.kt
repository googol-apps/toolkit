package com.zs.datastore.preferences

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import androidx.datastore.preferences.core.Preferences.Key as StoreKey

// ╔══════════════════════════════════════════════════════════════════════════════╗
// ║  DESIGN NOTE · Why wrap Preferences DataStore instead of building a store?   ║
// ╚══════════════════════════════════════════════════════════════════════════════╝
//
//  WHY A WRAPPER
//  ─────────────
//  • The hard parts (storage, atomic file writes, corruption handling, threading)
//    are already solved and tested by Google's DataStore. Reimplementing them would
//    only add risk, so this module leaves them alone and adds a thin layer on top.
//
//  • A from-scratch store would end up with the same design. The data still has to
//    be read and written through DataStore, and the boxing of values comes with
//    that API, so it can't be avoided at this layer.
//
//  • There are deliberately no typed accessors such as getInt or getString.
//    DataStore doesn't have them, and the typed Key<T> already carries the type.
//
//  SCOPE
//  ─────
//  This layer does two things, and only these:
//
//    1. Simple, non-suspending access with a getter and a setter.
//    2. A saver that converts richer types into simple values for storage, and back
//       (for example an enum stored as its id).
//
//  Anything beyond that is out of scope.

/**
 * Non-suspending access to a single Preferences DataStore file, using typed keys.
 *
 * `Preferences` behaves like a map from [Key] to value that you can read and write without
 * coroutines. An entry is addressed either by a plain [Key], stored as-is, or by a [SaverKey],
 * which converts a richer type to a storable one and back.
 *
 * **Threading**
 * - Reads ([get], [contains]) are synchronous. They block until the first load finishes and
 *   are then served from DataStore's in-memory cache. Preload at app start so a main-thread
 *   read doesn't wait on disk.
 * - Writes ([set], [remove], [clear]) return immediately and are persisted in the background,
 *   in call order. The last few milliseconds can be lost if the process dies.
 * - [get] followed by [set] is not atomic. Two callers doing a read-modify-write can overwrite
 *   each other's change.
 *
 * **Usage**
 * ```
 * val darkMode = booleanPreferencesKey("dark_mode")
 * val theme = intPreferencesKey("theme") by Saver<Int, Theme>(
 *     save = { it.id },
 *     restore = { Theme.fromId(it) },
 * )
 *
 * prefs[darkMode] = true
 * val dark = prefs[darkMode, false]
 * val current = prefs[theme, Theme.System]
 * prefs.observe(theme, Theme.System).collect { ... }
 * ```
 */
interface Preferences {

    /**
     * Key for values stored in Preferences. Type T is the type of the value associated with the
     * Key.
     *
     * T must be one of the following: Boolean, Int, Long, Float, String, Set<String>, Double,
     * ByteArray.
     *
     * Construct Keys for your data type using: [booleanPreferencesKey], [intPreferencesKey],
     * [longPreferencesKey], [floatPreferencesKey], [stringPreferencesKey],
     * [stringSetPreferencesKey], [doublePreferencesKey], [byteArrayPreferencesKey].
     *
     * To store any other type, convert it with [by].
     *
     * Keys are identified by name only. Don't use the same name for two different types, since
     * a value stored under one type can't be read back as the other.
     *
     * This is a thin wrapper around DataStore's own key, so that no androidx type appears in
     * this module's public API.
     */
    @JvmInline
    value class Key<T> internal constructor(private val raw: Any) {

        /** The underlying DataStore key. Internal, so androidx types never reach callers. */
        @Suppress("UNCHECKED_CAST")
        internal val actual: StoreKey<T>
            get() = raw as StoreKey<T>

        /**
         * Builds a [SaverKey] on this key. The compiler checks that the saver's stored type
         * matches the key's type [T]: `intKey("x") by stringSaver` does not compile.
         *
         * ```
         * val themeKey = intKey("theme") by PrefSerializer<Int, Theme>(
         *     save = { it.id },
         *     restore = { Theme.fromId(it) },
         * )
         * ```
         *
         * @param mapper describes how an [Original] value is converted to [T] and back.
         * @return a key that reads and writes [Original] values, stored as [T] under this key.
         */
        infix fun <Original> by(mapper: PrefMapper<T, Original>): SaverKey<T, Original> = mapper(this)
    }

    /**
     * A [key] paired with a conversion between the app-facing type [Original] and the type [T]
     * that is actually stored.
     *
     * Preferences DataStore stores only primitives, so this is how richer types fit in, for
     * example an enum stored as its Int id, or an object stored as a JSON String.
     *
     * Create instances with [Key.by] and `Saver`.
     *
     * Implementation contract:
     * - [write] and [read] must be pure and cheap: no I/O, no side effects. They can run on any
     *   thread, and more than once for the same value.
     * - The round trip must hold: `read(write(x)) == x` for every valid `x`.
     * - Never change how an existing key is encoded. Either make [read] accept both the old and
     *   the new format, or use a new key name and migrate.
     *
     * @param T the stored type of the underlying [key]. See [Key] for the allowed types.
     * @param Original the type that the app reads and writes.
     * @param key the key under which the converted value is stored.
     */
    abstract class SaverKey<T, Original> @PublishedApi internal constructor(
        internal val key: Key<T>
    ) {
        /**
         * Converts the [Original] object into a [T] object that can be saved in the DataStore.
         *
         * @param value the [Original] object that needs to be converted.
         * @return the [T] object that can be saved in the DataStore.
         */
        abstract fun write(value: Original): T

        /**
         * Converts the [T] object back into the [Original] object.
         *
         * [value] comes straight from disk, so it may have been written by an older app version
         * or be corrupted. Return a fallback (for example an enum's default entry) instead of
         * throwing, because an exception breaks every flow that is reading the key.
         *
         * @param value the [T] object that needs to be converted back to the [Original] object.
         * @return the [Original] object.
         */
        abstract fun read(value: T): Original
    }

    // ---- Plain keys ----

    /**
     * Emits the current value of [key], or null if it has none, then every change.
     * The flow doesn't complete on its own.
     *
     * @param key the key for the preference.
     */
    fun <T> observe(key: Key<T>): Flow<T?>

    /**
     * Like [observe], but emits [default] instead of null while [key] has no value.
     *
     * @param key the key for the preference.
     * @param default the value emitted while the key has none.
     */
    fun <T> observe(key: Key<T>, default: T): Flow<T> = observe(key).map { it ?: default }

    /**
     * Gets the value for [key]. If the key is not set, returns null. Use [contains] to check
     * whether a value is present.
     *
     * Blocks the calling thread, see the threading notes on [Preferences].
     *
     * @param key the key for the preference.
     */
    operator fun <T> get(key: Key<T>): T? = runBlocking { observe(key).first() }

    /**
     * Gets the value for [key], or [default] if the key is not set.
     *
     * Blocks the calling thread, like [get].
     *
     * @param key the key for the preference.
     * @param default the value returned if the key has none.
     */
    operator fun <T> get(key: Key<T>, default: T): T = get(key) ?: default

    /**
     * Saves [value] under [key]. Returns immediately; the value is persisted in the background.
     *
     * @param key the key for the preference.
     * @param value the value to store.
     */
    operator fun <T> set(key: Key<T>, value: T)

    /**
     * Removes the entry for [key].
     *
     * @param key the key for the preference.
     */
    fun <T> remove(key: Key<T>)

    /**
     * Checks whether [key] currently has a value.
     *
     * Blocks the calling thread, like [get].
     *
     * @param key the key to check for.
     */
    operator fun <T> contains(key: Key<T>): Boolean

    /**
     * Removes every entry in this file.
     */
    fun clear()

    // ---- Saver keys ----
    // Values are converted with SaverKey.read / write. contains and remove have nothing to
    // convert, so they forward to the underlying key.
    //
    // The observe overloads build on the plain observe(key.key). That relies on the
    // implementation de-duplicating it, so `read` runs only when this key's stored value
    // changes and not on writes to other keys.

    /**
     * Emits the current value of [key], converted by [SaverKey.read], or null if it has none,
     * then every change. The flow doesn't complete on its own.
     *
     * @param key the saver key for the preference.
     */
    fun <T, Original> observe(key: SaverKey<T, Original>): Flow<Original?> =
        observe(key.key).map { value ->
            if (value != null) key.read(value) else null
        }

    /**
     * Like [observe] for a saver key, but emits [default] instead of null while [key] has no
     * value.
     *
     * @param key the saver key for the preference.
     * @param default the value emitted while the key has none.
     */
    fun <T, Original> observe(key: SaverKey<T, Original>, default: Original): Flow<Original> =
        observe(key.key).map { value ->
            if (value == null) default else key.read(value)
        }

    /**
     * Converts [value] with [SaverKey.write] and saves it under the key's underlying entry.
     * Returns immediately; the value is persisted in the background.
     *
     * @param key the saver key for the preference.
     * @param value the original value to store.
     */
    operator fun <T, Original> set(key: SaverKey<T, Original>, value: Original) =
        set(key.key, key.write(value))

    /**
     * Gets the value for [key], converted by [SaverKey.read]. If the key is not set, returns
     * null. Use [contains] to check whether a value is present.
     *
     * Blocks the calling thread, see the threading notes on [Preferences].
     *
     * @param key the saver key for the preference.
     */
    operator fun <T, Original> get(key: SaverKey<T, Original>): Original? =
        runBlocking { observe(key).first() }

    /**
     * Gets the value for [key], or [default] if the key is not set.
     *
     * Blocks the calling thread, like [get].
     *
     * @param key the saver key for the preference.
     * @param default the value returned if the key has none.
     */
    operator fun <T, Original> get(key: SaverKey<T, Original>, default: Original): Original =
        get(key) ?: default

    /**
     * Checks whether [key] currently has a value.
     *
     * Blocks the calling thread, like [get].
     *
     * @param key the saver key to check for.
     */
    operator fun <T, Original> contains(key: SaverKey<T, Original>): Boolean =
        contains(key.key)

    /**
     * Removes the entry for [key].
     *
     * @param key the saver key for the preference.
     */
    fun <T, Original> remove(key: SaverKey<T, Original>) = remove(key.key)

    companion {

        operator fun invoke(
            context: Context,
            name: String = "preferences",
            scope: CoroutineScope
        ): Preferences = PreferencesImpl(context, name, scope)

    }
}
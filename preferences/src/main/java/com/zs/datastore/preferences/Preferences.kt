package com.zs.datastore.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import androidx.datastore.preferences.core.Preferences.Key as StoreKey

/**
 * Type-safe, non-suspend access to one Preferences DataStore file.
 *
 * Entries are addressed by a plain [Key], stored as-is, or by a [SaverKey], whose value is
 * converted on the way in and out. Every read has a nullable form (a missing entry is `null`)
 * and a form that takes a `default`.
 */
interface Preferences {

    // Threading model (applies to every member below):
    // - Reads (get, contains) are synchronous. They block until the first load finishes, then
    //   are served from DataStore's in-memory cache. Preload at app start so a main-thread
    //   read doesn't wait on disk.
    // - Writes (set, remove, clear) return immediately and are persisted in the background,
    //   in call order. The last few milliseconds can be lost if the process dies.
    // - get is built on observe, so an implementation's observe must make its first emission
    //   include every earlier write. That is what lets a read that follows a write see it.
    // - get followed by set is not atomic: two callers doing read-modify-write can overwrite
    //   each other's change. Add an atomic `update` if you need counters or toggles.
    // - A missing entry is null (observe emits null, get returns null); the `default`
    //   overloads turn that into a fallback value.

    /**
     * Type-safe handle for a single preference entry.
     *
     * This is a thin wrapper around DataStore's [StoreKey]. It exists so the androidx type
     * does not appear in this module's public API: consumers only see [Key], and the
     * `datastore-preferences` dependency can stay an implementation detail instead of
     * leaking downstream through `api(...)`.
     *
     * Being a `@JvmInline value class`, it adds no allocation overhead in most cases.
     * [T] is the stored value type (Int, Long, Float, Double, Boolean, String, Set<String>).
     *
     * Instances are created only inside this module, from the factory functions
     * (e.g. [intKey], [stringKey]).
     *
     * @param raw the underlying [StoreKey]. Typed as [Any] on purpose, see below.
     */
    @JvmInline
    value class Key<T> internal constructor(private val raw: Any) {

        // Why `raw` is `Any` and not `StoreKey<T>` (Preferences.Key<T>):
        // a public declaration can't mention a type from a dependency we don't re-export,
        // so typing it as `StoreKey<T>` would leak the androidx type into our public API.
        //
        // Invariant: `raw` is ALWAYS a `StoreKey<T>` created by this module. The constructor
        // is internal so outside code can't break that and cause a ClassCastException
        // at the cast below.

        /** The underlying DataStore key. Internal, so androidx types never reach callers. */
        @Suppress("UNCHECKED_CAST")
        internal val actual: StoreKey<T>
            get() = raw as StoreKey<T>

        // The saver's savable type must be this key's own type [T]. A separate type parameter
        // for it here would let any saver through and fail to compile at `saver(this)` below.

        /**
         * Builds a [SaverKey] on this key. The compiler checks that the saver's savable type
         * matches the key's type [T]: `intKey("x") by stringSaver` does not compile.
         *
         * ```
         * val themeKey = intKey("theme") by PrefSerializer<Int, Theme>(
         *     save = { it.id },
         *     restore = { Theme.fromId(it) },
         * )
         * ```
         */
        infix fun <Original> by(saver: Saver<T, Original>): SaverKey<T, Original> = saver(this)
    }

    /**
     * The `SaverKey` class pairs a plain [Key] holding [Saveable] values with a conversion
     * between objects of type [Original] and objects of type [Saveable].
     *
     * Implementations of this class can be used to simplify the objects before saving them in
     * Android's Preference DataStore, and restore the objects back to their original form
     * after reading them from the DataStore.
     *
     * Create instances with [Key.by] and [PrefSerializer].
     *
     * @author Zakir Sheikh
     * @since 1.0.0
     *
     * @param Saveable The type of the simplified object that can be saved in the DataStore.
     * @param Original The type of the original object that needs to be converted.
     * @param key The key under which the converted value is stored in the DataStore.
     */
    abstract class SaverKey<Saveable, Original> @PublishedApi internal constructor(
        internal val key: Key<Saveable>
    ) {

        // Why the constructor is @PublishedApi: the inline PrefSerializer creates an anonymous
        // subclass at its call sites, which can be in other modules. That makes this signature
        // part of the binary API, so changing it breaks callers compiled against the old one.
        // If construction moves into a published helper class, plain `internal` is enough.
        //
        // Preferences DataStore stores only primitives (Int, Long, Float, Double, Boolean,
        // String, Set<String>). A SaverKey is how richer types, such as an enum saved as its
        // Int id or an object saved as a JSON String, fit into that.
        //
        // Contract for implementers:
        // - save and restore must be pure and cheap: no I/O, no Context, no side effects.
        //   They can run on any thread, and more than once for the same value.
        // - The round trip must hold: restore(save(x)) == x for every valid x.
        // - restore receives whatever is on disk, including values from older app versions or
        //   corrupted data. Prefer a fallback value over throwing, because an exception breaks
        //   every flow that is reading the key.
        // - Never change how an existing key is encoded. Either make restore accept both the
        //   old and new format, or use a new key name and migrate.

        /**
         * Converts the [Original] object into a [Saveable] object that can be saved in the DataStore.
         *
         * @param value The [Original] object that needs to be converted.
         * @return The [Saveable] object that can be saved in the DataStore.
         */
        abstract fun save(value: Original): Saveable

        /**
         * Converts the [Saveable] object back into the [Original] object.
         *
         * @param value The [Saveable] object that needs to be converted back to the [Original] object.
         * @return The [Original] object.
         */
        abstract fun restore(value: Saveable): Original
    }

    // ---- Plain keys ----

    /** Emits the current value of [key], or null if it has none, then every change. */
    fun <T> observe(key: Key<T>): Flow<T?>

    /** Like [observe], but emits [default] instead of null while [key] has no value. */
    fun <T> observe(key: Key<T>, default: T): Flow<T> =
        observe(key).map { it ?: default }
            // "Absent" followed by an explicit value equal to [default] would emit the same
            // value twice in a row. The base observe is already de-duplicated, so this only
            // removes that case.
            .distinctUntilChanged()

    /** Returns the current value of [key], or null if it has none. Blocks, see the note above. */
    operator fun <T> get(key: Key<T>): T? = runBlocking { observe(key).first() }

    /** Returns the current value of [key], or [default] if it has none. Blocks like [get]. */
    operator fun <T> get(key: Key<T>, default: T): T = get(key) ?: default

    /** Saves [value] under [key]. Returns immediately; it is persisted in the background. */
    operator fun <T> set(key: Key<T>, value: T)

    /**
     * Removes the entry for [key].
     *
     * @return `true` if the entry existed.
     */
    fun <T> remove(key: Key<T>): Boolean

    /** Whether [key] currently has a value. Blocks like [get]. */
    operator fun <T> contains(key: Key<T>): Boolean

    /**
     * Removes every entry in this file.
     *
     * @return `true` if anything was removed.
     */
    fun clear(): Boolean

    // ---- Saver keys ----
    // Values are converted with SaverKey.save / restore. contains and remove have nothing
    // to convert, so they forward to the underlying key.

    /** Like [observe] for a plain key, with each value converted by [SaverKey.restore]. */
    fun <Saveable, Original> observe(key: SaverKey<Saveable, Original>): Flow<Original?>

    /** Like [observe] for a saver key, but emits [default] instead of null while [key] has no value. */
    fun <Saveable, Original> observe(key: SaverKey<Saveable, Original>, default: Original): Flow<Original> =
        observe(key).map { it ?: default }

    /** Converts [value] with [SaverKey.save] and saves it under the key's underlying entry. */
    operator fun <Saveable, Original> set(key: SaverKey<Saveable, Original>, value: Original)

    /** Returns the current value of [key], converted by [SaverKey.restore], or null if it has none. Blocks like [get]. */
    operator fun <Saveable, Original> get(key: SaverKey<Saveable, Original>): Original? =
        runBlocking { observe(key).first() }

    /** Like [get], but returns [default] if the key has no value. */
    operator fun <Saveable, Original> get(key: SaverKey<Saveable, Original>, default: Original): Original =
        get(key) ?: default

    /** Whether [key] currently has a value. Blocks like [get]. */
    operator fun <Saveable, Original> contains(key: SaverKey<Saveable, Original>): Boolean =
        contains(key.key)

    /**
     * Removes the entry for [key].
     *
     * @return `true` if the entry existed.
     */
    fun <Saveable, Original> remove(key: SaverKey<Saveable, Original>): Boolean = remove(key.key)
}
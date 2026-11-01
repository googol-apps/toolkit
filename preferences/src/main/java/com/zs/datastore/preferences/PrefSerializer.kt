package com.zs.datastore.preferences


/**
 * A recipe that turns a plain [Preferences.Key] holding [Saveable] values into a
 * [Preferences.SaverKey] that exposes [Original] values.
 * Create one with [PrefSerializer] and apply it with [Preferences.Key.by].
 */
typealias Saver<Saveable, Original> =
            (Preferences.Key<Saveable>) -> Preferences.SaverKey<Saveable, Original>

// Why the lambdas are wrapped in `{ save(it) }` instead of building an anonymous
// SaverKey object right here:
// - Inside an object that has members named save/restore, a call to `save(value)` is
//   ambiguous and can resolve to the member itself, i.e. infinite recursion. Wrapping keeps the
//   parameters named `save`/`restore` (so named-argument call sites keep working) and moves
//   the object into LambdaSaverKey, where the lambdas have different names.
// - An inline parameter can't be stored in a field, hence `crossinline`: it lets the
//   lambda be used from inside the wrapper lambdas, which are not inlined.
// - A public inline function may only call public or @PublishedApi code, which is why
//   LambdaSaverKey is @PublishedApi internal.

/**
 * Describes how an [Original] value is saved as a [Saveable] value.
 * [Saveable] is fixed by the key this is applied to, so the compiler rejects a mismatch,
 * such as an Int key with a String serializer.
 *
 * @param save converts an [Original] value to a [Saveable] one for writing.
 * @param restore converts a [Saveable] value read from disk back to an [Original] one.
 */
inline fun <Saveable, Original> PrefSerializer(
    crossinline write: (Original) -> Saveable,
    crossinline read: (Saveable) -> Original,
): Saver<Saveable, Original> = { key ->
    object : Preferences.SaverKey<Saveable, Original>(key){
        override fun save(value: Original): Saveable = write(value)
        override fun restore(value: Saveable): Original = read(value)
    }
}

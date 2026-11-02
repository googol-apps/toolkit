package com.zs.datastore.preferences


/**
 * A recipe that turns a plain [Preferences.Key] holding [Saveable] values into a
 * [Preferences.SaverKey] that exposes [Original] values.
 * Create one with [PrefSerializer] and apply it with [Preferences.Key.by].
 */
typealias PrefMapper<Savable, Original> = (Preferences.Key<Savable>) -> Preferences.SaverKey<Savable, Original>

/**
 * The [prefMapper] describes how the object of [Original] class can be simplified and converted into
 * something which is [Savable].
 *
 * What types can be saved is defined by [Preferences], by default everything which can be
 * stored in the [Preferences] class can be saved.
 *
 *
 * @param encode Defines how to convert the value into a saveable one. If null is returned the value
 *   will not be saved.
 * @param decode Defines how to convert the restored value back to the original Class. If null is
 *   returned the value will not be restored and would be initialized again instead.
 */
inline fun <Savable, Original> prefMapper(
    crossinline encode: (Original) -> Savable,
    crossinline decode: (Savable) -> Original,
): PrefMapper<Savable, Original> = { key ->
    object : Preferences.SaverKey<Savable, Original>(key){
        override fun write(value: Original): Savable = encode(value)
        override fun read(value: Savable): Original = decode(value)
    }
}

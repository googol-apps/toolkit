package com.zs.datastore.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey as booleanStoreKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey as byteArrayStoreKey
import androidx.datastore.preferences.core.doublePreferencesKey as doubleStoreKey
import androidx.datastore.preferences.core.floatPreferencesKey as floatStoreKey
import androidx.datastore.preferences.core.intPreferencesKey as intStoreKey
import androidx.datastore.preferences.core.longPreferencesKey as longStoreKey
import androidx.datastore.preferences.core.stringPreferencesKey as stringStoreKey
import androidx.datastore.preferences.core.stringSetPreferencesKey as stringSetStoreKey

/**
 * Get a key for an Int preference. You should not have multiple keys with the same name (for use
 * with the same Preferences). Using overlapping keys with different types can result in
 * ClassCastException.
 *
 * @param name the name of the preference
 * @return the Preferences.Key<Int> for [name]
 */
@JvmName("intKey")
public fun intPreferencesKey(name: String): Preferences.Key<Int> = Preferences.Key(intStoreKey(name))
/**
 * Get a key for a Double preference. You should not have multiple keys with the same name (for use
 * with the same Preferences). Using overlapping keys with different types can result in
 * ClassCastException.
 *
 * @param name the name of the preference
 * @return the Preferences.Key<Double> for [name]
 */
@JvmName("doubleKey")
public fun doublePreferencesKey(name: String): Preferences.Key<Double> = Preferences.Key(doubleStoreKey(name))
/**
 * Get a key for a String preference. You should not have multiple keys with the same name (for use
 * with the same Preferences). Using overlapping keys with different types can result in
 * ClassCastException.
 *
 * @param name the name of the preference
 * @return the Preferences.Key<String> for [name]
 */
@JvmName("stringKey")
public fun stringPreferencesKey(name: String): Preferences.Key<String> = Preferences.Key(stringStoreKey(name))
/**
 * Get a key for a Boolean preference. You should not have multiple keys with the same name (for use
 * with the same Preferences). Using overlapping keys with different types can result in
 * ClassCastException.
 *
 * @param name the name of the preference
 * @return the Preferences.Key<Boolean> for [name]
 */
@JvmName("booleanKey")
public fun booleanPreferencesKey(name: String): Preferences.Key<Boolean> = Preferences.Key(booleanStoreKey(name))
/**
 * Get a key for a Float preference. You should not have multiple keys with the same name (for use
 * with the same Preferences). Using overlapping keys with different types can result in
 * ClassCastException.
 *
 * @param name the name of the preference
 * @return the Preferences.Key<Float> for [name]
 */
@JvmName("floatKey")
public fun floatPreferencesKey(name: String): Preferences.Key<Float> = Preferences.Key(floatStoreKey(name))
/**
 * Get a key for a Long preference. You should not have multiple keys with the same name (for use
 * with the same Preferences). Using overlapping keys with different types can result in
 * ClassCastException.
 *
 * @param name the name of the preference
 * @return the Preferences.Key<Long> for [name]
 */
@JvmName("longKey")
public fun longPreferencesKey(name: String): Preferences.Key<Long> = Preferences.Key(longStoreKey(name))
/**
 * Get a key for a String Set preference. You should not have multiple keys with the same name (for
 * use with the same Preferences). Using overlapping keys with different types can result in
 * ClassCastException.
 *
 * Note: sets returned by DataStore are unmodifiable and will throw exceptions if mutated.
 *
 * @param name the name of the preference
 * @return the Preferences.Key<Set<String>> for [name]
 */
@JvmName("stringSetKey")
public fun stringSetPreferencesKey(name: String): Preferences.Key<Set<String>> =
    Preferences.Key(stringSetStoreKey(name))
/**
 * Get a key for a ByteArray preference. You should not have multiple keys with the same name (for
 * use with the same Preferences). Using overlapping keys with different types can result in
 * ClassCastException.
 *
 * Note: ByteArrays returned by DataStore are copies. Mutating their state will do nothing to the
 * underlying data store. They must be set explicitly.
 *
 * @param name the name of the preference
 * @return the Preferences.Key<ByteArray> for [name]
 */
@JvmName("byteArrayKey")
public fun byteArrayPreferencesKey(name: String): Preferences.Key<ByteArray> = Preferences.Key(byteArrayStoreKey(name))







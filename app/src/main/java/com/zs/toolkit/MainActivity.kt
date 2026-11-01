package com.zs.toolkit

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(
    name = "store"
)

val USER_NAME = stringPreferencesKey("user_name")
val DARK_MODE = booleanPreferencesKey("dark_mode")

@JvmInline
public value class StringKey <T> (internal val key: Preferences.Key<T>)

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            DisposableEffect(Unit) {
                onDispose {

                }
            }

        }
    }
}

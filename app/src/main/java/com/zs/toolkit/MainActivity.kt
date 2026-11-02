package com.zs.toolkit

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.lifecycleScope
import com.zs.datastore.preferences.prefMapper
import com.zs.datastore.preferences.Preferences
import com.zs.datastore.preferences.stringPreferencesKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


val USER_NAME = stringPreferencesKey("user_name")

val NUMBER_STRING = stringPreferencesKey("number") by prefMapper(
    encode = {"$it"},
    decode = String::toInt
)

@JvmInline
public value class StringKey <T> (internal val key: Preferences.Key<T>)

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    lateinit var pref: Preferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pref = Preferences(this, "prefs", lifecycleScope)



        lifecycleScope.launch {
            //pref.clear()
            while (true){
                var saver = pref[NUMBER_STRING, 0]
                Log.d(TAG, "onCreate: before = $saver")
                pref[NUMBER_STRING] = saver + 1
                Log.d(TAG, "onCreate: ${pref[NUMBER_STRING]}")
                delay(2000)
            }
        }

        setContent {

            DisposableEffect(Unit) {
                onDispose {

                }
            }

        }
    }
}

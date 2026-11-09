package com.zs.toolkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.button.Button
import com.zs.datastore.preferences.Preferences
import com.zs.datastore.preferences.prefMapper
import com.zs.datastore.preferences.stringPreferencesKey


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

        setContent {
            Box(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background(12.dp)), contentAlignment = Alignment.Center) {
                Column(){
                    Button(
                        onClick = {},
                        icon = {
                            Box(Modifier)
                        },
                        label = {
                           Box() {
                               BasicText("Button1!")
                           }
                        }
                    )
                    Button(
                        onClick = {},
                        icon = {
                            Box(Modifier)
                        },
                        label = {
                            BasicText("Click Me!")
                        }
                    )
                }
            }
        }
    }
}

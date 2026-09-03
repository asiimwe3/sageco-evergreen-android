package com.sagecoevergreen.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.sagecoevergreen.app.ui.SagecoApp
import com.sagecoevergreen.app.ui.theme.SagecoTheme
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val android.content.Context.dataStore by preferencesDataStore("sageco_prefs")
private val AGENT_ID_KEY = stringPreferencesKey("agent_id")

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SagecoTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                var savedAgentId by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val prefs = applicationContext.dataStore.data.first()
                    savedAgentId = prefs[AGENT_ID_KEY]
                }

                SagecoApp(
                    navController = navController,
                    savedAgentId = savedAgentId,
                    onSaveAgentId = { id ->
                        savedAgentId = id
                        scope.launch {
                            applicationContext.dataStore.edit { it[AGENT_ID_KEY] = id }
                        }
                    }
                )
            }
        }
    }
}

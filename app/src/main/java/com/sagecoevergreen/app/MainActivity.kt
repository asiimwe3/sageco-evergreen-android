package com.sagecoevergreen.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.sagecoevergreen.app.data.ApiClient
import com.sagecoevergreen.app.data.AppUpdater
import com.sagecoevergreen.app.ui.SagecoApp
import com.sagecoevergreen.app.ui.screens.UpdateScreen
import com.sagecoevergreen.app.ui.theme.*
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
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
                val scope = rememberCoroutineScope()
                var savedAgentId by remember { mutableStateOf<String?>(null) }

                // One-time version check — gates the whole app on freshness
                LaunchedEffect(Unit) {
                    try {
                        val latest = ApiClient.checkVersion()
                        if (latest != null && latest.versionCode > BuildConfig.BUILD_VERSION_CODE) {
                            AppUpdater.checked = true
                            AppUpdater.latestVersionCode = latest.versionCode
                            AppUpdater.latestNotes = latest.notes
                            AppUpdater.apkUrl = latest.apkUrl
                        } else {
                            AppUpdater.checked = false
                        }
                    } catch (_: Exception) {
                        // Network failed — don't block the user, let them in
                        AppUpdater.checked = false
                    }
                }

                LaunchedEffect(Unit) {
                    val prefs = applicationContext.dataStore.data.first()
                    savedAgentId = prefs[AGENT_ID_KEY]
                }

                when (AppUpdater.checked) {
                    null -> {
                        // Checking version — brief branded splash
                        Box(
                            modifier = Modifier.fillMaxSize().background(SagecoGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Gold)
                        }
                    }
                    true -> UpdateScreen(
                        installedCode = BuildConfig.BUILD_VERSION_CODE,
                        latestCode = AppUpdater.latestVersionCode,
                        notes = AppUpdater.latestNotes,
                        apkUrl = AppUpdater.apkUrl
                    )
                    else -> SagecoApp(
                        navController = rememberNavController(),
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
}

package com.example.playerlock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.playerlock.data.AppDatabase
import com.example.playerlock.data.SettingsDataStore
import com.example.playerlock.progression.ProgressionRepository
import com.example.playerlock.ui.MainScreen
import com.example.playerlock.ui.PlayerLockViewModel
import com.example.playerlock.ui.PlayerSetupScreen
import com.example.playerlock.ui.ProfileSelectionScreen
import com.example.playerlock.ui.theme.PlayerLockTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        lifecycleScope.launch {
            AppDatabase.seedDatabase()
        }
        val repository = ProgressionRepository(database)
        val settingsDataStore = SettingsDataStore(applicationContext)
        val viewModel = PlayerLockViewModel(repository, settingsDataStore)

        setContent {
            val appSettings by viewModel.appSettings.collectAsState()
            val activeProfile by viewModel.activeProfile.collectAsState()

            PlayerLockTheme(
                settings = appSettings,
                activeTeamName = activeProfile?.teamName
            ) {
                val allProfiles by viewModel.allProfiles.collectAsState()
                val activeProfileId by viewModel.activeProfileId.collectAsState()
                var isCreatingNewCareer by remember { mutableStateOf(false) }

                when {
                    allProfiles.isEmpty() || isCreatingNewCareer -> {
                        PlayerSetupScreen(
                            onPlayerCreated = { name, position, ovr, archetype, heightInches, weightLbs, jerseyNum, team, customAttrs, customBadges, avatarPath ->
                                viewModel.createPlayer(name, position, ovr, archetype, heightInches, weightLbs, jerseyNum, team, customAttrs, customBadges, avatarPath)
                                isCreatingNewCareer = false
                            },
                            onSkip = {
                                viewModel.createPlayer("MyPlayer", "PG", 60)
                                isCreatingNewCareer = false
                            },
                            onCareerImported = {
                                isCreatingNewCareer = false
                            },
                            viewModel = viewModel
                        )
                    }
                    activeProfileId == null -> {
                        ProfileSelectionScreen(
                            profiles = allProfiles,
                            onSelectProfile = { id -> viewModel.selectProfile(id) },
                            onNewCareer = { isCreatingNewCareer = true },
                            onDeleteProfile = { id -> viewModel.deleteProfile(id) }
                        )
                    }
                    else -> {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

package com.arv.buscaminas

import DifficultyScreen
import GameBoardScreen
import MenuScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.arv.buscaminas.data.GameViewModel

// Estados simples de navegación
enum class ScreenState { MENU, DIFFICULTY, GAME }

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Navegación simple
                var currentScreen by remember { mutableStateOf(ScreenState.MENU) }

                when (currentScreen) {
                    ScreenState.MENU -> {
                        MenuScreen(
                            onNewGame = { name ->
                                viewModel.username = name
                                currentScreen = ScreenState.DIFFICULTY
                            },
                            onContinue = {
                                if (viewModel.tryLoadGame()) {
                                    currentScreen = ScreenState.GAME
                                }
                            },
                            canContinue = viewModel.hasSavedGame()
                        )
                    }
                    ScreenState.DIFFICULTY -> {
                        DifficultyScreen(onDifficultySelected = { diff ->
                            viewModel.startNewGame(diff, viewModel.username)
                            currentScreen = ScreenState.GAME
                        })
                    }
                    ScreenState.GAME -> {
                        GameBoardScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = ScreenState.MENU }
                        )
                    }
                }
            }
        }
    }
}
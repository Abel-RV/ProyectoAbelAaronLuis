package com.arv.buscaminas.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = StorageManager(application)

    // Estados observables
    var cells = mutableStateListOf<Cell>()
        private set

    var gameStatus by mutableStateOf(GameStatus.PLAYING)
    var rows by mutableStateOf(9)
    var cols by mutableStateOf(9)
    var minesTotal by mutableStateOf(10)
    var flagsPlaced by mutableStateOf(0)
    var username by mutableStateOf("")

    // NUEVO: Temporizador y Debug
    var timeElapsed by mutableStateOf(0L) // Segundos
    var isDebugMode by mutableStateOf(false) // Ver minas
    private var timerJob: Job? = null

    // Iniciar nueva partida
    fun startNewGame(difficulty: Difficulty, user: String) {
        username = user
        rows = difficulty.rows
        cols = difficulty.cols
        minesTotal = difficulty.mines
        gameStatus = GameStatus.PLAYING
        flagsPlaced = 0
        timeElapsed = 0L // Reiniciar tiempo
        isDebugMode = false

        generateBoard()
        startTimer() // Arrancar reloj
    }

    private fun startTimer() {
        timerJob?.cancel() // Cancelar anterior si existe
        timerJob = viewModelScope.launch {
            while (gameStatus == GameStatus.PLAYING) {
                delay(1000)
                timeElapsed++
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

    fun toggleDebug() {
        isDebugMode = !isDebugMode
    }

    private fun generateBoard() {
        cells.clear()
        val tempCells = MutableList(rows * cols) { index ->
            Cell(row = index / cols, col = index % cols)
        }

        var minesPlaced = 0
        while (minesPlaced < minesTotal) {
            val idx = (tempCells.indices).random()
            if (!tempCells[idx].isMine) {
                tempCells[idx] = tempCells[idx].copy(isMine = true)
                minesPlaced++
            }
        }

        cells.addAll(tempCells.map { cell ->
            if (!cell.isMine) {
                cell.copy(neighbors = countMinesAround(cell, tempCells))
            } else cell
        })
    }

    private fun countMinesAround(cell: Cell, allCells: List<Cell>): Int {
        var count = 0
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val r = cell.row + dr
                val c = cell.col + dc
                if (r in 0 until rows && c in 0 until cols) {
                    val idx = r * cols + c
                    if (allCells[idx].isMine) count++
                }
            }
        }
        return count
    }

    fun onCellClick(index: Int) {
        if (gameStatus != GameStatus.PLAYING) return
        val cell = cells[index]
        if (cell.status == CellStatus.FLAGGED || cell.status == CellStatus.REVEALED) return

        if (cell.isMine) {
            gameStatus = GameStatus.LOST
            stopTimer() // Parar reloj
            revealAllMines()
        } else {
            revealCell(index)
            checkWin()
        }
        autoSave()
    }

    fun onCellLongClick(index: Int) {
        if (gameStatus != GameStatus.PLAYING) return
        val cell = cells[index]
        if (cell.status == CellStatus.REVEALED) return

        if (cell.status == CellStatus.HIDDEN) {
            cells[index] = cell.copy(status = CellStatus.FLAGGED)
            flagsPlaced++
        } else if (cell.status == CellStatus.FLAGGED) {
            cells[index] = cell.copy(status = CellStatus.HIDDEN)
            flagsPlaced--
        }
        autoSave()
    }

    private fun revealCell(index: Int) {
        val cell = cells[index]
        if (cell.status != CellStatus.HIDDEN) return

        cells[index] = cell.copy(status = CellStatus.REVEALED)

        if (cell.neighbors == 0) {
            for (dr in -1..1) {
                for (dc in -1..1) {
                    val r = cell.row + dr
                    val c = cell.col + dc
                    if (r in 0 until rows && c in 0 until cols) {
                        val neighborIdx = r * cols + c
                        revealCell(neighborIdx)
                    }
                }
            }
        }
    }

    private fun revealAllMines() {
        cells.forEachIndexed { idx, cell ->
            if (cell.isMine) cells[idx] = cell.copy(status = CellStatus.REVEALED)
        }
        storage.clearSave()
    }

    private fun checkWin() {
        val safeCells = (rows * cols) - minesTotal
        val revealed = cells.count { it.status == CellStatus.REVEALED }
        if (revealed == safeCells) {
            gameStatus = GameStatus.WON
            stopTimer() // Parar reloj
            storage.clearSave()
        }
    }

    fun tryLoadGame(): Boolean {
        val data = storage.loadGame() ?: return false
        username = data.username
        rows = data.rows
        cols = data.cols
        minesTotal = data.totalMines
        gameStatus = data.status
        timeElapsed = data.timeElapsed // Cargar tiempo
        cells.clear()
        cells.addAll(data.cells)
        flagsPlaced = cells.count { it.status == CellStatus.FLAGGED }

        if (gameStatus == GameStatus.PLAYING) startTimer() // Reanudar reloj
        return true
    }

    fun hasSavedGame() = storage.hasSavedGame()

    private fun autoSave() {
        if (gameStatus == GameStatus.PLAYING) {
            // Guardamos el tiempo actual
            storage.saveGame(SaveData(username, cells, rows, cols, minesTotal, gameStatus, timeElapsed))
        }
    }
}
package com.arv.buscaminas.data
enum class CellStatus { HIDDEN, REVEALED, FLAGGED }
enum class GameStatus { PLAYING, WON, LOST }

data class Cell(
    val row: Int,
    val col: Int,
    val isMine: Boolean = false,
    var status: CellStatus = CellStatus.HIDDEN,
    var neighbors: Int = 0
)

sealed class Difficulty(val rows: Int, val cols: Int, val mines: Int) {
    // FÁCIL: 8 de ancho (muy grandes)
    data object Easy : Difficulty(10, 8, 10)

    // NORMAL: 10 de ancho (cómodo) - Más filas para compensar
    data object Normal : Difficulty(16, 10, 25)

    // DIFÍCIL: 12 de ancho (límite para dedos) - Muy alto
    data object Hard : Difficulty(24, 12, 50)

    data class Custom(val r: Int, val c: Int, val m: Int) : Difficulty(r, c, m)
}

data class SaveData(
    val username: String,
    val cells: List<Cell>,
    val rows: Int,
    val cols: Int,
    val totalMines: Int,
    val status: GameStatus,
    val timeElapsed: Long
)

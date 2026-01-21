import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arv.buscaminas.data.Cell
import com.arv.buscaminas.data.CellStatus
import com.arv.buscaminas.data.Difficulty
import com.arv.buscaminas.data.GameStatus
import com.arv.buscaminas.data.GameViewModel

fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

// --- MENU SCREEN ---
@Composable
fun MenuScreen(
    onNewGame: (String) -> Unit,
    onContinue: () -> Unit,
    canContinue: Boolean
) {
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .systemBarsPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("💣 FEAR OF BOMB", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A90E2))
        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Nombre de usuario") }, singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color(0xFFEEEEEE)),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onNewGame(name.ifBlank { "Jugador" }) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
        ) { Text("Nueva Partida", fontSize = 18.sp) }

        if (canContinue) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE94560))
            ) { Text("Continuar Partida", fontSize = 18.sp) }
        }
    }
}

// --- DIFFICULTY SCREEN (Límites Estrictos) ---
@Composable
fun DifficultyScreen(onDifficultySelected: (Difficulty) -> Unit) {
    var showCustom by remember { mutableStateOf(false) }
    var cRows by remember { mutableStateOf("15") }
    var cCols by remember { mutableStateOf("10") } // Default seguro
    var cMines by remember { mutableStateOf("20") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .systemBarsPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dificultad", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(30.dp))

        val btnMod = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(50.dp)

        Button(onClick = { onDifficultySelected(Difficulty.Easy) }, modifier = btnMod) { Text("Fácil (8x10)") }
        Button(onClick = { onDifficultySelected(Difficulty.Normal) }, modifier = btnMod) { Text("Normal (10x16)") }
        Button(onClick = { onDifficultySelected(Difficulty.Hard) }, modifier = btnMod) { Text("Difícil (12x24)") }

        OutlinedButton(
            onClick = { showCustom = !showCustom },
            modifier = btnMod,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) { Text("Personalizado") }

        if (showCustom) {
            Card(
                modifier = Modifier.padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Máx columnas: 12", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom=8.dp))
                    Row {
                        TextField(value = cRows, onValueChange = { cRows = it }, label = { Text("Filas") }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        TextField(value = cCols, onValueChange = { cCols = it }, label = { Text("Cols") }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    TextField(value = cMines, onValueChange = { cMines = it }, label = { Text("Minas") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            // FORZAMOS LÍMITES PARA QUE SE VEA BIEN
                            val c = (cCols.toIntOrNull() ?: 8).coerceIn(4, 12) // Máximo 12 ancho
                            val r = (cRows.toIntOrNull() ?: 10).coerceIn(5, 30) // Alto libre
                            val maxMines = (r * c) - 1
                            val m = (cMines.toIntOrNull() ?: 10).coerceIn(1, maxMines)

                            onDifficultySelected(Difficulty.Custom(r, c, m))
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) { Text("Jugar Custom") }
                }
            }
        }
    }
}

// --- GAME BOARD (Sin Scroll - Ajuste Perfecto) ---
@Composable
fun GameBoardScreen(viewModel: GameViewModel, onBack: () -> Unit) {
    val cells = viewModel.cells
    val cols = viewModel.cols

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF222222))
            .statusBarsPadding()
    ) {
        // HEADER
        Surface(color = Color(0xFF16213E), shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("👤 ${viewModel.username}", color = Color.White, fontSize = 12.sp)
                    Text("🚩 ${viewModel.flagsPlaced}/${viewModel.minesTotal}", color = Color(0xFFE94560), fontWeight = FontWeight.Bold)
                }
                Text("⏱️ ${formatTime(viewModel.timeElapsed)}", color = Color.Yellow, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = { viewModel.toggleDebug() }) {
                        Text(if (viewModel.isDebugMode) "👁️" else "🔒", fontSize = 20.sp)
                    }
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)), contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(35.dp)) {
                        Text("SALIR", fontSize = 10.sp)
                    }
                }
            }
        }

        if (viewModel.gameStatus != GameStatus.PLAYING) {
            val bgStatus = if(viewModel.gameStatus == GameStatus.WON) Color(0xFF4CAF50) else Color(0xFFE94560)
            Box(modifier = Modifier.fillMaxWidth().background(bgStatus).padding(8.dp)) {
                Text(
                    text = if(viewModel.gameStatus == GameStatus.WON) "🎉 ¡VICTORIA! 🎉" else "💥 ¡EXPLOSIÓN! 💥",
                    modifier = Modifier.align(Alignment.Center),
                    fontWeight = FontWeight.Bold, color = Color.White
                )
            }
        }

        // --- GRID QUE OCUPA TODO EL ANCHO SIN SCROLL ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(4.dp), // Un pequeño margen externo
            contentAlignment = Alignment.Center // Centramos el tablero verticalmente si sobra espacio
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols), // Divide el ancho exacto entre columnas
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cells.size) { index ->
                    CellView(
                        cell = cells[index],
                        isDebug = viewModel.isDebugMode,
                        onClick = { viewModel.onCellClick(index) },
                        onLongClick = { viewModel.onCellLongClick(index) }
                    )
                }
            }
        }

        Text("Toque: Cavar | Mantener: Bandera", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally).navigationBarsPadding().padding(bottom = 8.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CellView(cell: Cell, isDebug: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val bgColor = when {
        cell.status == CellStatus.REVEALED && cell.isMine -> Color(0xFFE94560)
        cell.status == CellStatus.REVEALED -> Color(0xFFDDDDDD)
        cell.status == CellStatus.HIDDEN && cell.isMine && isDebug -> Color(0xFFFFA500)
        cell.status == CellStatus.FLAGGED -> Color(0xFF16213E)
        else -> Color(0xFF30475E)
    }

    // CUADRADO PERFECTO AUTOMÁTICO
    Box(
        modifier = Modifier
            .fillMaxWidth() // Llena el ancho de su columna
            .aspectRatio(1f) // Fuerza que la altura sea igual al ancho
            .background(bgColor, RoundedCornerShape(3.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        if (cell.status == CellStatus.FLAGGED) {
            Text("🚩", fontSize = 16.sp)
        } else if (cell.status == CellStatus.REVEALED) {
            if (cell.isMine) {
                Text("💣", fontSize = 16.sp)
            } else if (cell.neighbors > 0) {
                val numColor = when(cell.neighbors) {
                    1 -> Color(0xFF1976D2); 2 -> Color(0xFF388E3C); 3 -> Color(0xFFD32F2F); 4 -> Color(0xFF7B1FA2); else -> Color.Black
                }
                // Tamaño de fuente ajustado para que no se salga
                Text("${cell.neighbors}", fontWeight = FontWeight.Black, color = numColor, fontSize = 18.sp)
            }
        } else if (isDebug && cell.isMine) {
            Text("💣", fontSize = 10.sp, color = Color.Black.copy(alpha = 0.5f))
        }
    }
}
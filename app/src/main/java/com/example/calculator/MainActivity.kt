package com.example.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// ── Colors ────────────────────────────────────────────────────────────────────

private val BgColor       = Color(0xFF1F172E)
private val BtnColor      = Color(0xFF84789C)
private val BtnHover      = Color(0xFFBEB2D6)
private val ExtraBtn      = Color(0xFF5C4F72)
private val DisplayBg     = Color(0xFFFFFFFF)
private val DisplayText   = Color(0xFF1F172E)
private val StoryColor    = Color(0xFFB0A8C8)

// ── Activity ──────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculatorApp()
        }
    }
}

// ── Root composable ───────────────────────────────────────────────────────────

@Composable
fun CalculatorApp(vm: CalculatorViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    var extraOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(8.dp)
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.8f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcButton(
                    text = "☰",
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    color = BtnColor,
                    fontSize = 24
                ) { extraOpen = !extraOpen }

                CalcButton(
                    text = "↷",
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    color = BtnColor,
                    enabled = vm.canRedo
                ) { vm.redo() }
            }

            // ── Story (history) line ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.8f),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = state.story,
                        color = StoryColor,
                        fontSize = 32.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // ── Display ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.8f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DisplayBg)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = state.display,
                    color = DisplayText,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Button grid ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(5f),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Main 4-column grid
                Column(
                    modifier = Modifier.weight(4f),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    val rows = listOf(
                        listOf(
                            Btn("CE", BtnColor) { vm.undo() },
                            Btn("C",  BtnColor) { vm.onClear() },
                            Btn("⌫",  BtnColor) { vm.onBackspace() },
                            Btn("÷",  BtnColor) { vm.onOperator("÷") },
                        ),
                        listOf(
                            Btn("7", BtnColor) { vm.onDigit("7") },
                            Btn("8", BtnColor) { vm.onDigit("8") },
                            Btn("9", BtnColor) { vm.onDigit("9") },
                            Btn("×", BtnColor) { vm.onOperator("×") },
                        ),
                        listOf(
                            Btn("4", BtnColor) { vm.onDigit("4") },
                            Btn("5", BtnColor) { vm.onDigit("5") },
                            Btn("6", BtnColor) { vm.onDigit("6") },
                            Btn("−", BtnColor) { vm.onOperator("−") },
                        ),
                        listOf(
                            Btn("1", BtnColor) { vm.onDigit("1") },
                            Btn("2", BtnColor) { vm.onDigit("2") },
                            Btn("3", BtnColor) { vm.onDigit("3") },
                            Btn("+", BtnColor) { vm.onOperator("+") },
                        ),
                        listOf(
                            Btn("00", BtnColor) { vm.onDigit("00") },
                            Btn("0",  BtnColor) { vm.onDigit("0") },
                            Btn(".",  BtnColor) { vm.onPoint() },
                            Btn("=",  BtnColor) { vm.onEquals() },
                        ),
                    )

                    rows.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            row.forEach { btn ->
                                CalcButton(
                                    text = btn.label,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    color = btn.color,
                                    onClick = btn.action
                                )
                            }
                        }
                    }
                }

                // Extra scientific column
                AnimatedVisibility(
                    visible = extraOpen,
                    enter = expandHorizontally(),
                    exit = shrinkHorizontally()
                ) {
                    Column(
                        modifier = Modifier
                            .width(72.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        listOf(
                            Btn("π",  ExtraBtn) { vm.onPi() },
                            Btn("e",  ExtraBtn) { vm.onE() },
                            Btn("√x", ExtraBtn) { vm.onSqrt() },
                            Btn("^",  ExtraBtn) { vm.onPow() },
                            Btn("ln", ExtraBtn) { vm.onLn() },
                        ).forEach { btn ->
                            CalcButton(
                                text = btn.label,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                color = btn.color,
                                fontSize = 18,
                                onClick = btn.action
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Button helpers ────────────────────────────────────────────────────────────

private data class Btn(val label: String, val color: Color, val action: () -> Unit)

@Composable
fun CalcButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = BtnColor,
    fontSize: Int = 24,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.padding(4.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = color.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(4.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1
        )
    }
}
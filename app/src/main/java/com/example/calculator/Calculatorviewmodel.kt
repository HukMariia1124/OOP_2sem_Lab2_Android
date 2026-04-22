package com.example.calculator

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Stack

class CalculatorViewModel : ViewModel() {

    private var _state = CalcState.Initial
    private val _uiState = MutableStateFlow(_state)
    val uiState = _uiState.asStateFlow()

    private val undoStack = Stack<ICalculatorCommand>()
    private val redoStack = Stack<ICalculatorCommand>()

    val canUndo get() = undoStack.isNotEmpty()
    val canRedo get() = redoStack.isNotEmpty()

    private fun run(cmd: ICalculatorCommand) {
        _state = cmd.execute(_state)
        undoStack.push(cmd)
        redoStack.clear()
        _uiState.value = _state
    }

    private fun runNoUndo(cmd: ICalculatorCommand) {
        _state = cmd.execute(_state)
        undoStack.clear()
        redoStack.clear()
        _uiState.value = _state
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val cmd = undoStack.pop()
        redoStack.push(cmd)
        _state = cmd.previousState
        _uiState.value = _state
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val cmd = redoStack.pop()
        _state = cmd.execute(cmd.previousState)
        undoStack.push(cmd)
        _uiState.value = _state
    }

    fun onDigit(digit: String) = run(DigitCommand(_state, digit))
    fun onOperator(op: String) = run(BinaryOperatorCommand(_state, op))
    fun onEquals() = run(EqualsCommand(_state))
    fun onClear() = runNoUndo(ClearAllCommand(_state))
    fun onBackspace() = run(BackspaceCommand(_state))
    fun onPoint() = run(PointCommand(_state))
    fun onSqrt() = run(UnaryCommand(_state, "√"))
    fun onLn() = run(UnaryCommand(_state, "ln"))
    fun onPi() = run(UnaryCommand(_state, "π"))
    fun onE() = run(UnaryCommand(_state, "e"))
    fun onPow() = run(BinaryOperatorCommand(_state, "^"))
    fun onNegate() = run(NegateCommand(_state))
    fun onAbs() = run(AbsCommand(_state))
    fun onPow10() = run(Pow10Command(_state))
    fun onFactorial() = run(FactorialCommand(_state))
    fun onModulo() = run(ModuloCommand(_state))
}
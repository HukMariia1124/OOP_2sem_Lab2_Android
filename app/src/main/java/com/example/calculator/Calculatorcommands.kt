package com.example.calculator

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

// ── State ────────────────────────────────────────────────────────────────────

data class CalcState(
    val display: String = "0",
    val story: String = "",
    val accumulator: Double? = null,
    val operator: String? = null,
    val freshOperand: Boolean = true,
    val justEquals: Boolean = false,
    val rawInput: String = "0",
    val displayLabel: String? = null
) {
    companion object {
        val Initial = CalcState()
    }
}

// ── Command interface ─────────────────────────────────────────────────────────

interface ICalculatorCommand {
    val previousState: CalcState
    fun execute(current: CalcState): CalcState
}

// ── Helpers ───────────────────────────────────────────────────────────────────

internal fun formatNumber(v: Double): String = when {
    v.isNaN() -> "Error"
    v.isInfinite() -> "∞"
    else -> v.toBigDecimal().stripTrailingZeros()
        .toPlainString()
        .let {
            if (v == v.toLong().toDouble() && !v.isInfinite())
                v.toLong().toString()
            else
                "%.11G".format(v).trimEnd('0').trimEnd('.')
        }
}

internal fun evaluate(left: Double, right: Double, op: String): Double = when (op) {
    "+" -> left + right
    "−" -> left - right
    "×" -> left * right
    "÷" -> if (right == 0.0) throw ArithmeticException("Division by zero") else left / right
    "^" -> left.pow(right)
    else -> right
}

private fun parseRaw(raw: String) = raw.toDouble()

// ── Commands ──────────────────────────────────────────────────────────────────

class DigitCommand(override val previousState: CalcState, private val digit: String) : ICalculatorCommand {
    override fun execute(current: CalcState): CalcState {
        var s = current

        // Автомноження: після константи (π, e) одразу йде цифра → вставляємо ×
        if (s.displayLabel != null && s.operator == null && !s.justEquals) {
            s = s.copy(
                accumulator = parseRaw(s.rawInput),
                operator = "×",
                story = s.displayLabel + " ×",
                freshOperand = true,
                displayLabel = null
            )
        }

        val raw: String
        if (s.freshOperand || s.justEquals) {
            raw = if (digit == "00") "0" else digit
        } else {
            val currentRaw = s.rawInput
            if (currentRaw == "0" && digit == "00") return s
            val digitCount = currentRaw.replace("-", "").replace(".", "").length
            val adding = if (digit == "00") 2 else 1
            if (digitCount + adding > 11) return s
            raw = if (currentRaw == "0") digit else currentRaw + digit
        }

        return s.copy(
            display = raw,
            rawInput = raw,
            story = if (s.justEquals) "" else s.story,
            freshOperand = false,
            justEquals = false,
            displayLabel = null
        )
    }
}

class PointCommand(override val previousState: CalcState) : ICalculatorCommand {
    override fun execute(current: CalcState): CalcState {
        val s = current
        val raw = if (s.freshOperand || s.justEquals) "0" else s.rawInput
        if (raw.contains('.')) return s.copy(freshOperand = false, justEquals = false)
        val newRaw = "$raw."
        return s.copy(
            display = newRaw,
            rawInput = newRaw,
            story = if (s.justEquals) "" else s.story,
            freshOperand = false,
            justEquals = false,
            displayLabel = null
        )
    }
}

class BinaryOperatorCommand(override val previousState: CalcState, private val op: String) : ICalculatorCommand {
    override fun execute(current: CalcState): CalcState {
        val s = current
        val currentVal = parseRaw(s.rawInput)

        val freshWithoutUnary = s.freshOperand && s.displayLabel == null

        val acc: Double
        if (s.justEquals || s.accumulator == null || freshWithoutUnary) {
            acc = currentVal
        } else {
            acc = try {
                evaluate(s.accumulator, currentVal, s.operator!!)
            } catch (e: ArithmeticException) {
                return s.copy(display = "Error", story = "", accumulator = null, operator = null, freshOperand = true, justEquals = true)
            }
        }

        val accLabel = if (s.justEquals || s.accumulator == null || freshWithoutUnary)
            s.displayLabel ?: formatNumber(acc)
        else
            formatNumber(acc)

        val story = "$accLabel $op"

        return s.copy(
            display = formatNumber(acc),
            story = story,
            accumulator = acc,
            operator = op,
            freshOperand = true,
            justEquals = false,
            rawInput = acc.toString(),
            displayLabel = null
        )
    }
}

class EqualsCommand(override val previousState: CalcState) : ICalculatorCommand {
    override fun execute(current: CalcState): CalcState {
        val s = current
        if (s.accumulator == null || s.operator == null)
            return s.copy(story = "", justEquals = true)

        val right = parseRaw(s.rawInput)
        val rightLabel = s.displayLabel ?: formatNumber(right)
        val story = "${s.story} $rightLabel ="

        val result = try {
            evaluate(s.accumulator, right, s.operator)
        } catch (e: ArithmeticException) {
            return s.copy(display = "Error", story = story, accumulator = null, operator = null, freshOperand = true, justEquals = true)
        }

        return s.copy(
            display = formatNumber(result),
            story = story,
            accumulator = null,
            operator = null,
            freshOperand = true,
            justEquals = true,
            rawInput = result.toString(),
            displayLabel = null
        )
    }
}

class BackspaceCommand(override val previousState: CalcState) : ICalculatorCommand {
    override fun execute(current: CalcState): CalcState {
        val s = current
        if (s.freshOperand || s.justEquals) return s
        var raw = if (s.rawInput.length <= 1) "0" else s.rawInput.dropLast(1)
        if (raw == "-") raw = "0"
        return s.copy(display = raw, rawInput = raw)
    }
}

class UnaryCommand(override val previousState: CalcState, private val func: String) : ICalculatorCommand {
    override fun execute(current: CalcState): CalcState {
        val s = current
        val x = parseRaw(s.rawInput)
        val currentToken = s.displayLabel ?: formatNumber(x)

        val result: Double
        val label: String

        try {
            when (func) {
                "√" -> {
                    if (x < 0) throw IllegalArgumentException("√ of negative")
                    result = sqrt(x)
                    label = "√($currentToken)"
                }
                "ln" -> {
                    if (x <= 0) throw IllegalArgumentException("ln of non-positive")
                    result = ln(x)
                    label = "ln($currentToken)"
                }
                "π" -> { result = Math.PI; label = "π" }
                "e" -> { result = Math.E; label = "e" }
                else -> return s
            }
        } catch (e: Exception) {
            return s.copy(display = e.message ?: "Error", story = "", freshOperand = true, justEquals = true)
        }

        // Автомноження: число вже введено, оператора немає → число × константа
        val isConstant = func == "π" || func == "e"
        val numberEntered = !s.freshOperand && !s.justEquals && s.operator == null && s.accumulator == null

        if (isConstant && numberEntered) {
            val multiplied = x * result
            val multStory = "${formatNumber(x)} × $label ="
            return s.copy(
                display = formatNumber(multiplied),
                story = multStory,
                accumulator = null,
                operator = null,
                freshOperand = true,
                justEquals = true,
                rawInput = multiplied.toString(),
                displayLabel = null
            )
        }

        val newStory = if (s.operator != null && !s.justEquals) s.story else label

        return s.copy(
            display = formatNumber(result),
            story = newStory,
            accumulator = if (s.justEquals) null else s.accumulator,
            operator = if (s.justEquals) null else s.operator,
            freshOperand = true,
            justEquals = false,
            rawInput = result.toString(),
            displayLabel = label
        )
    }
}

class ClearAllCommand(override val previousState: CalcState) : ICalculatorCommand {
    override fun execute(current: CalcState) = CalcState.Initial
}
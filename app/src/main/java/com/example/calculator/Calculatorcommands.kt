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
    "mod" -> if (right == 0.0) throw ArithmeticException("Division by zero") else left % right
    else -> right
}

private fun parseRaw(raw: String) = raw.toDouble()

// ── Commands ──────────────────────────────────────────────────────────────────

class DigitCommand(override val previousState: CalcState, private val digit: String) : ICalculatorCommand {
    override fun execute(current: CalcState): CalcState {
        var s = current

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
    override fun execute(s: CalcState): CalcState {

        if (func == "π" || func == "e")
        {
            val constVal = if (func == "π") Math.PI else Math.E
            val strVal = String.format(java.util.Locale.US, "%.10f", constVal).trimEnd('0')
            return s.copy(
                display = strVal,
                rawInput = strVal,
                freshOperand = true,
                justEquals = false,
                displayLabel = null,
                story = if (s.justEquals) "" else s.story
            )
        }
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
                else -> return s
            }
        } catch (e: Exception) {
            return s.copy(display = e.message ?: "Error", freshOperand = true, justEquals = true)
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

class NegateCommand(override val previousState: CalcState) : ICalculatorCommand {
    override fun execute(s: CalcState): CalcState {
        val x = parseRaw(s.rawInput)
        if (x == 0.0) return s
        val result = -x
        return s.copy(
            display = formatNumber(result),
            rawInput = result.toString(),
            displayLabel = s.displayLabel?.let { "negate($it)" }
        )
    }
}

class AbsCommand(override val previousState: CalcState) : ICalculatorCommand {
    override fun execute(s: CalcState): CalcState {
        val x = parseRaw(s.rawInput)
        val result = kotlin.math.abs(x)
        val currentToken = s.displayLabel ?: formatNumber(x)
        val label = "|$currentToken|"
        val newStory = if (s.operator != null && !s.justEquals) s.story else label
        return s.copy(
            display = formatNumber(result),
            story = newStory,
            rawInput = result.toString(),
            freshOperand = true,
            justEquals = false,
            displayLabel = label
        )
    }
}

class Pow10Command(override val previousState: CalcState) : ICalculatorCommand {
    override fun execute(s: CalcState): CalcState {
        val x = parseRaw(s.rawInput)
        val result = 10.0.pow(x)
        val currentToken = s.displayLabel ?: formatNumber(x)
        val label = "10^($currentToken)"
        val newStory = if (s.operator != null && !s.justEquals) s.story else label
        return s.copy(
            display = formatNumber(result),
            story = newStory,
            rawInput = result.toString(),
            freshOperand = true,
            justEquals = false,
            displayLabel = label
        )
    }
}

class FactorialCommand(override val previousState: CalcState) : ICalculatorCommand {
    override fun execute(s: CalcState): CalcState {
        val x = parseRaw(s.rawInput)
        val currentToken = s.displayLabel ?: formatNumber(x)
        val label = "$currentToken!"
        if (x < 0 || x != kotlin.math.floor(x) || x > 170)
            return s.copy(display = "Invalid input for n!", freshOperand = true, justEquals = true)
        var result = 1.0
        for (i in 2..x.toInt()) result *= i
        val newStory = if (s.operator != null && !s.justEquals) s.story else label
        return s.copy(
            display = formatNumber(result),
            story = newStory,
            rawInput = result.toString(),
            freshOperand = true,
            justEquals = false,
            displayLabel = label
        )
    }
}

class ModuloCommand(override val previousState: CalcState) : ICalculatorCommand {
    override fun execute(s: CalcState): CalcState {
        val currentVal = parseRaw(s.rawInput)
        val freshWithoutUnary = s.freshOperand && s.displayLabel == null

        val acc: Double = if (s.justEquals || s.accumulator == null || freshWithoutUnary) {
            currentVal
        } else {
            try {
                evaluate(s.accumulator, currentVal, s.operator!!)
            } catch (e: ArithmeticException) {
                return s.copy(display = "Error", story = "", accumulator = null, operator = null, freshOperand = true, justEquals = true)
            }
        }

        val accLabel = if (s.justEquals || s.accumulator == null || freshWithoutUnary)
            s.displayLabel ?: formatNumber(acc)
        else
            formatNumber(acc)

        return s.copy(
            display = formatNumber(acc),
            story = "$accLabel mod",
            accumulator = acc,
            operator = "mod",
            freshOperand = true,
            justEquals = false,
            rawInput = acc.toString(),
            displayLabel = null
        )
    }
}
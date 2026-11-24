package com.creazzy.rofof.feature.smartentry

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Small offline arithmetic parser. Supports decimal numbers, parentheses and
 * + - * / with normal operator precedence. It never evaluates Kotlin/JS code.
 */
object MathExpressionEvaluator {
    fun evaluate(source: String): BigDecimal? {
        if (source.isBlank() || source.length > 128) return null
        return runCatching {
            Parser(source.replace(',', '.')).parse().stripTrailingZeros()
                .also { require(it.precision() <= 18 && it.scale() <= 8) }
        }.getOrNull()
    }

    private class Parser(private val input: String) {
        private var index = 0
        private var operations = 0

        fun parse(): BigDecimal {
            val result = expression(0)
            whitespace()
            require(index == input.length) { "Unexpected character" }
            require(result.signum() > 0) { "Amount must be positive" }
            return result
        }

        private fun expression(depth: Int): BigDecimal {
            guardDepth(depth)
            var value = term(depth + 1)
            while (true) {
                whitespace()
                val op = peek()
                if (op != '+' && op != '-') return value
                consumeOperation()
                index++
                val rhs = term(depth + 1)
                value = if (op == '+') value.add(rhs) else value.subtract(rhs)
            }
        }

        private fun term(depth: Int): BigDecimal {
            guardDepth(depth)
            var value = factor(depth + 1)
            while (true) {
                whitespace()
                val op = peek()
                if (op != '*' && op != '/') return value
                consumeOperation()
                index++
                val rhs = factor(depth + 1)
                value = if (op == '*') value.multiply(rhs)
                else value.divide(rhs, 8, RoundingMode.HALF_UP)
            }
        }

        private fun factor(depth: Int): BigDecimal {
            guardDepth(depth)
            whitespace()
            when (peek()) {
                '+' -> { index++; return factor(depth + 1) }
                '-' -> { index++; return factor(depth + 1).negate() }
                '(' -> {
                    index++
                    val value = expression(depth + 1)
                    whitespace()
                    require(peek() == ')') { "Missing closing parenthesis" }
                    index++
                    return value
                }
            }
            val start = index
            var decimalSeen = false
            while (index < input.length) {
                val ch = input[index]
                when {
                    ch.isDigit() -> index++
                    ch == '.' && !decimalSeen -> { decimalSeen = true; index++ }
                    else -> break
                }
            }
            require(index > start) { "Number expected" }
            return BigDecimal(input.substring(start, index))
        }

        private fun guardDepth(depth: Int) {
            require(depth <= 16) { "Expression nesting is too deep" }
        }

        private fun consumeOperation() {
            operations++
            require(operations <= 32) { "Expression has too many operations" }
        }

        private fun whitespace() {
            while (index < input.length && input[index].isWhitespace()) index++
        }

        private fun peek(): Char? = input.getOrNull(index)
    }
}

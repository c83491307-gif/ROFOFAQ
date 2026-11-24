package com.creazzy.rofof.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.creazzy.rofof.data.local.CategoryEntity
import com.creazzy.rofof.data.local.CategoryExpenseSum
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private val DonutPalette = listOf(
    Color(0xFF147D64), Color(0xFF3976B9), Color(0xFFE0A33A),
    Color(0xFFB45B52), Color(0xFF8064A8), Color(0xFF3F9A9A),
    Color(0xFF9B7653), Color(0xFF6D8792)
)

/** Draws expense distribution for one currency; currencies are never combined. */
@Composable
fun DonutChart(
    data: List<CategoryExpenseSum>,
    categories: List<CategoryEntity>,
    currencyCode: String,
    currencyExponent: Int,
    modifier: Modifier = Modifier,
    language: String = "ar",
    emptyLabel: String = if (language == "ar") "لا توجد مصروفات بعد" else "No expenses yet"
) {
    val currencyData = data.filter {
        it.currencyCode == currencyCode &&
            it.currencyExponent == currencyExponent &&
            it.totalMinor > 0L
    }.sortedByDescending { it.totalMinor }

    val totalMinor = currencyData.fold(0L) { total, item ->
        Math.addExact(total, item.totalMinor)
    }

    if (currencyData.isEmpty() || totalMinor <= 0L) {
        Box(
            modifier = modifier.fillMaxWidth().height(156.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(emptyLabel, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val categoryNames = categories.associate { it.id to it.name }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(modifier = Modifier.size(152.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(144.dp)) {
                var startAngle = -90f
                currencyData.forEach { item ->
                    val sweep = (item.totalMinor.toDouble() / totalMinor.toDouble() * 360.0).toFloat()
                    drawArc(
                        color = donutColor(item.categoryId),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (language == "ar") "الإجمالي" else "Total",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    formatDonutAmount(totalMinor, currencyCode, currencyExponent, language),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            currencyData.take(5).forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(9.dp)) {
                        drawCircle(donutColor(item.categoryId))
                    }
                    Spacer(Modifier.width(7.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            categoryNames[item.categoryId] ?: if (language == "ar") "أخرى" else "Other",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${percentage(item.totalMinor, totalMinor)}% · ${formatDonutAmount(item.totalMinor, currencyCode, currencyExponent, language)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (currencyData.size > 5) {
                Text(
                    if (language == "ar") "و ${currencyData.size - 5} فئات أخرى" else "and ${currencyData.size - 5} more",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun donutColor(categoryId: String): Color {
    val index = (categoryId.hashCode() and Int.MAX_VALUE) % DonutPalette.size
    return DonutPalette[index]
}

private fun percentage(amount: Long, total: Long): Int =
    ((amount.toDouble() / total.toDouble()) * 100.0).toInt()

private fun formatDonutAmount(
    amountMinor: Long,
    currencyCode: String,
    exponent: Int,
    language: String
): String {
    val locale = if (language == "ar") Locale("ar", "EG") else Locale.US
    return NumberFormat.getCurrencyInstance(locale).apply {
        currency = Currency.getInstance(currencyCode)
        minimumFractionDigits = exponent
        maximumFractionDigits = exponent
    }.format(BigDecimal.valueOf(amountMinor, exponent))
}

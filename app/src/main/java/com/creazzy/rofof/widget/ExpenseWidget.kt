package com.creazzy.rofof.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.creazzy.rofof.app.MainActivity
import com.creazzy.rofof.data.local.RofofDatabase
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Currency
import java.util.Locale

class ExpenseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val amountMinor = runCatching {
            val today = LocalDate.now()
            val start = today.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            val end = today.plusMonths(1).withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            RofofDatabase.getInstance(context).transactionDao()
                .observeMonthlyExpenseTotal("EGP", 2, start, end).first()
        }.getOrDefault(0L)
        val locale = Locale("ar", "EG")
        val formatted = NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance("EGP")
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(BigDecimal.valueOf(amountMinor, 2))

        provideContent {
            WidgetContent(context, formatted)
        }
    }

    private fun WidgetContent(context: Context, total: String) {
        val quickAddIntent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_QUICK_ADD
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(ColorProvider(Color(0xFFFFFFFF)))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "مصروفات الشهر · EGP",
                style = TextStyle(color = ColorProvider(Color(0xFF53635F)), fontSize = 12.sp)
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = total,
                style = TextStyle(
                    color = ColorProvider(Color(0xFF147D64)),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.background(ColorProvider(Color(0xFFE6F2ED)))
                    .clickable(actionStartActivity(quickAddIntent))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "＋ إضافة سريعة",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF126B57)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

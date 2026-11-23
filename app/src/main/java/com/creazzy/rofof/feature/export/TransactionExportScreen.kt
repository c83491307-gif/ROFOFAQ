package com.creazzy.rofof.feature.export

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.creazzy.rofof.data.export.TransactionCsvExporter
import com.creazzy.rofof.data.local.RofofDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun TransactionExportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { RofofDatabase.getInstance(context) }
    val createDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    val csv = withContext(Dispatchers.IO) {
                        TransactionCsvExporter.export(database.transactionDao().getAllForExport())
                    }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "w")?.bufferedWriter(Charsets.UTF_8)?.use {
                            it.write(csv)
                        } ?: error("Unable to open selected destination")
                    }
                }
                Toast.makeText(
                    context,
                    if (result.isSuccess) "تم تصدير العمليات بنجاح · Export complete"
                    else "تعذر التصدير · Export failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("تصدير البيانات · Export data")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تصدير جميع معاملات الدخل والمصروفات إلى CSV بترميز UTF-8 مع BOM لدعم النص العربي.")
                Text("Export all income and expense transactions to a UTF-8 CSV file.")
                Text("لا يتم رفع البيانات إلى أي خادم. اختر مكان الحفظ بنفسك.")
                Text("No data is uploaded. You choose the save location.")
                Button(
                    onClick = { createDocument.launch("rofof-transactions-" + LocalDate.now() + ".csv") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("اختيار مكان الحفظ · Save CSV") }
            }
        }
        Button(onClick = onBack) { Text("رجوع · Back") }
    }
}

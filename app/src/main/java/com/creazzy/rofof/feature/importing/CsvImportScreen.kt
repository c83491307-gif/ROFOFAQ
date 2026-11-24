package com.creazzy.rofof.feature.importing

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.creazzy.rofof.data.importing.TransactionCsvImportService
import com.creazzy.rofof.data.importing.TransactionCsvImporter
import com.creazzy.rofof.data.local.RofofDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

private const val MAX_IMPORT_BYTES = 5 * 1024 * 1024

@Composable
fun CsvImportScreen(
    language: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isArabic = language == "ar"
    var selectedName by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<TransactionCsvImporter.ImportResult?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var imported by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            busy = true
            status = null
            preview = null
            imported = false
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val name = context.contentResolver.query(
                            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else null
                        } ?: "CSV"
                        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                            val output = ByteArrayOutputStream()
                            val buffer = ByteArray(8 * 1024)
                            var total = 0
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                total += count
                                require(total <= MAX_IMPORT_BYTES) { "CSV exceeds the 5 MiB import limit" }
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        } ?: error("Unable to open selected document")
                        val csv = StandardCharsets.UTF_8.newDecoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .decode(ByteBuffer.wrap(bytes)).toString()
                        name to TransactionCsvImporter.parse(csv)
                    }
                }
                busy = false
                result.onSuccess { (name, parsed) ->
                    selectedName = name
                    preview = parsed
                    if (!parsed.isValid) {
                        status = if (isArabic) "تعذر اعتماد الملف: راجع أخطاء التحقق أدناه." else "File validation failed. Review the errors below."
                    }
                }.onFailure {
                    status = it.message ?: if (isArabic) "تعذر قراءة الملف." else "Could not read the file."
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text(if (isArabic) "رجوع" else "Back") }
        Text(
            if (isArabic) "استيراد معاملات CSV" else "Import transactions from CSV",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            if (isArabic) "اختر ملف تصدير ROFOF بصيغة CSV. تتم معاينة الملف والتحقق من كل الصفوف قبل أي تعديل على قاعدة البيانات. الحد الأقصى 5 ميجابايت."
            else "Choose a ROFOF CSV export. Every row is validated before any database change. Maximum file size: 5 MiB.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = { picker.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel")) }, enabled = !busy) {
            Text(if (isArabic) "اختيار ملف CSV" else "Choose CSV file")
        }
        if (busy) CircularProgressIndicator()
        selectedName?.let { Text(if (isArabic) "الملف: $it" else "File: $it") }
        preview?.let { parsed ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (parsed.isValid) {
                            if (isArabic) "جاهز للمراجعة · ${parsed.transactions.size} معاملة" else "Ready for review · ${parsed.transactions.size} transactions"
                        } else {
                            if (isArabic) "فشل التحقق · ${parsed.errors.size} أخطاء" else "Validation failed · ${parsed.errors.size} errors"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    parsed.transactions.take(10).forEach { transaction ->
                        Text("• ${transaction.title} — ${transaction.amountMinor} ${transaction.currencyCode}")
                    }
                    if (parsed.transactions.size > 10) {
                        Text(if (isArabic) "و${parsed.transactions.size - 10} معاملات أخرى" else "And ${parsed.transactions.size - 10} more")
                    }
                    parsed.errors.take(30).forEach { error ->
                        Text(
                            if (isArabic) "السطر ${error.row}: ${error.message}" else "Row ${error.row}: ${error.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (parsed.errors.size > 30) {
                        Text(if (isArabic) "يتم عرض أول 30 خطأ فقط." else "Showing the first 30 errors.")
                    }
                }
            }
            if (parsed.isValid && !imported) {
                Text(
                    if (isArabic) "لن تُضاف البيانات إلا بعد تأكيدك. يجب أن تكون الحسابات والتصنيفات ومعرّفاتها موجودة على هذا الجهاز."
                    else "Nothing is saved until you confirm. Referenced accounts, categories and IDs must already exist on this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    val database = RofofDatabase.getInstance(context)
                                    val service = TransactionCsvImportService(
                                        database.transactionDao(),
                                        database.accountDao(),
                                        database.categoryDao()
                                    )
                                    context.contentResolver.openInputStream(
                                        // Re-open through the document picker URI retained below.
                                        requireNotNull(currentImportUri)
                                    )?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                                        service.importConfirmed(reader.readText())
                                    } ?: error("Unable to reopen selected CSV")
                                }
                            }
                            busy = false
                            result.onSuccess { outcome ->
                                if (outcome.succeeded) {
                                    imported = true
                                    status = if (isArabic) "تم استيراد ${outcome.importedCount} معاملة بنجاح." else "Imported ${outcome.importedCount} transactions successfully."
                                } else {
                                    status = outcome.errors.take(10).joinToString("\n")
                                }
                            }.onFailure { status = it.message ?: "Import failed" }
                        }
                    }
                ) { Text(if (isArabic) "تأكيد الاستيراد" else "Confirm import") }
            }
        }
        status?.let { Text(it, color = if (imported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
        if (imported) {
            TextButton(onClick = {
                preview = null
                selectedName = null
                status = null
                imported = false
            }) { Text(if (isArabic) "استيراد ملف آخر" else "Import another file") }
        }
    }
}

private var currentImportUri: Uri? = null

package com.creazzy.rofof.feature.smartentry

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.creazzy.rofof.data.local.FrequentCategory

@Composable
fun FrequentCategoryChips(categories: List<FrequentCategory>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.take(4).forEach { category ->
            AssistChip(onClick = { onSelect(category.categoryId) }, label = { Text(category.categoryName) })
        }
    }
}

@Composable
fun SuggestedAmountChip(label: String, onApply: () -> Unit, modifier: Modifier = Modifier) {
    AssistChip(onClick = onApply, modifier = modifier, label = { Text(label) })
}

/** Clipboard is inspected only after an explicit user tap; accepting the suggestion remains explicit. */
@Composable
fun SmartPasteAction(onAccept: (SmartPasteSuggestion) -> Unit, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    var pending by remember { mutableStateOf<SmartPasteSuggestion?>(null) }
    AssistChip(
        onClick = { pending = clipboard.getText()?.text?.let(SmartPasteParser::parse) },
        modifier = modifier.semantics { contentDescription = "Paste bank message and review transaction" },
        label = { Text("Paste transaction") }
    )
    pending?.let { suggestion ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Review pasted transaction") },
            text = { Text(suggestion.amountText + " · " + (suggestion.merchantText ?: suggestion.parsedEntry.category?.categoryNameArabic ?: "Uncategorized")) },
            confirmButton = { TextButton(onClick = { onAccept(suggestion); pending = null }) { Text("Review and continue") } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun LocalVoiceInputButton(onRecognized: (String) -> Unit, onError: (Int) -> Unit = {}, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val currentOnRecognized by rememberUpdatedState(onRecognized)
    val currentOnError by rememberUpdatedState(onError)
    val recognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { currentOnError(error) }
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }?.let(currentOnRecognized)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose { recognizer?.cancel(); recognizer?.destroy() }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            runCatching { recognizer?.startListening(intent) }.onFailure { currentOnError(SpeechRecognizer.ERROR_CLIENT) }
        } else currentOnError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
    }
    AssistChip(
        onClick = {
            if (recognizer == null) currentOnError(SpeechRecognizer.ERROR_CLIENT)
            else permission.launch(Manifest.permission.RECORD_AUDIO)
        },
        modifier = modifier.semantics { contentDescription = "Start voice transaction entry" },
        label = { Text("🎙 Voice") }
    )
}

@Composable
fun rememberSaveHaptic(): () -> Unit {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { { feedback.performHapticFeedback(HapticFeedbackType.LongPress) } }
}

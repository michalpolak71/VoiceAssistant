package com.voiceassistant.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Manager rozpoznawania mowy.
 * Używa wbudowanego silnika Android (Google offline lub producenta).
 * Działa offline gdy pobrano pakiet językowy PL.
 */
class SpeechRecognitionManager(private val context: Context) {

    sealed class SpeechResult {
        data class Partial(val text: String) : SpeechResult()
        data class Final(val text: String) : SpeechResult()
        data class Error(val message: String) : SpeechResult()
        object Started : SpeechResult()
        object Stopped : SpeechResult()
    }

    fun startListening(): Flow<SpeechResult> = callbackFlow {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pl-PL")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // Offline preference
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(SpeechResult.Started)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                trySend(SpeechResult.Partial(partial))
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                trySend(SpeechResult.Final(text))
                close()
            }

            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Błąd audio"
                    SpeechRecognizer.ERROR_CLIENT -> "Błąd klienta"
                    SpeechRecognizer.ERROR_NETWORK -> "Brak sieci – sprawdź pakiet offline"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Nie rozpoznano mowy"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Zbyt cicho – spróbuj ponownie"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Rozpoznawanie zajęte"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Brak uprawnień mikrofonu"
                    else -> "Nieznany błąd ($error)"
                }
                trySend(SpeechResult.Error(msg))
                close()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { trySend(SpeechResult.Stopped) }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)

        awaitClose {
            recognizer.stopListening()
            recognizer.destroy()
        }
    }

    companion object {
        fun isAvailable(context: Context): Boolean =
            SpeechRecognizer.isRecognitionAvailable(context)
    }
}

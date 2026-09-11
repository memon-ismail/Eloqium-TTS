package org.eloqium.tts.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import org.eloqium.tts.R

class GetSampleTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val result = Intent().apply {
            putExtra(TextToSpeech.Engine.EXTRA_SAMPLE_TEXT, getString(R.string.sample_text))
        }
        setResult(TextToSpeech.LANG_AVAILABLE, result)
        finish()
    }
}

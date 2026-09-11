package org.eloqium.tts.ui

import android.content.Context
import android.content.Intent
import android.net.Uri

object AboutNavigation {
    const val GITHUB_URL = "https://github.com/memon-ismail/eloqium-tts"
    const val TELEGRAM_URL = "https://t.me/blindroidofficial"
    const val TELEGRAM_APP_URI = "tg://resolve?domain=blindroidofficial"

    fun openGitHub(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))
        context.startActivity(intent)
    }

    fun openTelegram(context: Context) {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_APP_URI))
        try {
            context.startActivity(appIntent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_URL))
            context.startActivity(webIntent)
        }
    }
}

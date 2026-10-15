package br.com.synergyone.android.ui.modules

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import br.com.synergyone.android.BuildConfig

/**
 * Abre uma página do site no navegador (Custom Tab). O login do site é **separado** do app: quem
 * abre pode precisar entrar de novo. Nunca passamos token nem dado de sessão na URL.
 */
object WebLauncher {
    fun url(path: String): String = BuildConfig.API_BASE_URL.trimEnd('/') + "/" + path.trimStart('/')

    fun open(context: Context, path: String) {
        val uri = Uri.parse(url(path))
        try {
            CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, uri)
        } catch (_: Exception) {
            // Sem navegador com Custom Tabs: cai no visualizador padrão.
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
}

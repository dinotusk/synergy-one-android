package br.com.synergyone.android.ui.leads

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Ações de contato reais do lead (abrem o app do sistema; nada é enviado pelo Synergy One). */
object ContactActions {
    private fun start(context: Context, intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun digits(phone: String): String = phone.filter { it.isDigit() }

    fun dial(context: Context, phone: String) = start(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${digits(phone)}")))

    /** `wa.me` exige DDI; telefones da API vêm com `+55…`. Sem DDI plausível, assume Brasil. */
    fun whatsapp(context: Context, phone: String) {
        val d = digits(phone).let { if (it.length in 10..11) "55$it" else it }
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$d")))
    }

    fun email(context: Context, address: String) = start(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")))
}

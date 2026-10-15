package br.com.synergyone.android.ui.termometro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.AplicacaoDto
import br.com.synergyone.android.data.jornada.FormError
import br.com.synergyone.android.data.termometro.AvaliacaoForm
import br.com.synergyone.android.data.termometro.CLASSIFICACOES
import br.com.synergyone.android.ui.leads.DateField
import br.com.synergyone.android.ui.leads.PickerField
import kotlinx.coroutines.launch

/**
 * Criar/editar/corrigir uma avaliação. Fica sempre como **rascunho**: o cliente só vê depois de
 * publicada (por admin/gestão). A nota interna nunca é exibida ao cliente.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AvaliacaoSheet(
    titulo: String,
    initial: AvaliacaoForm,
    aplicacoes: List<AplicacaoDto>,
    onDismiss: () -> Unit,
    onSave: suspend (AvaliacaoForm) -> ApiResult<*>,
) {
    var form by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<FormError?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun erro(campo: String) = error?.takeIf { it.campo == campo }?.message
    val campos = setOf("classificacao", "periodoInicio", "periodoFim", "justificativa", "feedback", "notaInterna")

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(titulo, style = MaterialTheme.typography.titleLarge)

            Text("Classificação", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CLASSIFICACOES.forEach { wire ->
                    val c = Classificacao.fromWire(wire)
                    FilterChip(
                        selected = form.classificacao == wire,
                        onClick = { form = form.copy(classificacao = wire); error = null },
                        colors = br.com.synergyone.android.ui.components.synergyChipColors(), label = { Text(c.label) },
                        leadingIcon = { Icon(c.icon, contentDescription = null, tint = c.color()) },
                    )
                }
            }
            erro("classificacao")?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }

            DateField("Início do período", form.periodoInicio, { form = form.copy(periodoInicio = it) }, erro("periodoInicio"))
            DateField("Fim do período", form.periodoFim, { form = form.copy(periodoFim = it) }, erro("periodoFim"))

            if (aplicacoes.isNotEmpty()) {
                PickerField("Método aplicado", aplicacoes.map { it.id to (it.nome + (it.metodoNome?.let { m -> " ($m)" } ?: "")) }, form.aplicacaoId, "Nenhum") {
                    form = form.copy(aplicacaoId = it)
                }
            }

            OutlinedTextField(
                value = form.justificativa,
                onValueChange = { if (it.length <= 2000) form = form.copy(justificativa = it) },
                label = { Text("Justificativa (o cliente vê)") },
                supportingText = { Text(erro("justificativa") ?: "${form.justificativa.length}/2000") },
                isError = erro("justificativa") != null,
                modifier = Modifier.fillMaxWidth(), minLines = 3,
            )
            OutlinedTextField(
                value = form.feedback,
                onValueChange = { if (it.length <= 4000) form = form.copy(feedback = it) },
                label = { Text("Feedback e próximos passos (o cliente vê)") },
                supportingText = { Text(erro("feedback") ?: "${form.feedback.length}/4000") },
                isError = erro("feedback") != null,
                modifier = Modifier.fillMaxWidth(), minLines = 3,
            )
            OutlinedTextField(
                value = form.notaInterna,
                onValueChange = { if (it.length <= 2000) form = form.copy(notaInterna = it) },
                label = { Text("Nota interna (o cliente NÃO vê)") },
                supportingText = { Text(erro("notaInterna") ?: "${form.notaInterna.length}/2000") },
                isError = erro("notaInterna") != null,
                modifier = Modifier.fillMaxWidth(), minLines = 2,
            )

            error?.takeIf { it.campo !in campos }?.let { Text(it.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }

            Button(
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val local = form.validate()
                    if (local != null) { error = local; return@Button }
                    saving = true; error = null
                    scope.launch {
                        val r = onSave(form)
                        saving = false
                        if (r is ApiResult.Error) error = FormError(r.field ?: "_servidor", r.message)
                    }
                },
            ) {
                if (saving) CircularProgressIndicator(Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                Text(if (saving) "Salvando…" else "Salvar rascunho")
            }
        }
    }
}

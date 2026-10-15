package br.com.synergyone.android.ui.termometro

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.data.api.AvaliacaoPublicadaDto
import br.com.synergyone.android.ui.common.formatPeriod
import java.time.Instant
import java.time.ZoneOffset

@Composable
private fun SheetRow(selected: Boolean, onClick: () -> Unit, leading: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading()
        Column(modifier = Modifier.weight(1f)) { content() }
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Selecionado", tint = MaterialTheme.colorScheme.primary)
    }
}

/** Equipe: escolhe um período pronto ou personalizado. A API aceita `desde`/`ate`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamPeriodSheet(selected: PeriodRange?, onSelect: (PeriodRange) -> Unit, onDismiss: () -> Unit) {
    var custom by remember { mutableStateOf(false) }
    if (custom) {
        CustomRangeDialog(onConfirm = { onSelect(it) }, onDismiss = { custom = false })
        return
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text("Período", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            periodPresets().forEach { p ->
                SheetRow(selected = selected?.desde == p.desde && selected.ate == p.ate, onClick = { onSelect(p) }) {
                    Text(p.label, style = MaterialTheme.typography.bodyLarge)
                    Text(formatPeriod(p.desde, p.ate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SheetRow(selected = false, onClick = { custom = true }, leading = { Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary) }) {
                Text("Personalizado…", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangeDialog(onConfirm: (PeriodRange) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDateRangePickerState()
    val ini = state.selectedStartDateMillis
    val fim = state.selectedEndDateMillis
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = ini != null && fim != null,
                onClick = {
                    // O seletor devolve meia-noite UTC do dia escolhido: converte sem deslocar o dia.
                    val a = Instant.ofEpochMilli(ini!!).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    val b = Instant.ofEpochMilli(fim!!).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    onConfirm(PeriodRange("Personalizado", a, b))
                },
            ) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) {
        DateRangePicker(state = state, modifier = Modifier.weight(1f, fill = false))
    }
}

/** Cliente: o "período" é a avaliação publicada que ele quer rever (a atual e o histórico). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvaliacaoPeriodSheet(
    avaliacoes: List<AvaliacaoPublicadaDto>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.navigationBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            Text("Período avaliado", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            avaliacoes.forEach { a ->
                val c = Classificacao.fromWire(a.classificacao)
                SheetRow(selected = a.id == selectedId, onClick = { onSelect(a.id) }, leading = { ClassificationDot(c) }) {
                    Text(formatPeriod(a.periodoInicio, a.periodoFim), style = MaterialTheme.typography.bodyLarge)
                    Text(c.label, style = MaterialTheme.typography.bodySmall, color = c.color())
                }
            }
        }
    }
}

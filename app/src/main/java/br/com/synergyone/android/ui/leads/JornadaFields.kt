package br.com.synergyone.android.ui.leads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.synergyone.android.ui.common.formatDate
import java.time.Instant
import java.time.ZoneOffset

/** Campo de data (AAAA-MM-DD) com seletor nativo. O valor vazio aparece como "Escolher data". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, value: String?, onChange: (String?) -> Unit, error: String? = null, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(if (value.isNullOrBlank()) "$label: escolher data" else "$label: ${formatDate(value)}")
        }
        if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = value?.let { runCatching { java.time.LocalDate.parse(it.take(10)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull() },
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    // O seletor devolve meia-noite UTC do dia escolhido: converte sem deslocar o dia.
                    onChange(state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() })
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
        ) { DatePicker(state = state) }
    }
}

/** Lista suspensa de opções `(id → rótulo)` com uma opção "vazia" opcional. */
@Composable
fun <T> PickerField(
    label: String,
    options: List<Pair<T, String>>,
    selected: T?,
    emptyLabel: String,
    modifier: Modifier = Modifier,
    onSelect: (T?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${options.firstOrNull { it.first == selected }?.second ?: emptyLabel}", maxLines = 1)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(emptyLabel) }, onClick = { open = false; onSelect(null) })
            options.forEach { (id, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { open = false; onSelect(id) }) }
        }
    }
}

@Composable
fun MoneyField(label: String, value: String, onChange: (String) -> Unit, error: String? = null, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) },
        label = { Text(label) },
        prefix = { Text("R$ ") },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

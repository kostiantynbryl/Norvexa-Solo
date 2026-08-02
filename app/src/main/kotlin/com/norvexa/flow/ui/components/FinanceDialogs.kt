package com.norvexa.flow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.norvexa.flow.data.local.PlannedExpenseEntity
import com.norvexa.flow.data.local.ReceivableEntity
import com.norvexa.flow.data.local.ReserveEntity
import com.norvexa.flow.data.local.WalletEntity
import com.norvexa.flow.data.local.ClientEntity
import com.norvexa.flow.data.settings.UserSettings
import com.norvexa.flow.domain.MarginInput
import com.norvexa.flow.domain.MarginResult
import com.norvexa.flow.domain.PriceInput
import com.norvexa.flow.domain.PriceResult
import com.norvexa.flow.domain.TransactionType
import com.norvexa.flow.domain.formatMoney
import com.norvexa.flow.domain.parseMinor
import com.norvexa.flow.domain.parseRateMicros
import java.time.LocalDate

@Composable
fun AddWalletDialog(
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(baseCurrency) }
    var balance by remember { mutableStateOf("0") }
    var rate by remember { mutableStateOf("1") }
    val valid = name.isNotBlank() && currency.length == 3 &&
        parseMinor(balance) != null && parseRateMicros(rate) != null

    FormDialog(
        title = "Новый кошелёк",
        onDismiss = onDismiss,
        valid = valid,
        onSave = {
            onSave(
                name.trim(),
                currency,
                parseMinor(balance) ?: 0,
                if (currency == baseCurrency) 1_000_000 else parseRateMicros(rate) ?: 1_000_000,
            )
        },
    ) {
        Field(name, { name = it }, "Название")
        Field(currency, { currency = it.uppercase().take(3) }, "Валюта")
        NumberField(balance, { balance = it }, "Текущий остаток")
        if (currency != baseCurrency) {
            NumberField(rate, { rate = it }, "Курс: 1 $currency = ? $baseCurrency")
        }
    }
}

@Composable
fun AddTransactionDialog(
    wallets: List<WalletEntity>,
    clients: List<ClientEntity>,
    initialType: String,
    onDismiss: () -> Unit,
    onSave: (Long, String, Long, String, String, Long?) -> Unit,
) {
    var walletId by remember(wallets) { mutableLongStateOf(wallets.firstOrNull()?.id ?: 0) }
    var type by remember { mutableStateOf(initialType) }
    var amount by remember { mutableStateOf("") }
    var category by remember {
        mutableStateOf(if (initialType == TransactionType.INCOME) "Оплата клиента" else "Бизнес-расход")
    }
    var note by remember { mutableStateOf("") }
    var clientId by remember { mutableStateOf<Long?>(null) }

    FormDialog(
        title = if (type == TransactionType.INCOME) "Добавить доход" else "Добавить расход",
        onDismiss = onDismiss,
        valid = walletId != 0L && (parseMinor(amount) ?: 0) > 0,
        onSave = { onSave(walletId, type, parseMinor(amount) ?: 0, category, note, clientId) },
    ) {
        Selector(
            label = "Тип",
            selected = if (type == TransactionType.INCOME) "Доход" else "Расход",
            options = listOf("Доход" to TransactionType.INCOME, "Расход" to TransactionType.EXPENSE),
            onSelected = { type = it },
        )
        Selector(
            label = "Кошелёк",
            selected = wallets.firstOrNull { it.id == walletId }?.name ?: "Нет кошельков",
            options = wallets.map { it.name to it.id },
            onSelected = { walletId = it },
        )
        NumberField(amount, { amount = it }, "Сумма")
        Field(category, { category = it }, "Категория")
        if (clients.isNotEmpty()) {
            Selector(
                label = "Клиент",
                selected = clients.firstOrNull { it.id == clientId }?.name ?: "Без клиента",
                options = listOf<Pair<String, Long?>>("Без клиента" to null) + clients.map { it.name to it.id },
                onSelected = { clientId = it },
            )
        }
        Field(note, { note = it }, "Заметка", singleLine = false)
    }
}

@Composable
fun AddClientDialog(
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(baseCurrency) }
    var note by remember { mutableStateOf("") }

    FormDialog(
        title = "Новый клиент",
        onDismiss = onDismiss,
        valid = name.isNotBlank() && currency.length == 3,
        onSave = { onSave(name.trim(), email.trim(), currency, note.trim()) },
    ) {
        Field(name, { name = it }, "Имя или компания")
        Field(email, { email = it }, "Email")
        Field(currency, { currency = it.uppercase().take(3) }, "Основная валюта")
        Field(note, { note = it }, "Заметка", singleLine = false)
    }
}

@Composable
fun AddReceivableDialog(
    clients: List<ClientEntity>,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (ReceivableEntity) -> Unit,
) {
    var clientId by remember(clients) { mutableLongStateOf(clients.firstOrNull()?.id ?: 0) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(clients.firstOrNull()?.defaultCurrency ?: baseCurrency) }
    var rate by remember { mutableStateOf("1") }
    var date by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var probability by remember { mutableStateOf("100") }
    var note by remember { mutableStateOf("") }
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
    val parsedProbability = probability.toIntOrNull()
    val valid = clientId != 0L && title.isNotBlank() && (parseMinor(amount) ?: 0) > 0 &&
        parsedDate != null && (parsedProbability ?: -1) in 0..100 && parseRateMicros(rate) != null

    FormDialog(
        title = "Ожидаемая оплата",
        onDismiss = onDismiss,
        valid = valid,
        onSave = {
            onSave(
                ReceivableEntity(
                    clientId = clientId,
                    title = title.trim(),
                    amountMinor = parseMinor(amount) ?: 0,
                    currency = currency,
                    rateToBaseMicros = if (currency == baseCurrency) 1_000_000 else parseRateMicros(rate) ?: 1_000_000,
                    expectedAtEpochDay = parsedDate?.toEpochDay() ?: LocalDate.now().toEpochDay(),
                    probabilityPercent = parsedProbability ?: 100,
                    note = note.trim(),
                ),
            )
        },
    ) {
        if (clients.isEmpty()) {
            Text("Сначала добавьте клиента", color = MaterialTheme.colorScheme.error)
        } else {
            Selector(
                label = "Клиент",
                selected = clients.firstOrNull { it.id == clientId }?.name ?: "Выберите",
                options = clients.map { it.name to it.id },
                onSelected = { id ->
                    clientId = id
                    currency = clients.firstOrNull { it.id == id }?.defaultCurrency ?: baseCurrency
                },
            )
        }
        Field(title, { title = it }, "Проект или назначение")
        NumberField(amount, { amount = it }, "Сумма")
        Field(currency, { currency = it.uppercase().take(3) }, "Валюта")
        if (currency != baseCurrency) {
            NumberField(rate, { rate = it }, "Курс: 1 $currency = ? $baseCurrency")
        }
        Field(date, { date = it }, "Дата оплаты YYYY-MM-DD")
        NumberField(probability, { probability = it.filter(Char::isDigit).take(3) }, "Вероятность, %", decimal = false)
        Field(note, { note = it }, "Заметка", singleLine = false)
    }
}

@Composable
fun AddExpenseDialog(
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (PlannedExpenseEntity) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(baseCurrency) }
    var rate by remember { mutableStateOf("1") }
    var date by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var category by remember { mutableStateOf("Обязательные расходы") }
    var mandatory by remember { mutableStateOf(true) }
    var recurrence by remember { mutableStateOf("NONE") }
    var note by remember { mutableStateOf("") }
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
    val valid = title.isNotBlank() && (parseMinor(amount) ?: 0) > 0 &&
        parsedDate != null && parseRateMicros(rate) != null

    FormDialog(
        title = "Будущий расход",
        onDismiss = onDismiss,
        valid = valid,
        onSave = {
            onSave(
                PlannedExpenseEntity(
                    title = title.trim(),
                    amountMinor = parseMinor(amount) ?: 0,
                    currency = currency,
                    rateToBaseMicros = if (currency == baseCurrency) 1_000_000 else parseRateMicros(rate) ?: 1_000_000,
                    dueAtEpochDay = parsedDate?.toEpochDay() ?: LocalDate.now().toEpochDay(),
                    category = category,
                    isMandatory = mandatory,
                    recurrence = recurrence,
                    note = note.trim(),
                ),
            )
        },
    ) {
        Field(title, { title = it }, "Название")
        NumberField(amount, { amount = it }, "Сумма")
        Field(currency, { currency = it.uppercase().take(3) }, "Валюта")
        if (currency != baseCurrency) NumberField(rate, { rate = it }, "Курс")
        Field(date, { date = it }, "Дата YYYY-MM-DD")
        Field(category, { category = it }, "Категория")
        Selector(
            label = "Повторение",
            selected = recurrenceLabel(recurrence),
            options = listOf("Не повторять" to "NONE", "Ежемесячно" to "MONTHLY", "Ежегодно" to "YEARLY"),
            onSelected = { recurrence = it },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Обязательный расход")
            Switch(checked = mandatory, onCheckedChange = { mandatory = it })
        }
        Field(note, { note = it }, "Заметка", singleLine = false)
    }
}

@Composable
fun AddReserveDialog(
    baseCurrency: String,
    onDismiss: () -> Unit,
    onSave: (ReserveEntity) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("0") }
    var type by remember { mutableStateOf("CUSTOM") }
    var isProtected by remember { mutableStateOf(true) }

    FormDialog(
        title = "Новый резерв",
        onDismiss = onDismiss,
        valid = name.isNotBlank() && (parseMinor(target) ?: 0) > 0 && parseMinor(current) != null,
        onSave = {
            onSave(
                ReserveEntity(
                    name = name.trim(),
                    targetMinor = parseMinor(target) ?: 0,
                    currentMinor = parseMinor(current) ?: 0,
                    currency = baseCurrency,
                    type = type,
                    isProtected = isProtected,
                ),
            )
        },
    ) {
        Field(name, { name = it }, "Название")
        NumberField(target, { target = it }, "Целевая сумма")
        NumberField(current, { current = it }, "Уже отложено")
        Selector(
            label = "Тип",
            selected = reserveTypeLabel(type),
            options = listOf(
                "Пользовательский" to "CUSTOM",
                "Налоговый" to "TAX",
                "Финансовая подушка" to "EMERGENCY",
                "Оборудование" to "EQUIPMENT",
            ),
            onSelected = { type = it },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Вычитать из доступных денег")
            Switch(checked = isProtected, onCheckedChange = { isProtected = it })
        }
    }
}

@Composable
fun UpdateReserveDialog(
    reserve: ReserveEntity,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    var amount by remember { mutableStateOf((reserve.currentMinor / 100.0).toString()) }
    FormDialog(
        title = "Изменить резерв",
        onDismiss = onDismiss,
        valid = parseMinor(amount) != null,
        onSave = { onSave(parseMinor(amount) ?: 0) },
    ) {
        Text(reserve.name, fontWeight = FontWeight.SemiBold)
        NumberField(amount, { amount = it }, "Текущая сумма")
    }
}

@Composable
fun SettingsDialog(
    settings: UserSettings,
    onDismiss: () -> Unit,
    onSave: (String, Int, Long) -> Unit,
) {
    var currency by remember { mutableStateOf(settings.baseCurrency) }
    var tax by remember { mutableStateOf(settings.taxPercent.toString()) }
    var safe by remember { mutableStateOf((settings.safeBalanceMinor / 100.0).toString()) }
    val parsedTax = tax.toIntOrNull()

    FormDialog(
        title = "Финансовые настройки",
        onDismiss = onDismiss,
        valid = currency.length == 3 && (parsedTax ?: -1) in 0..95 && parseMinor(safe) != null,
        onSave = { onSave(currency, parsedTax ?: 0, parseMinor(safe) ?: 0) },
    ) {
        Field(currency, { currency = it.uppercase().take(3) }, "Базовая валюта")
        NumberField(tax, { tax = it.filter(Char::isDigit).take(2) }, "Плановый налоговый резерв, %", decimal = false)
        NumberField(safe, { safe = it }, "Безопасный остаток")
        Text(
            "Ставки задаются пользователем. Это не налоговая консультация.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun PriceCalculatorDialog(
    currency: String,
    defaultTaxPercent: Int,
    calculate: (PriceInput) -> PriceResult,
    onDismiss: () -> Unit,
) {
    var net by remember { mutableStateOf("3000") }
    var costs by remember { mutableStateOf("300") }
    var reserve by remember { mutableStateOf("300") }
    var billable by remember { mutableStateOf("100") }
    var hours by remember { mutableStateOf("20") }
    var direct by remember { mutableStateOf("0") }
    var tax by remember { mutableStateOf(defaultTaxPercent.toString()) }
    var fee by remember { mutableStateOf("3") }
    var risk by remember { mutableStateOf("10") }
    var result by remember { mutableStateOf<PriceResult?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Калькулятор цены проекта") },
        text = {
            DialogColumn {
                NumberField(net, { net = it }, "Желаемый чистый доход в месяц")
                NumberField(costs, { costs = it }, "Бизнес-расходы в месяц")
                NumberField(reserve, { reserve = it }, "Пополнение резерва")
                NumberField(billable, { billable = it.filter(Char::isDigit) }, "Оплачиваемых часов", decimal = false)
                NumberField(hours, { hours = it.filter(Char::isDigit) }, "Часов на проект", decimal = false)
                NumberField(direct, { direct = it }, "Прямые расходы")
                NumberField(tax, { tax = it.filter(Char::isDigit) }, "Налоговый резерв, %", decimal = false)
                NumberField(fee, { fee = it.filter(Char::isDigit) }, "Комиссии, %", decimal = false)
                NumberField(risk, { risk = it.filter(Char::isDigit) }, "Запас риска, %", decimal = false)
                result?.let {
                    Text("Минимальная ставка: ${formatMoney(it.minimumHourlyRateMinor, currency)}/ч")
                    Text("Минимальная цена: ${formatMoney(it.minimumProjectPriceMinor, currency)}")
                    Text(
                        "Рекомендуемая: ${formatMoney(it.recommendedProjectPriceMinor, currency)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    runCatching {
                        calculate(
                            PriceInput(
                                desiredNetMonthlyMinor = parseMinor(net) ?: 0,
                                monthlyBusinessCostsMinor = parseMinor(costs) ?: 0,
                                monthlyReserveContributionMinor = parseMinor(reserve) ?: 0,
                                billableHoursPerMonth = billable.toIntOrNull() ?: 0,
                                projectHours = hours.toIntOrNull() ?: 0,
                                directProjectCostsMinor = parseMinor(direct) ?: 0,
                                taxPercent = tax.toIntOrNull() ?: 0,
                                feePercent = fee.toIntOrNull() ?: 0,
                                riskPercent = risk.toIntOrNull() ?: 0,
                            ),
                        )
                    }.onSuccess { result = it }
                },
            ) { Text("Рассчитать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable
fun MarginCalculatorDialog(
    currency: String,
    defaultTaxPercent: Int,
    calculate: (MarginInput) -> MarginResult,
    onDismiss: () -> Unit,
) {
    var revenue by remember { mutableStateOf("1000") }
    var costs by remember { mutableStateOf("100") }
    var hours by remember { mutableStateOf("20") }
    var hourly by remember { mutableStateOf("20") }
    var tax by remember { mutableStateOf(defaultTaxPercent.toString()) }
    var fee by remember { mutableStateOf("3") }
    var result by remember { mutableStateOf<MarginResult?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Калькулятор маржи") },
        text = {
            DialogColumn {
                NumberField(revenue, { revenue = it }, "Цена проекта")
                NumberField(costs, { costs = it }, "Прямые расходы")
                NumberField(hours, { hours = it.filter(Char::isDigit) }, "Затрачено часов", decimal = false)
                NumberField(hourly, { hourly = it }, "Стоимость вашего часа")
                NumberField(tax, { tax = it.filter(Char::isDigit) }, "Налоговый резерв, %", decimal = false)
                NumberField(fee, { fee = it.filter(Char::isDigit) }, "Комиссии, %", decimal = false)
                result?.let {
                    Text("Денежная прибыль: ${formatMoney(it.cashProfitMinor, currency)}")
                    Text("Экономическая прибыль: ${formatMoney(it.economicProfitMinor, currency)}")
                    Text(
                        "Маржа: ${it.marginPercent}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    runCatching {
                        calculate(
                            MarginInput(
                                revenueMinor = parseMinor(revenue) ?: 0,
                                directCostsMinor = parseMinor(costs) ?: 0,
                                hours = hours.toIntOrNull() ?: 0,
                                hourlyCostMinor = parseMinor(hourly) ?: 0,
                                taxPercent = tax.toIntOrNull() ?: 0,
                                feePercent = fee.toIntOrNull() ?: 0,
                            ),
                        )
                    }.onSuccess { result = it }
                },
            ) { Text("Рассчитать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { Button(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun FormDialog(
    title: String,
    onDismiss: () -> Unit,
    valid: Boolean,
    onSave: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { DialogColumn(content) },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave()
                    onDismiss()
                },
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun DialogColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        content()
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NumberField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    decimal: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun <T> Selector(
    label: String,
    selected: String,
    options: List<Pair<String, T>>,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(selected) }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (text, value) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            onSelected(value)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private fun recurrenceLabel(value: String): String = when (value) {
    "MONTHLY" -> "Ежемесячно"
    "YEARLY" -> "Ежегодно"
    else -> "Не повторять"
}

private fun reserveTypeLabel(value: String): String = when (value) {
    "TAX" -> "Налоговый"
    "EMERGENCY" -> "Финансовая подушка"
    "EQUIPMENT" -> "Оборудование"
    else -> "Пользовательский"
}

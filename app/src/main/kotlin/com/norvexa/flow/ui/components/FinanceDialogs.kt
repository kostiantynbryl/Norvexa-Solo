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
import com.norvexa.flow.data.local.ClientEntity
import com.norvexa.flow.data.local.PlannedExpenseEntity
import com.norvexa.flow.data.local.ReceivableEntity
import com.norvexa.flow.data.local.ReserveEntity
import com.norvexa.flow.data.local.WalletEntity
import com.norvexa.flow.data.settings.UserSettings
import com.norvexa.flow.domain.MarginInput
import com.norvexa.flow.domain.MarginResult
import com.norvexa.flow.domain.PriceInput
import com.norvexa.flow.domain.PriceResult
import com.norvexa.flow.domain.TransactionType
import com.norvexa.flow.domain.formatMoney
import com.norvexa.flow.domain.isValidCurrencyCode
import com.norvexa.flow.domain.minorToDecimal
import com.norvexa.flow.domain.parseMinorForCurrency
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
    val valid = name.isNotBlank() &&
        isValidCurrencyCode(currency) &&
        parseMinorForCurrency(balance, currency) != null &&
        (currency == baseCurrency || (parseRateMicros(rate) ?: 0) > 0)

    FormDialog(
        title = "Новый кошелёк",
        onDismiss = onDismiss,
        valid = valid,
        onSave = {
            onSave(
                name.trim(),
                currency,
                parseMinorForCurrency(balance, currency) ?: 0,
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
    var walletId by remember(wallets) { mutableLongStateOf(wallets.firstOrNull { it.isActive }?.id ?: 0) }
    var type by remember { mutableStateOf(initialType) }
    var amount by remember { mutableStateOf("") }
    var category by remember {
        mutableStateOf(if (initialType == TransactionType.INCOME) "Оплата клиента" else "Бизнес-расход")
    }
    var note by remember { mutableStateOf("") }
    var clientId by remember { mutableStateOf<Long?>(null) }
    val selectedWallet = wallets.firstOrNull { it.id == walletId }
    val parsedAmount = selectedWallet?.let { parseMinorForCurrency(amount, it.currency) }

    FormDialog(
        title = if (type == TransactionType.INCOME) "Добавить доход" else "Добавить расход",
        onDismiss = onDismiss,
        valid = walletId != 0L && (parsedAmount ?: 0) > 0,
        onSave = { onSave(walletId, type, parsedAmount ?: 0, category, note, clientId) },
    ) {
        Selector(
            label = "Тип",
            selected = if (type == TransactionType.INCOME) "Доход" else "Расход",
            options = listOf("Доход" to TransactionType.INCOME, "Расход" to TransactionType.EXPENSE),
            onSelected = { type = it },
        )
        Selector(
            label = "Кошелёк",
            selected = selectedWallet?.name ?: "Нет кошельков",
            options = wallets.filter { it.isActive }.map { it.name to it.id },
            onSelected = { walletId = it },
        )
        NumberField(amount, { amount = it }, "Сумма${selectedWallet?.currency?.let { " · $it" }.orEmpty()}")
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
        valid = name.isNotBlank() && isValidCurrencyCode(currency),
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
    val parsedAmount = parseMinorForCurrency(amount, currency)
    val valid = clientId != 0L && title.isNotBlank() &&
        isValidCurrencyCode(currency) &&
        (parsedAmount ?: 0) > 0 &&
        parsedDate != null &&
        (parsedProbability ?: -1) in 0..100 &&
        (currency == baseCurrency || (parseRateMicros(rate) ?: 0) > 0)

    FormDialog(
        title = "Ожидаемая оплата",
        onDismiss = onDismiss,
        valid = valid,
        onSave = {
            onSave(
                ReceivableEntity(
                    clientId = clientId,
                    title = title.trim(),
                    amountMinor = parsedAmount ?: 0,
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
        NumberField(amount, { amount = it }, "Сумма · $currency")
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
    val parsedAmount = parseMinorForCurrency(amount, currency)
    val valid = title.isNotBlank() &&
        isValidCurrencyCode(currency) &&
        (parsedAmount ?: 0) > 0 &&
        parsedDate != null &&
        (currency == baseCurrency || (parseRateMicros(rate) ?: 0) > 0)

    FormDialog(
        title = "Будущий расход",
        onDismiss = onDismiss,
        valid = valid,
        onSave = {
            onSave(
                PlannedExpenseEntity(
                    title = title.trim(),
                    amountMinor = parsedAmount ?: 0,
                    currency = currency,
                    rateToBaseMicros = if (currency == baseCurrency) 1_000_000 else parseRateMicros(rate) ?: 1_000_000,
                    dueAtEpochDay = parsedDate?.toEpochDay() ?: LocalDate.now().toEpochDay(),
                    category = category.trim().ifEmpty { "Обязательные расходы" },
                    isMandatory = mandatory,
                    recurrence = recurrence,
                    note = note.trim(),
                ),
            )
        },
    ) {
        Field(title, { title = it }, "Название")
        NumberField(amount, { amount = it }, "Сумма · $currency")
        Field(currency, { currency = it.uppercase().take(3) }, "Валюта")
        if (currency != baseCurrency) NumberField(rate, { rate = it }, "Курс: 1 $currency = ? $baseCurrency")
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
    val parsedTarget = parseMinorForCurrency(target, baseCurrency)
    val parsedCurrent = parseMinorForCurrency(current, baseCurrency)

    FormDialog(
        title = "Новый резерв",
        onDismiss = onDismiss,
        valid = name.isNotBlank() && (parsedTarget ?: 0) > 0 && (parsedCurrent ?: -1) >= 0,
        onSave = {
            onSave(
                ReserveEntity(
                    name = name.trim(),
                    targetMinor = parsedTarget ?: 0,
                    currentMinor = parsedCurrent ?: 0,
                    currency = baseCurrency,
                    type = type,
                    isProtected = isProtected,
                ),
            )
        },
    ) {
        Field(name, { name = it }, "Название")
        NumberField(target, { target = it }, "Целевая сумма · $baseCurrency")
        NumberField(current, { current = it }, "Уже отложено · $baseCurrency")
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
    var amount by remember { mutableStateOf(minorToDecimal(reserve.currentMinor, reserve.currency).toPlainString()) }
    val parsed = parseMinorForCurrency(amount, reserve.currency)
    FormDialog(
        title = "Изменить резерв",
        onDismiss = onDismiss,
        valid = (parsed ?: -1) >= 0,
        onSave = { onSave(parsed ?: 0) },
    ) {
        Text(reserve.name, fontWeight = FontWeight.SemiBold)
        NumberField(amount, { amount = it }, "Текущая сумма · ${reserve.currency}")
    }
}

@Composable
fun SettingsDialog(
    settings: UserSettings,
    onDismiss: () -> Unit,
    onSave: (Int, Long) -> Unit,
) {
    var tax by remember { mutableStateOf(settings.taxPercent.toString()) }
    var safe by remember {
        mutableStateOf(minorToDecimal(settings.safeBalanceMinor, settings.baseCurrency).toPlainString())
    }
    val parsedTax = tax.toIntOrNull()
    val parsedSafe = parseMinorForCurrency(safe, settings.baseCurrency)

    FormDialog(
        title = "Финансовые настройки",
        onDismiss = onDismiss,
        valid = (parsedTax ?: -1) in 0..95 && (parsedSafe ?: -1) >= 0,
        onSave = { onSave(parsedTax ?: 0, parsedSafe ?: 0) },
    ) {
        Text(
            "Базовая валюта: ${settings.baseCurrency}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Для сохранения исторических курсов базовая валюта фиксируется после первоначальной настройки.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NumberField(tax, { tax = it.filter(Char::isDigit).take(2) }, "Плановый налоговый резерв, %", decimal = false)
        NumberField(safe, { safe = it }, "Безопасный остаток · ${settings.baseCurrency}")
        Text(
            "Ставки задаются пользователем. Это не налоговая консультация.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun WalletPickerDialog(
    title: String,
    amountLabel: String,
    wallets: List<WalletEntity>,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    val activeWallets = wallets.filter { it.isActive }
    var walletId by remember(activeWallets) { mutableLongStateOf(activeWallets.firstOrNull()?.id ?: 0L) }
    FormDialog(
        title = title,
        onDismiss = onDismiss,
        valid = walletId != 0L,
        onSave = { onSave(walletId) },
    ) {
        Text(amountLabel, style = MaterialTheme.typography.bodyMedium)
        if (activeWallets.isEmpty()) {
            Text("Сначала добавьте активный кошелёк", color = MaterialTheme.colorScheme.error)
        } else {
            Selector(
                label = "Кошелёк",
                selected = activeWallets.firstOrNull { it.id == walletId }?.name ?: "Выберите",
                options = activeWallets.map { "${it.name} · ${it.currency}" to it.id },
                onSelected = { walletId = it },
            )
        }
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
                                desiredNetMonthlyMinor = parseMinorForCurrency(net, currency) ?: 0,
                                monthlyBusinessCostsMinor = parseMinorForCurrency(costs, currency) ?: 0,
                                monthlyReserveContributionMinor = parseMinorForCurrency(reserve, currency) ?: 0,
                                billableHoursPerMonth = billable.toIntOrNull() ?: 0,
                                projectHours = hours.toIntOrNull() ?: 0,
                                directProjectCostsMinor = parseMinorForCurrency(direct, currency) ?: 0,
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
                                revenueMinor = parseMinorForCurrency(revenue, currency) ?: 0,
                                directCostsMinor = parseMinorForCurrency(costs, currency) ?: 0,
                                hours = hours.toIntOrNull() ?: 0,
                                hourlyCostMinor = parseMinorForCurrency(hourly, currency) ?: 0,
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

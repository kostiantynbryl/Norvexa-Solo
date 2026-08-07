package com.norvexa.flow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.norvexa.flow.domain.parseMinor
import com.norvexa.flow.ui.components.GroupCard
import com.norvexa.flow.ui.components.IconBubble

@Composable
fun OnboardingScreen(
    onComplete: (String, Int, Long, String, Long) -> Unit,
) {
    var page by remember { mutableIntStateOf(0) }
    var currency by remember { mutableStateOf("USD") }
    var tax by remember { mutableStateOf("10") }
    var safe by remember { mutableStateOf("500") }
    var wallet by remember { mutableStateOf("Основной кошелёк") }
    var balance by remember { mutableStateOf("0") }

    val finalPageValid = currency.length == 3 &&
        wallet.isNotBlank() &&
        tax.toIntOrNull() in 0..95 &&
        parseMinor(safe) != null &&
        parseMinor(balance) != null

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Norvexa Flow",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    PageDots(page = page, count = 3)
                }

                Spacer(Modifier.height(18.dp))

                when (page) {
                    0 -> IntroPanel(
                        icon = Icons.Rounded.AutoGraph,
                        eyebrow = "ПЛАНИРОВАНИЕ",
                        title = "Деньги без сюрпризов.",
                        text = "Сразу видно, сколько действительно доступно, какие платежи впереди и когда может возникнуть кассовый разрыв.",
                    )
                    1 -> IntroPanel(
                        icon = Icons.Rounded.Lock,
                        eyebrow = "КОНФИДЕНЦИАЛЬНОСТЬ",
                        title = "Ваши финансы остаются вашими.",
                        text = "Регистрация и банковские аккаунты не требуются. Основные данные хранятся локально на устройстве.",
                    )
                    else -> {
                        IntroPanel(
                            icon = Icons.Rounded.AccountBalanceWallet,
                            eyebrow = "НАЧАЛЬНАЯ НАСТРОЙКА",
                            title = "Настроим основу.",
                            text = "Пять коротких полей — и приложение сразу сможет считать доступные деньги и прогноз.",
                        )

                        GroupCard(Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedTextField(
                                    value = currency,
                                    onValueChange = { currency = it.uppercase().take(3) },
                                    label = { Text("Базовая валюта") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = tax,
                                    onValueChange = { tax = it.filter(Char::isDigit).take(2) },
                                    label = { Text("Плановый резерв, %") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = safe,
                                    onValueChange = { safe = it },
                                    label = { Text("Безопасный остаток") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = wallet,
                                    onValueChange = { wallet = it },
                                    label = { Text("Название кошелька") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = balance,
                                    onValueChange = { balance = it },
                                    label = { Text("Текущий баланс") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        Text(
                            text = "Ставки и прогнозы задаются вами и служат только для личного планирования.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    enabled = page < 2 || finalPageValid,
                    onClick = {
                        if (page < 2) {
                            page += 1
                        } else {
                            onComplete(
                                currency,
                                tax.toIntOrNull() ?: 0,
                                parseMinor(safe) ?: 0,
                                wallet,
                                parseMinor(balance) ?: 0,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Text(if (page < 2) "Продолжить" else "Начать")
                }

                if (page > 0) {
                    TextButton(
                        onClick = { page -= 1 },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Назад")
                    }
                }
            }
        }
    }
}

@Composable
private fun IntroPanel(
    icon: ImageVector,
    eyebrow: String,
    title: String,
    text: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        IconBubble(
            icon = icon,
            modifier = Modifier.size(54.dp),
        )
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PageDots(
    page: Int,
    count: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            Surface(
                modifier = Modifier.size(if (index == page) 18.dp else 7.dp, 7.dp),
                shape = CircleShape,
                color = if (index == page) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
            ) {}
        }
    }
}

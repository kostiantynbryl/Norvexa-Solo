package com.norvexa.flow.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.norvexa.flow.data.local.*
import com.norvexa.flow.data.settings.UserSettings
import com.norvexa.flow.domain.*
import com.norvexa.flow.ui.components.EmptyState
import com.norvexa.flow.ui.components.SectionHeader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ActivityScreen(transactions:List<TransactionEntity>,wallets:List<WalletEntity>,clients:List<ClientEntity>,baseCurrency:String,onDelete:(Long)->Unit){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp,20.dp,20.dp,110.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Text("Операции",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Фактические доходы и расходы",color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(wallets.isEmpty())item{EmptyState("Добавьте кошелёк","Без кошелька нельзя сохранить операцию")}
        if(transactions.isEmpty())item{EmptyState("Операций пока нет","Добавьте первый доход или расход через кнопку +")}
        items(transactions,key={it.id}){tx->
            val wallet=wallets.firstOrNull{it.id==tx.walletId};val client=clients.firstOrNull{it.id==tx.clientId};val date=Instant.ofEpochMilli(tx.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            Card{ListItem(headlineContent={Text(tx.category,fontWeight=FontWeight.SemiBold)},supportingContent={Text(listOfNotNull(wallet?.name,client?.name,date.toString(),tx.note.takeIf{it.isNotBlank()}).joinToString(" · "))},leadingContent={Icon(if(tx.type==TransactionType.INCOME)Icons.Rounded.SouthWest else Icons.Rounded.NorthEast,null,tint=if(tx.type==TransactionType.INCOME)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)},trailingContent={Column{Text((if(tx.type==TransactionType.INCOME)"+ " else "− ")+formatMoney(tx.amountMinor,tx.currency),fontWeight=FontWeight.Bold);IconButton(onClick={onDelete(tx.id)}){Icon(Icons.Rounded.DeleteOutline,"Удалить")}}})}
        }
    }
}

@Composable
fun ClientsScreen(clients:List<ClientEntity>,receivables:List<ReceivableEntity>,onPaid:(Long)->Unit,onDelete:(Long)->Unit){
    val open=receivables.filter{it.status!=ReceivableStatus.PAID&&it.status!=ReceivableStatus.CANCELLED}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp,20.dp,20.dp,110.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("Клиенты",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Ожидаемые и просроченные оплаты",color=MaterialTheme.colorScheme.onSurfaceVariant)}
        if(clients.isEmpty())item{EmptyState("Клиентов пока нет","Добавьте клиента через кнопку +")}
        items(clients,key={"c${it.id}"}){client->
            val debts=open.filter{it.clientId==client.id};val total=debts.sumOf{(it.amountMinor-it.receivedMinor).coerceAtLeast(0)}
            Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(client.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);if(client.email.isNotBlank())Text(client.email,style=MaterialTheme.typography.bodySmall)};Text(formatMoney(total,client.defaultCurrency),fontWeight=FontWeight.Bold)};if(debts.isEmpty())Text("Нет открытых оплат",color=MaterialTheme.colorScheme.onSurfaceVariant)else debts.forEach{r->HorizontalDivider();Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(r.title);Text("До ${LocalDate.ofEpochDay(r.expectedAtEpochDay)} · ${r.probabilityPercent}%",style=MaterialTheme.typography.bodySmall)};Text(formatMoney((r.amountMinor-r.receivedMinor).coerceAtLeast(0),r.currency));IconButton(onClick={onPaid(r.id)}){Icon(Icons.Rounded.Done,"Получено")};IconButton(onClick={onDelete(r.id)}){Icon(Icons.Rounded.DeleteOutline,"Удалить")}}}}}
        }
    }
}

@Composable
fun PlanningScreen(expenses:List<PlannedExpenseEntity>,reserves:List<ReserveEntity>,onCompleteExpense:(Long)->Unit,onDeleteExpense:(Long)->Unit,onUpdateReserve:(ReserveEntity)->Unit,onDeleteReserve:(Long)->Unit,onOpenPriceCalculator:()->Unit,onOpenMarginCalculator:()->Unit){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp,20.dp,20.dp,110.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("Планирование",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Расходы, резервы и цена вашей работы",color=MaterialTheme.colorScheme.onSurfaceVariant)}
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){ElevatedButton(onClick=onOpenPriceCalculator,modifier=Modifier.weight(1f)){Icon(Icons.Rounded.Calculate,null);Spacer(Modifier.width(6.dp));Text("Цена")};ElevatedButton(onClick=onOpenMarginCalculator,modifier=Modifier.weight(1f)){Icon(Icons.Rounded.Percent,null);Spacer(Modifier.width(6.dp));Text("Маржа")}}}
        item{SectionHeader("Финансовые резервы")}
        if(reserves.isEmpty())item{EmptyState("Резервов пока нет","Создайте налоговый резерв или финансовую подушку")}else items(reserves,key={"s${it.id}"}){r->Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(r.name,fontWeight=FontWeight.SemiBold);Text(if(r.isProtected)"Защищённый резерв" else "Не вычитается из доступного",style=MaterialTheme.typography.bodySmall)};Text("${formatMoney(r.currentMinor,r.currency)} / ${formatMoney(r.targetMinor,r.currency)}")};LinearProgressIndicator(progress={if(r.targetMinor<=0)0f else (r.currentMinor.toFloat()/r.targetMinor).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth());Row{TextButton(onClick={onUpdateReserve(r)}){Text("Изменить")};TextButton(onClick={onDeleteReserve(r.id)}){Text("Удалить")}}}}}
        item{SectionHeader("Будущие расходы")}
        if(expenses.filter{!it.isCompleted}.isEmpty())item{EmptyState("Нет будущих расходов","Добавьте аренду, подписки и другие обязательства")}else items(expenses.filter{!it.isCompleted},key={"p${it.id}"}){e->Card{ListItem(headlineContent={Text(e.title,fontWeight=FontWeight.SemiBold)},supportingContent={Text("${e.category} · ${LocalDate.ofEpochDay(e.dueAtEpochDay)}${if(e.recurrence!="NONE")" · повторяется" else ""}")},leadingContent={Text(formatMoney(e.amountMinor,e.currency),fontWeight=FontWeight.Bold)},trailingContent={Row{IconButton(onClick={onCompleteExpense(e.id)}){Icon(Icons.Rounded.Done,"Оплачено")};IconButton(onClick={onDeleteExpense(e.id)}){Icon(Icons.Rounded.DeleteOutline,"Удалить")}}})}}
    }
}

@Composable
fun MoreScreen(settings:UserSettings,onUpdateSettings:()->Unit,onSetTheme:(String)->Unit,onPrivacyMode:(Boolean)->Unit,onExportCsv:(Uri)->Unit,onExportPdf:(Uri)->Unit,onBackup:(Uri)->Unit,onRestore:(Uri)->Unit,onClearData:()->Unit){
    val csv=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){it?.let(onExportCsv)}
    val pdf=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){it?.let(onExportPdf)}
    val backup=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){it?.let(onBackup)}
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(onRestore)}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp,20.dp,20.dp,80.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{Text("Ещё",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Отчёты, резервные копии и конфиденциальность",color=MaterialTheme.colorScheme.onSurfaceVariant)}
        item{SectionHeader("Профиль");SettingsItem(Icons.Rounded.Tune,"Финансовые настройки","${settings.baseCurrency} · резерв ${settings.taxPercent}%",onUpdateSettings)}
        item{SectionHeader("Внешний вид")}
        item{Card{Column{SettingsItem(Icons.Rounded.BrightnessAuto,"Системная тема",null){onSetTheme("SYSTEM")};SettingsItem(Icons.Rounded.LightMode,"Светлая тема",null){onSetTheme("LIGHT")};SettingsItem(Icons.Rounded.DarkMode,"Тёмная тема",null){onSetTheme("DARK")}}}}
        item{SectionHeader("Экспорт и данные")}
        item{Card{Column{SettingsItem(Icons.Rounded.TableView,"Экспорт CSV","Операции для Excel и Google Sheets"){csv.launch("norvexa-flow-${LocalDate.now()}.csv")};SettingsItem(Icons.Rounded.PictureAsPdf,"Финансовый отчёт PDF","Сводка, оплаты и операции"){pdf.launch("norvexa-flow-report-${LocalDate.now()}.pdf")};SettingsItem(Icons.Rounded.Backup,"Создать резервную копию","Формат .nvxflow"){backup.launch("norvexa-flow-${LocalDate.now()}.nvxflow")};SettingsItem(Icons.Rounded.Restore,"Восстановить копию","Текущие данные будут заменены"){restore.launch(arrayOf("application/octet-stream","application/json","text/plain"))}}}}
        item{SectionHeader("Конфиденциальность")}
        item{Card{ListItem(headlineContent={Text("Защита экрана")},supportingContent={Text("Запретить скриншоты и скрыть суммы в обзоре приложений")},leadingContent={Icon(Icons.Rounded.Security,null)},trailingContent={Switch(settings.privacyMode,onPrivacyMode)})}}
        item{SectionHeader("О приложении")}
        item{Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Norvexa Flow 0.1.0 alpha",fontWeight=FontWeight.SemiBold);Text("Локальный финансовый помощник фрилансера. Не является бухгалтерской, налоговой, юридической, банковской или инвестиционной консультацией.",style=MaterialTheme.typography.bodySmall);OutlinedButton(onClick=onClearData,colors=ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.error)){Text("Удалить все финансовые данные")}}}}
    }
}

@Composable private fun SettingsItem(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String?,onClick:()->Unit){ListItem(headlineContent={Text(title)},supportingContent=subtitle?.let{{Text(it)}},leadingContent={Icon(icon,null)},trailingContent={Icon(Icons.Rounded.ChevronRight,null)},modifier=Modifier.fillMaxWidth(),colors=ListItemDefaults.colors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow));Box(Modifier.fillMaxWidth().height(1.dp));TextButton(onClick=onClick,modifier=Modifier.fillMaxWidth()){Text("Открыть: $title")}}

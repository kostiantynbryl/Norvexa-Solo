package com.norvexa.flow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.norvexa.flow.data.local.ClientEntity
import com.norvexa.flow.data.local.PlannedExpenseEntity
import com.norvexa.flow.data.local.ReceivableEntity
import com.norvexa.flow.domain.*
import com.norvexa.flow.ui.components.*
import java.time.LocalDate

@Composable
fun DashboardScreen(summary:DashboardSummary,currency:String,receivables:List<ReceivableEntity>,expenses:List<PlannedExpenseEntity>,clients:List<ClientEntity>,onPayReceivable:(Long)->Unit,onCompleteExpense:(Long)->Unit){
    val today=LocalDate.now().toEpochDay();val open=receivables.filter{it.status!=ReceivableStatus.PAID&&it.status!=ReceivableStatus.CANCELLED}.sortedBy{it.expectedAtEpochDay};val upcoming=expenses.filter{!it.isCompleted}.sortedBy{it.dueAtEpochDay}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp,20.dp,20.dp,110.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        item{Text("Финансовый обзор",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Что доступно сейчас и что произойдёт дальше",color=MaterialTheme.colorScheme.onSurfaceVariant)}
        item{MetricCard("Доступно сейчас",formatMoney(summary.availableNowMinor,currency),Modifier.fillMaxWidth(),"Баланс минус защищённые резервы и обязательные расходы 30 дней",true)}
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){MetricCard("Общий баланс",formatMoney(summary.totalBalanceMinor,currency),Modifier.weight(1f));MetricCard("Ожидается",formatMoney(summary.openReceivablesMinor,currency),Modifier.weight(1f),if(summary.overdueReceivablesMinor>0)"Просрочено ${formatMoney(summary.overdueReceivablesMinor,currency)}" else null)}}
        item{Card(colors=CardDefaults.cardColors(containerColor=if(summary.cashGap==null)MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.errorContainer)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Icon(if(summary.cashGap==null)Icons.Rounded.Check else Icons.Rounded.WarningAmber,null);Text(if(summary.cashGap==null)"Кассовый разрыв не прогнозируется" else "Возможен низкий остаток",fontWeight=FontWeight.SemiBold)};Text(if(summary.cashGap==null)"На горизонте 30 дней баланс остаётся выше безопасного уровня." else "${summary.cashGap.date}: прогноз ${formatMoney(summary.cashGap.balanceMinor,currency)} при безопасном уровне ${formatMoney(summary.cashGap.safeBalanceMinor,currency)}")}}}
        item{SectionHeader("Прогноз движения денег");Card{CashFlowChart(summary.forecast,currency,Modifier.padding(18.dp).fillMaxWidth())}}
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){MetricCard("Через 7 дней",formatMoney(summary.projected7Minor,currency),Modifier.weight(1f));MetricCard("Через 30 дней",formatMoney(summary.projected30Minor,currency),Modifier.weight(1f))}}
        item{SectionHeader("Ближайшие оплаты")}
        if(open.isEmpty())item{EmptyState("Нет ожидаемых оплат","Добавьте клиента и ожидаемую оплату через кнопку +")}else items(open.take(5),key={"r${it.id}"}){r->Card{ListItem(headlineContent={Text(r.title)},supportingContent={Text("${clients.firstOrNull{c->c.id==r.clientId}?.name?:"Клиент"} · ${LocalDate.ofEpochDay(r.expectedAtEpochDay)}${if(r.expectedAtEpochDay<today)" · просрочено" else ""}")},trailingContent={TextButton(onClick={onPayReceivable(r.id)}){Text("Получено")}},leadingContent={Text(formatMoney((r.amountMinor-r.receivedMinor).coerceAtLeast(0),r.currency),fontWeight=FontWeight.Bold)})}}
        item{SectionHeader("Ближайшие расходы")}
        if(upcoming.isEmpty())item{EmptyState("Нет будущих расходов","Добавьте обязательные и повторяющиеся платежи")}else items(upcoming.take(5),key={"e${it.id}"}){e->Card{ListItem(headlineContent={Text(e.title)},supportingContent={Text("${e.category} · ${LocalDate.ofEpochDay(e.dueAtEpochDay)}")},leadingContent={Text(formatMoney(e.amountMinor,e.currency),fontWeight=FontWeight.Bold)},trailingContent={TextButton(onClick={onCompleteExpense(e.id)}){Text("Оплачено")}})}}
    }
}

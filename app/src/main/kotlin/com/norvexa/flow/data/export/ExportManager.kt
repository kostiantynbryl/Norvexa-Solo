package com.norvexa.flow.data.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.norvexa.flow.data.settings.UserSettings
import com.norvexa.flow.domain.FinanceData
import com.norvexa.flow.domain.FinancialCalculator
import com.norvexa.flow.domain.ReceivableStatus
import com.norvexa.flow.domain.TransactionType
import com.norvexa.flow.domain.formatMoney
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ExportManager {
    fun writeCsv(context: Context, uri: Uri, data: FinanceData) {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                writer.appendLine("type,date,wallet,client,category,amount,currency,note")
                data.transactions.forEach { tx ->
                    val date = Instant.ofEpochMilli(tx.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                    val wallet = data.wallets.firstOrNull { it.id == tx.walletId }?.name.orEmpty()
                    val client = data.clients.firstOrNull { it.id == tx.clientId }?.name.orEmpty()
                    writer.appendLine(listOf(tx.type,date.toString(),wallet,client,tx.category,(tx.amountMinor/100.0).toString(),tx.currency,tx.note).joinToString(",") { csv(it) })
                }
            }
        } ?: error("Cannot open destination")
    }
    fun writeBackup(context: Context, uri: Uri, data: FinanceData) { context.contentResolver.openOutputStream(uri)?.use { it.write(BackupCodec.encode(data).toByteArray(Charsets.UTF_8)) } ?: error("Cannot open destination") }
    fun readBackup(context: Context, uri: Uri): FinanceData { val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: error("Cannot read backup"); return BackupCodec.decode(text) }
    fun writePdf(context: Context, uri: Uri, data: FinanceData, settings: UserSettings) {
        val summary = FinancialCalculator.dashboard(data.wallets,data.transactions,data.receivables,data.plannedExpenses,data.reserves,settings.taxPercent,settings.safeBalanceMinor)
        val doc = PdfDocument(); val page = doc.startPage(PdfDocument.PageInfo.Builder(595,842,1).create()); val canvas = page.canvas
        val title = Paint().apply { textSize=22f; isFakeBoldText=true }; val heading=Paint().apply{textSize=14f;isFakeBoldText=true}; val body=Paint().apply{textSize=11f}; var y=48f
        canvas.drawText("Norvexa Flow — финансовый отчёт",40f,y,title); y+=30f; canvas.drawText("Дата: ${LocalDate.now()}",40f,y,body); y+=28f; canvas.drawText("Сводка",40f,y,heading); y+=22f
        listOf("Общий баланс: ${formatMoney(summary.totalBalanceMinor,settings.baseCurrency)}","Доступно сейчас: ${formatMoney(summary.availableNowMinor,settings.baseCurrency)}","Защищённые резервы: ${formatMoney(summary.protectedReservesMinor,settings.baseCurrency)}","Ожидаемые оплаты: ${formatMoney(summary.openReceivablesMinor,settings.baseCurrency)}","Плановые расходы 30 дней: ${formatMoney(summary.mandatoryExpenses30Minor,settings.baseCurrency)}","Прогноз через 30 дней: ${formatMoney(summary.projected30Minor,settings.baseCurrency)}").forEach { canvas.drawText(it,40f,y,body); y+=18f }
        y+=16f; canvas.drawText("Открытые оплаты",40f,y,heading); y+=22f
        data.receivables.filter { it.status!=ReceivableStatus.PAID && it.status!=ReceivableStatus.CANCELLED }.take(15).forEach { r -> val c=data.clients.firstOrNull{it.id==r.clientId}?.name?:"Клиент"; canvas.drawText("$c — ${r.title}: ${formatMoney((r.amountMinor-r.receivedMinor).coerceAtLeast(0),r.currency)} до ${LocalDate.ofEpochDay(r.expectedAtEpochDay)}",40f,y,body); y+=17f }
        y+=14f; canvas.drawText("Последние операции",40f,y,heading); y+=22f
        data.transactions.take(15).forEach { tx -> canvas.drawText("${if(tx.type==TransactionType.INCOME) "+" else "−"} ${formatMoney(tx.amountMinor,tx.currency)} — ${tx.category}",40f,y,body); y+=17f }
        canvas.drawText("Документ предназначен для личного планирования и не является бухгалтерской или налоговой отчётностью.",40f,810f,Paint().apply{textSize=8f})
        doc.finishPage(page); context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) } ?: error("Cannot open destination"); doc.close()
    }
    private fun csv(value:String)="\"${value.replace("\"","\"\"")}\""
}

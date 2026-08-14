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
import com.norvexa.flow.domain.minorToDecimal
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ExportManager {
    private const val MAX_BACKUP_BYTES = 25 * 1024 * 1024

    fun writeCsv(context: Context, uri: Uri, data: FinanceData) {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                writer.append('\uFEFF')
                writer.appendLine("type,date,wallet,client,category,amount,currency,note")
                data.transactions.forEach { tx ->
                    val date = Instant.ofEpochMilli(tx.occurredAtEpochMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    val wallet = data.wallets.firstOrNull { it.id == tx.walletId }?.name.orEmpty()
                    val client = data.clients.firstOrNull { it.id == tx.clientId }?.name.orEmpty()
                    val amount = minorToDecimal(tx.amountMinor, tx.currency).toPlainString()
                    writer.appendLine(
                        listOf(
                            tx.type,
                            date.toString(),
                            wallet,
                            client,
                            tx.category,
                            amount,
                            tx.currency,
                            tx.note,
                        ).joinToString(",") { csv(it) },
                    )
                }
            }
        } ?: error("Не удалось открыть файл для CSV")
    }

    fun writeBackup(
        context: Context,
        uri: Uri,
        data: FinanceData,
        settings: UserSettings,
    ) {
        context.contentResolver.openOutputStream(uri)?.use {
            it.write(BackupCodec.encode(data, settings).toByteArray(Charsets.UTF_8))
        } ?: error("Не удалось открыть файл резервной копии")
    }

    fun readBackup(context: Context, uri: Uri): BackupPayload {
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Не удалось прочитать резервную копию")
        val bytes = input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                require(total <= MAX_BACKUP_BYTES) { "Резервная копия слишком большая" }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }
        return BackupCodec.decode(bytes.toString(Charsets.UTF_8))
    }

    fun writePdf(context: Context, uri: Uri, data: FinanceData, settings: UserSettings) {
        val summary = FinancialCalculator.dashboard(
            wallets = data.wallets,
            transactions = data.transactions,
            receivables = data.receivables,
            expenses = data.plannedExpenses,
            reserves = data.reserves,
            taxPercent = settings.taxPercent,
            safeBalanceMinor = settings.safeBalanceMinor,
            baseCurrency = settings.baseCurrency,
        )

        val document = PdfDocument()
        try {
            val pdf = PdfWriter(document)
            pdf.heading("Norvexa Flow — финансовый отчёт", 22f)
            pdf.line("Дата: ${LocalDate.now()}")
            pdf.space(8f)
            pdf.heading("Сводка")
            listOf(
                "Общий баланс: ${formatMoney(summary.totalBalanceMinor, settings.baseCurrency)}",
                "Доступно сейчас: ${formatMoney(summary.availableNowMinor, settings.baseCurrency)}",
                "Защищённые резервы: ${formatMoney(summary.protectedReservesMinor, settings.baseCurrency)}",
                "Плановый налоговый резерв месяца: ${formatMoney(summary.suggestedTaxReserveMinor, settings.baseCurrency)}",
                "Ожидаемые оплаты: ${formatMoney(summary.openReceivablesMinor, settings.baseCurrency)}",
                "Плановые расходы 30 дней: ${formatMoney(summary.mandatoryExpenses30Minor, settings.baseCurrency)}",
                "Прогноз через 30 дней: ${formatMoney(summary.projected30Minor, settings.baseCurrency)}",
            ).forEach(pdf::line)

            pdf.space(12f)
            pdf.heading("Открытые оплаты")
            val openReceivables = data.receivables.filter {
                it.status != ReceivableStatus.PAID && it.status != ReceivableStatus.CANCELLED
            }
            if (openReceivables.isEmpty()) {
                pdf.line("Нет открытых оплат")
            } else {
                openReceivables.forEach { receivable ->
                    val client = data.clients.firstOrNull { it.id == receivable.clientId }?.name ?: "Клиент"
                    pdf.line(
                        "$client — ${receivable.title}: " +
                            "${formatMoney((receivable.amountMinor - receivable.receivedMinor).coerceAtLeast(0), receivable.currency)} " +
                            "до ${LocalDate.ofEpochDay(receivable.expectedAtEpochDay)}",
                    )
                }
            }

            pdf.space(12f)
            pdf.heading("Операции")
            if (data.transactions.isEmpty()) {
                pdf.line("Нет операций")
            } else {
                data.transactions.forEach { tx ->
                    val sign = if (tx.type == TransactionType.INCOME) "+" else "−"
                    val date = Instant.ofEpochMilli(tx.occurredAtEpochMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    pdf.line("$date · $sign ${formatMoney(tx.amountMinor, tx.currency)} · ${tx.category}")
                }
            }

            pdf.finish()
            context.contentResolver.openOutputStream(uri)?.use { document.writeTo(it) }
                ?: error("Не удалось открыть файл для PDF")
        } finally {
            document.close()
        }
    }

    private fun csv(value: String): String {
        val safe = if (value.firstOrNull() in setOf('=', '+', '-', '@', '\t', '\r')) "'$value" else value
        return "\"${safe.replace("\"", "\"\"")}\""
    }

    private class PdfWriter(private val document: PdfDocument) {
        private val body = Paint().apply { textSize = 10.5f; isAntiAlias = true }
        private val heading = Paint().apply { textSize = 14f; isFakeBoldText = true; isAntiAlias = true }
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var y = 0f

        init {
            newPage()
        }

        fun heading(text: String, size: Float = 14f) {
            ensureSpace(28f)
            heading.textSize = size
            drawWrapped(text, heading, lineHeight = size + 5f)
            y += 5f
        }

        fun line(text: String) {
            ensureSpace(20f)
            drawWrapped(text, body, lineHeight = 15f)
        }

        fun space(value: Float) {
            ensureSpace(value)
            y += value
        }

        fun finish() {
            finishCurrentPage()
        }

        private fun newPage() {
            finishCurrentPage()
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            y = 44f
        }

        private fun finishCurrentPage() {
            val current = page ?: return
            val footer = Paint().apply { textSize = 7.5f; isAntiAlias = true }
            current.canvas.drawText(
                "Norvexa Flow · личное планирование, не бухгалтерская или налоговая отчётность · стр. $pageNumber",
                40f,
                815f,
                footer,
            )
            document.finishPage(current)
            page = null
        }

        private fun ensureSpace(required: Float) {
            if (y + required > 790f) newPage()
        }

        private fun drawWrapped(text: String, paint: Paint, lineHeight: Float) {
            val maxWidth = 515f
            var rest = text
            while (rest.isNotEmpty()) {
                ensureSpace(lineHeight)
                val canvas = page?.canvas ?: return
                var count = paint.breakText(rest, true, maxWidth, null).coerceAtLeast(1)
                if (count < rest.length) {
                    val split = rest.lastIndexOf(' ', startIndex = count - 1)
                    if (split > 0) count = split
                }
                val line = rest.take(count).trimEnd()
                canvas.drawText(line, 40f, y, paint)
                y += lineHeight
                rest = rest.drop(count).trimStart()
            }
        }
    }
}

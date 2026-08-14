package com.norvexa.flow.data.repository

import androidx.room.withTransaction
import com.norvexa.flow.data.local.ClientEntity
import com.norvexa.flow.data.local.FinanceDao
import com.norvexa.flow.data.local.NorvexaDatabase
import com.norvexa.flow.data.local.PlannedExpenseEntity
import com.norvexa.flow.data.local.ReceivableEntity
import com.norvexa.flow.data.local.ReserveEntity
import com.norvexa.flow.data.local.TransactionEntity
import com.norvexa.flow.data.local.WalletEntity
import com.norvexa.flow.domain.FinanceData
import com.norvexa.flow.domain.FinancialCalculator
import com.norvexa.flow.domain.ReceivableStatus
import com.norvexa.flow.domain.TransactionType
import com.norvexa.flow.domain.isValidCurrencyCode
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class FinanceRepository(
    private val database: NorvexaDatabase,
    private val dao: FinanceDao,
) {
    private data class PrimaryData(
        val wallets: List<WalletEntity>,
        val transactions: List<TransactionEntity>,
        val clients: List<ClientEntity>,
    )

    private data class PlanningData(
        val receivables: List<ReceivableEntity>,
        val expenses: List<PlannedExpenseEntity>,
        val reserves: List<ReserveEntity>,
    )

    val financeData: Flow<FinanceData> = combine(
        combine(
            dao.observeWallets(),
            dao.observeTransactions(),
            dao.observeClients(),
        ) { wallets, transactions, clients ->
            PrimaryData(wallets, transactions, clients)
        },
        combine(
            dao.observeReceivables(),
            dao.observePlannedExpenses(),
            dao.observeReserves(),
        ) { receivables, expenses, reserves ->
            PlanningData(receivables, expenses, reserves)
        },
    ) { primary, planning ->
        FinanceData(
            primary.wallets,
            primary.transactions,
            primary.clients,
            planning.receivables,
            planning.expenses,
            planning.reserves,
        )
    }

    suspend fun addWallet(
        name: String,
        currency: String,
        balanceMinor: Long,
        rateToBaseMicros: Long,
    ): Long {
        require(name.isNotBlank())
        require(isValidCurrencyCode(currency)) { "Некорректный код валюты" }
        require(rateToBaseMicros > 0)
        return dao.insertWallet(
            WalletEntity(
                name = name.trim(),
                currency = currency.uppercase(),
                balanceMinor = balanceMinor,
                rateToBaseMicros = rateToBaseMicros,
            ),
        )
    }

    suspend fun addTransaction(
        walletId: Long,
        type: String,
        amountMinor: Long,
        category: String,
        note: String,
        clientId: Long? = null,
        occurredAtEpochMillis: Long = System.currentTimeMillis(),
    ) {
        require(type == TransactionType.INCOME || type == TransactionType.EXPENSE)
        require(amountMinor > 0)
        database.withTransaction {
            val wallet = dao.getWallet(walletId) ?: error("Кошелёк не найден")
            require(wallet.isActive) { "Кошелёк неактивен" }
            val signed = if (type == TransactionType.INCOME) amountMinor else -amountMinor
            dao.updateWallet(wallet.copy(balanceMinor = Math.addExact(wallet.balanceMinor, signed)))
            dao.insertTransaction(
                TransactionEntity(
                    walletId = walletId,
                    clientId = clientId,
                    type = type,
                    amountMinor = amountMinor,
                    currency = wallet.currency,
                    rateToBaseMicros = wallet.rateToBaseMicros,
                    category = category.trim().ifEmpty { "Другое" },
                    note = note.trim(),
                    occurredAtEpochMillis = occurredAtEpochMillis,
                ),
            )
        }
    }

    suspend fun deleteTransaction(id: Long) {
        database.withTransaction {
            val transaction = dao.getTransaction(id) ?: return@withTransaction
            require(transaction.sourceType == null) {
                "Автоматическую операцию нельзя удалить отдельно от связанного платежа"
            }
            val wallet = dao.getWallet(transaction.walletId)
                ?: error("Кошелёк операции не найден")
            val rollback = if (transaction.type == TransactionType.INCOME) {
                -transaction.amountMinor
            } else {
                transaction.amountMinor
            }
            dao.updateWallet(wallet.copy(balanceMinor = Math.addExact(wallet.balanceMinor, rollback)))
            dao.deleteTransaction(transaction)
        }
    }

    suspend fun addClient(name: String, email: String, currency: String, note: String): Long {
        require(name.isNotBlank())
        require(isValidCurrencyCode(currency)) { "Некорректный код валюты" }
        return dao.insertClient(
            ClientEntity(
                name = name.trim(),
                email = email.trim(),
                defaultCurrency = currency.uppercase(),
                note = note.trim(),
            ),
        )
    }

    suspend fun addReceivable(value: ReceivableEntity): Long {
        require(value.amountMinor > 0)
        require(value.receivedMinor in 0..value.amountMinor)
        require(value.rateToBaseMicros > 0)
        require(value.probabilityPercent in 0..100)
        require(isValidCurrencyCode(value.currency)) { "Некорректный код валюты" }
        return dao.insertReceivable(value)
    }

    suspend fun settleReceivable(
        id: Long,
        walletId: Long,
        baseCurrency: String,
        amountMinor: Long? = null,
    ) {
        database.withTransaction {
            val receivable = dao.getReceivable(id) ?: error("Ожидаемая оплата не найдена")
            require(receivable.status != ReceivableStatus.PAID && receivable.status != ReceivableStatus.CANCELLED) {
                "Оплата уже закрыта"
            }
            val wallet = dao.getWallet(walletId) ?: error("Кошелёк не найден")
            require(wallet.isActive) { "Кошелёк неактивен" }

            val remaining = (receivable.amountMinor - receivable.receivedMinor).coerceAtLeast(0)
            require(remaining > 0) { "Оплата уже получена" }
            val receivedNow = amountMinor ?: remaining
            require(receivedNow in 1..remaining) { "Некорректная сумма оплаты" }

            val baseAmount = FinancialCalculator.toBaseMinor(
                receivedNow,
                receivable.rateToBaseMicros,
                receivable.currency,
                baseCurrency,
            )
            val walletAmount = FinancialCalculator.fromBaseMinor(
                baseAmount,
                wallet.rateToBaseMicros,
                wallet.currency,
                baseCurrency,
            )
            require(walletAmount > 0) { "Сумма слишком мала для валюты кошелька" }

            dao.updateWallet(
                wallet.copy(balanceMinor = Math.addExact(wallet.balanceMinor, walletAmount)),
            )
            dao.insertTransaction(
                TransactionEntity(
                    walletId = wallet.id,
                    clientId = receivable.clientId,
                    type = TransactionType.INCOME,
                    amountMinor = walletAmount,
                    currency = wallet.currency,
                    rateToBaseMicros = wallet.rateToBaseMicros,
                    category = "Оплата клиента",
                    note = receivable.title,
                    sourceType = SOURCE_RECEIVABLE,
                    sourceId = receivable.id,
                ),
            )

            val totalReceived = receivable.receivedMinor + receivedNow
            dao.updateReceivable(
                receivable.copy(
                    receivedMinor = totalReceived,
                    status = if (totalReceived >= receivable.amountMinor) {
                        ReceivableStatus.PAID
                    } else {
                        ReceivableStatus.PARTIAL
                    },
                ),
            )
        }
    }

    suspend fun deleteReceivable(id: Long) {
        dao.getReceivable(id)?.let { dao.deleteReceivable(it) }
    }

    suspend fun addPlannedExpense(value: PlannedExpenseEntity): Long {
        require(value.amountMinor > 0)
        require(value.rateToBaseMicros > 0)
        require(value.recurrence in setOf("NONE", "MONTHLY", "YEARLY"))
        require(isValidCurrencyCode(value.currency)) { "Некорректный код валюты" }
        return dao.insertPlannedExpense(value)
    }

    suspend fun settlePlannedExpense(
        id: Long,
        walletId: Long,
        baseCurrency: String,
    ) {
        database.withTransaction {
            val expense = dao.getPlannedExpense(id) ?: error("Плановый расход не найден")
            require(!expense.isCompleted) { "Расход уже закрыт" }
            val wallet = dao.getWallet(walletId) ?: error("Кошелёк не найден")
            require(wallet.isActive) { "Кошелёк неактивен" }

            val baseAmount = FinancialCalculator.toBaseMinor(
                expense.amountMinor,
                expense.rateToBaseMicros,
                expense.currency,
                baseCurrency,
            )
            val walletAmount = FinancialCalculator.fromBaseMinor(
                baseAmount,
                wallet.rateToBaseMicros,
                wallet.currency,
                baseCurrency,
            )
            require(walletAmount > 0) { "Сумма слишком мала для валюты кошелька" }

            dao.updateWallet(
                wallet.copy(balanceMinor = Math.subtractExact(wallet.balanceMinor, walletAmount)),
            )
            dao.insertTransaction(
                TransactionEntity(
                    walletId = wallet.id,
                    type = TransactionType.EXPENSE,
                    amountMinor = walletAmount,
                    currency = wallet.currency,
                    rateToBaseMicros = wallet.rateToBaseMicros,
                    category = expense.category.trim().ifEmpty { "Плановый расход" },
                    note = expense.title,
                    sourceType = SOURCE_PLANNED_EXPENSE,
                    sourceId = expense.id,
                ),
            )

            if (expense.recurrence == "NONE") {
                dao.updatePlannedExpense(expense.copy(isCompleted = true))
            } else {
                val currentDue = LocalDate.ofEpochDay(expense.dueAtEpochDay)
                val nextDue = when (expense.recurrence) {
                    "MONTHLY" -> currentDue.plusMonths(1)
                    "YEARLY" -> currentDue.plusYears(1)
                    else -> currentDue
                }
                dao.updatePlannedExpense(
                    expense.copy(
                        dueAtEpochDay = nextDue.toEpochDay(),
                        isCompleted = false,
                    ),
                )
            }
        }
    }

    suspend fun deletePlannedExpense(id: Long) {
        dao.getPlannedExpense(id)?.let { dao.deletePlannedExpense(it) }
    }

    suspend fun addReserve(value: ReserveEntity): Long {
        require(value.targetMinor > 0)
        require(value.currentMinor >= 0)
        require(value.rateToBaseMicros > 0)
        require(isValidCurrencyCode(value.currency)) { "Некорректный код валюты" }
        return dao.insertReserve(value)
    }

    suspend fun updateReserveAmount(id: Long, newAmountMinor: Long) {
        dao.getReserves().firstOrNull { it.id == id }?.let {
            dao.updateReserve(it.copy(currentMinor = newAmountMinor.coerceAtLeast(0)))
        }
    }

    suspend fun deleteReserve(id: Long) {
        dao.getReserves().firstOrNull { it.id == id }?.let { dao.deleteReserve(it) }
    }

    suspend fun snapshot(): FinanceData = FinanceData(
        dao.getWallets(),
        dao.getTransactions(),
        dao.getClients(),
        dao.getReceivables(),
        dao.getPlannedExpenses(),
        dao.getReserves(),
    )

    suspend fun replaceAll(data: FinanceData) {
        validateSnapshot(data)
        database.withTransaction {
            dao.clearTransactions()
            dao.clearReceivables()
            dao.clearPlannedExpenses()
            dao.clearReserves()
            dao.clearClients()
            dao.clearWallets()
            dao.insertWallets(data.wallets)
            dao.insertClients(data.clients)
            dao.insertTransactions(data.transactions)
            dao.insertReceivables(data.receivables)
            dao.insertPlannedExpenses(data.plannedExpenses)
            dao.insertReserves(data.reserves)
        }
    }

    suspend fun clearAll() = replaceAll(FinanceData())

    private fun validateSnapshot(data: FinanceData) {
        require(data.wallets.map { it.id }.filter { it != 0L }.distinct().size == data.wallets.count { it.id != 0L }) {
            "Дублирующиеся ID кошельков"
        }
        require(data.clients.map { it.id }.filter { it != 0L }.distinct().size == data.clients.count { it.id != 0L }) {
            "Дублирующиеся ID клиентов"
        }
        val walletIds = data.wallets.map { it.id }.toSet()
        val clientIds = data.clients.map { it.id }.toSet()
        val receivableIds = data.receivables.map { it.id }.toSet()
        val plannedExpenseIds = data.plannedExpenses.map { it.id }.toSet()

        require(data.wallets.all {
            it.rateToBaseMicros > 0 && isValidCurrencyCode(it.currency)
        }) { "Некорректные кошельки в резервной копии" }
        require(data.clients.all { isValidCurrencyCode(it.defaultCurrency) }) {
            "Некорректные валюты клиентов в резервной копии"
        }
        require(data.transactions.all { transaction ->
            val sourceValid = when (transaction.sourceType) {
                null -> transaction.sourceId == null
                SOURCE_RECEIVABLE -> transaction.sourceId in receivableIds
                SOURCE_PLANNED_EXPENSE -> transaction.sourceId in plannedExpenseIds
                else -> false
            }
            transaction.walletId in walletIds &&
                (transaction.clientId == null || transaction.clientId in clientIds) &&
                transaction.amountMinor > 0 &&
                transaction.rateToBaseMicros > 0 &&
                transaction.type in setOf(TransactionType.INCOME, TransactionType.EXPENSE) &&
                isValidCurrencyCode(transaction.currency) &&
                sourceValid
        }) { "Некорректные операции в резервной копии" }
        require(data.receivables.all {
            it.clientId in clientIds &&
                it.amountMinor > 0 &&
                it.receivedMinor in 0..it.amountMinor &&
                it.rateToBaseMicros > 0 &&
                it.probabilityPercent in 0..100 &&
                isValidCurrencyCode(it.currency) &&
                it.status in setOf(
                    ReceivableStatus.EXPECTED,
                    ReceivableStatus.PARTIAL,
                    ReceivableStatus.PAID,
                    ReceivableStatus.CANCELLED,
                )
        }) { "Некорректные ожидаемые оплаты в резервной копии" }
        require(data.plannedExpenses.all {
            it.amountMinor > 0 &&
                it.rateToBaseMicros > 0 &&
                isValidCurrencyCode(it.currency) &&
                it.recurrence in setOf("NONE", "MONTHLY", "YEARLY")
        }) { "Некорректные плановые расходы в резервной копии" }
        require(data.reserves.all {
            it.targetMinor > 0 &&
                it.currentMinor >= 0 &&
                it.rateToBaseMicros > 0 &&
                isValidCurrencyCode(it.currency)
        }) { "Некорректные резервы в резервной копии" }
    }

    companion object {
        const val SOURCE_RECEIVABLE = "RECEIVABLE"
        const val SOURCE_PLANNED_EXPENSE = "PLANNED_EXPENSE"
    }
}

package com.norvexa.flow.data.export

import com.norvexa.flow.data.local.ClientEntity
import com.norvexa.flow.data.local.PlannedExpenseEntity
import com.norvexa.flow.data.local.ReceivableEntity
import com.norvexa.flow.data.local.ReserveEntity
import com.norvexa.flow.data.local.TransactionEntity
import com.norvexa.flow.data.local.WalletEntity
import com.norvexa.flow.data.settings.UserSettings
import com.norvexa.flow.domain.FinanceData
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

data class BackupFinancialSettings(
    val baseCurrency: String,
    val taxPercent: Int,
    val safeBalanceMinor: Long,
)

data class BackupPayload(
    val data: FinanceData,
    val settings: BackupFinancialSettings?,
    val version: Int,
)

object BackupCodec {
    private const val FORMAT = "norvexa-flow"
    private const val CURRENT_VERSION = 2

    fun encode(data: FinanceData, settings: UserSettings): String = JSONObject().apply {
        put("format", FORMAT)
        put("version", CURRENT_VERSION)
        put("createdAt", Instant.now().toString())
        put("settings", JSONObject().apply {
            put("baseCurrency", settings.baseCurrency)
            put("taxPercent", settings.taxPercent)
            put("safeBalanceMinor", settings.safeBalanceMinor)
        })
        put("wallets", JSONArray().apply { data.wallets.forEach { put(walletToJson(it)) } })
        put("transactions", JSONArray().apply { data.transactions.forEach { put(transactionToJson(it)) } })
        put("clients", JSONArray().apply { data.clients.forEach { put(clientToJson(it)) } })
        put("receivables", JSONArray().apply { data.receivables.forEach { put(receivableToJson(it)) } })
        put("plannedExpenses", JSONArray().apply { data.plannedExpenses.forEach { put(expenseToJson(it)) } })
        put("reserves", JSONArray().apply { data.reserves.forEach { put(reserveToJson(it)) } })
    }.toString(2)

    fun decode(text: String): BackupPayload {
        require(text.toByteArray(Charsets.UTF_8).size <= 25 * 1024 * 1024) {
            "Резервная копия слишком большая"
        }
        val root = JSONObject(text)
        require(root.optString("format") == FORMAT) { "Неподдерживаемый формат резервной копии" }
        val version = root.optInt("version", 1)
        require(version in 1..CURRENT_VERSION) { "Неподдерживаемая версия резервной копии: $version" }

        val settings = if (version >= 2 && root.has("settings")) {
            val json = root.getJSONObject("settings")
            BackupFinancialSettings(
                baseCurrency = json.getString("baseCurrency").uppercase(),
                taxPercent = json.getInt("taxPercent"),
                safeBalanceMinor = json.getLong("safeBalanceMinor"),
            ).also {
                require(it.baseCurrency.length == 3)
                require(it.taxPercent in 0..95)
                require(it.safeBalanceMinor >= 0)
            }
        } else {
            null
        }

        val data = FinanceData(
            root.requiredArray("wallets").objects().map(::walletFromJson),
            root.requiredArray("transactions").objects().map(::transactionFromJson),
            root.requiredArray("clients").objects().map(::clientFromJson),
            root.requiredArray("receivables").objects().map(::receivableFromJson),
            root.requiredArray("plannedExpenses").objects().map(::expenseFromJson),
            root.requiredArray("reserves").objects().map(::reserveFromJson),
        )
        return BackupPayload(data = data, settings = settings, version = version)
    }

    private fun JSONObject.requiredArray(name: String): JSONArray =
        getJSONArray(name).also { require(it.length() <= 100_000) { "Слишком много записей: $name" } }

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).map { getJSONObject(it) }

    private fun walletToJson(v: WalletEntity) = JSONObject().apply {
        put("id", v.id)
        put("name", v.name)
        put("currency", v.currency)
        put("balanceMinor", v.balanceMinor)
        put("rateToBaseMicros", v.rateToBaseMicros)
        put("isActive", v.isActive)
    }

    private fun walletFromJson(v: JSONObject) = WalletEntity(
        id = v.getLong("id"),
        name = v.getString("name"),
        currency = v.getString("currency"),
        balanceMinor = v.getLong("balanceMinor"),
        rateToBaseMicros = v.getLong("rateToBaseMicros"),
        isActive = v.optBoolean("isActive", true),
    )

    private fun transactionToJson(v: TransactionEntity) = JSONObject().apply {
        put("id", v.id)
        put("walletId", v.walletId)
        put("clientId", v.clientId ?: JSONObject.NULL)
        put("type", v.type)
        put("amountMinor", v.amountMinor)
        put("currency", v.currency)
        put("rateToBaseMicros", v.rateToBaseMicros)
        put("category", v.category)
        put("note", v.note)
        put("occurredAtEpochMillis", v.occurredAtEpochMillis)
    }

    private fun transactionFromJson(v: JSONObject) = TransactionEntity(
        id = v.getLong("id"),
        walletId = v.getLong("walletId"),
        clientId = if (v.isNull("clientId")) null else v.getLong("clientId"),
        type = v.getString("type"),
        amountMinor = v.getLong("amountMinor"),
        currency = v.getString("currency"),
        rateToBaseMicros = v.getLong("rateToBaseMicros"),
        category = v.getString("category"),
        note = v.optString("note"),
        occurredAtEpochMillis = v.getLong("occurredAtEpochMillis"),
    )

    private fun clientToJson(v: ClientEntity) = JSONObject().apply {
        put("id", v.id)
        put("name", v.name)
        put("email", v.email)
        put("defaultCurrency", v.defaultCurrency)
        put("note", v.note)
        put("isActive", v.isActive)
    }

    private fun clientFromJson(v: JSONObject) = ClientEntity(
        id = v.getLong("id"),
        name = v.getString("name"),
        email = v.optString("email"),
        defaultCurrency = v.getString("defaultCurrency"),
        note = v.optString("note"),
        isActive = v.optBoolean("isActive", true),
    )

    private fun receivableToJson(v: ReceivableEntity) = JSONObject().apply {
        put("id", v.id)
        put("clientId", v.clientId)
        put("title", v.title)
        put("amountMinor", v.amountMinor)
        put("receivedMinor", v.receivedMinor)
        put("currency", v.currency)
        put("rateToBaseMicros", v.rateToBaseMicros)
        put("expectedAtEpochDay", v.expectedAtEpochDay)
        put("probabilityPercent", v.probabilityPercent)
        put("status", v.status)
        put("note", v.note)
    }

    private fun receivableFromJson(v: JSONObject) = ReceivableEntity(
        id = v.getLong("id"),
        clientId = v.getLong("clientId"),
        title = v.getString("title"),
        amountMinor = v.getLong("amountMinor"),
        receivedMinor = v.optLong("receivedMinor"),
        currency = v.getString("currency"),
        rateToBaseMicros = v.getLong("rateToBaseMicros"),
        expectedAtEpochDay = v.getLong("expectedAtEpochDay"),
        probabilityPercent = v.optInt("probabilityPercent", 100),
        status = v.optString("status", "EXPECTED"),
        note = v.optString("note"),
    )

    private fun expenseToJson(v: PlannedExpenseEntity) = JSONObject().apply {
        put("id", v.id)
        put("title", v.title)
        put("amountMinor", v.amountMinor)
        put("currency", v.currency)
        put("rateToBaseMicros", v.rateToBaseMicros)
        put("dueAtEpochDay", v.dueAtEpochDay)
        put("category", v.category)
        put("isMandatory", v.isMandatory)
        put("isCompleted", v.isCompleted)
        put("recurrence", v.recurrence)
        put("note", v.note)
    }

    private fun expenseFromJson(v: JSONObject) = PlannedExpenseEntity(
        id = v.getLong("id"),
        title = v.getString("title"),
        amountMinor = v.getLong("amountMinor"),
        currency = v.getString("currency"),
        rateToBaseMicros = v.getLong("rateToBaseMicros"),
        dueAtEpochDay = v.getLong("dueAtEpochDay"),
        category = v.getString("category"),
        isMandatory = v.optBoolean("isMandatory", true),
        isCompleted = v.optBoolean("isCompleted", false),
        recurrence = v.optString("recurrence", "NONE"),
        note = v.optString("note"),
    )

    private fun reserveToJson(v: ReserveEntity) = JSONObject().apply {
        put("id", v.id)
        put("name", v.name)
        put("targetMinor", v.targetMinor)
        put("currentMinor", v.currentMinor)
        put("currency", v.currency)
        put("rateToBaseMicros", v.rateToBaseMicros)
        put("type", v.type)
        put("isProtected", v.isProtected)
    }

    private fun reserveFromJson(v: JSONObject) = ReserveEntity(
        id = v.getLong("id"),
        name = v.getString("name"),
        targetMinor = v.getLong("targetMinor"),
        currentMinor = v.getLong("currentMinor"),
        currency = v.getString("currency"),
        rateToBaseMicros = v.getLong("rateToBaseMicros"),
        type = v.optString("type", "CUSTOM"),
        isProtected = v.optBoolean("isProtected", true),
    )
}

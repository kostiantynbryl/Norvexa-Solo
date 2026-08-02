package com.norvexa.flow.data.export

import com.norvexa.flow.data.local.ClientEntity
import com.norvexa.flow.data.local.PlannedExpenseEntity
import com.norvexa.flow.data.local.ReceivableEntity
import com.norvexa.flow.data.local.ReserveEntity
import com.norvexa.flow.data.local.TransactionEntity
import com.norvexa.flow.data.local.WalletEntity
import com.norvexa.flow.domain.FinanceData
import org.json.JSONArray
import org.json.JSONObject

object BackupCodec {
    fun encode(data: FinanceData): String = JSONObject().apply {
        put("format", "norvexa-flow"); put("version", 1)
        put("wallets", JSONArray().apply { data.wallets.forEach { put(walletToJson(it)) } })
        put("transactions", JSONArray().apply { data.transactions.forEach { put(transactionToJson(it)) } })
        put("clients", JSONArray().apply { data.clients.forEach { put(clientToJson(it)) } })
        put("receivables", JSONArray().apply { data.receivables.forEach { put(receivableToJson(it)) } })
        put("plannedExpenses", JSONArray().apply { data.plannedExpenses.forEach { put(expenseToJson(it)) } })
        put("reserves", JSONArray().apply { data.reserves.forEach { put(reserveToJson(it)) } })
    }.toString(2)

    fun decode(text: String): FinanceData {
        val root = JSONObject(text); require(root.optString("format") == "norvexa-flow") { "Unsupported backup format" }
        return FinanceData(
            root.getJSONArray("wallets").objects().map(::walletFromJson),
            root.getJSONArray("transactions").objects().map(::transactionFromJson),
            root.getJSONArray("clients").objects().map(::clientFromJson),
            root.getJSONArray("receivables").objects().map(::receivableFromJson),
            root.getJSONArray("plannedExpenses").objects().map(::expenseFromJson),
            root.getJSONArray("reserves").objects().map(::reserveFromJson),
        )
    }
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun walletToJson(v: WalletEntity) = JSONObject().apply { put("id",v.id);put("name",v.name);put("currency",v.currency);put("balanceMinor",v.balanceMinor);put("rateToBaseMicros",v.rateToBaseMicros);put("isActive",v.isActive) }
    private fun walletFromJson(v: JSONObject) = WalletEntity(v.getLong("id"),v.getString("name"),v.getString("currency"),v.getLong("balanceMinor"),v.getLong("rateToBaseMicros"),v.optBoolean("isActive",true))
    private fun transactionToJson(v: TransactionEntity) = JSONObject().apply { put("id",v.id);put("walletId",v.walletId);put("clientId",v.clientId?:JSONObject.NULL);put("type",v.type);put("amountMinor",v.amountMinor);put("currency",v.currency);put("rateToBaseMicros",v.rateToBaseMicros);put("category",v.category);put("note",v.note);put("occurredAtEpochMillis",v.occurredAtEpochMillis) }
    private fun transactionFromJson(v: JSONObject) = TransactionEntity(v.getLong("id"),v.getLong("walletId"),if(v.isNull("clientId")) null else v.getLong("clientId"),v.getString("type"),v.getLong("amountMinor"),v.getString("currency"),v.getLong("rateToBaseMicros"),v.getString("category"),v.optString("note"),v.getLong("occurredAtEpochMillis"))
    private fun clientToJson(v: ClientEntity) = JSONObject().apply { put("id",v.id);put("name",v.name);put("email",v.email);put("defaultCurrency",v.defaultCurrency);put("note",v.note);put("isActive",v.isActive) }
    private fun clientFromJson(v: JSONObject) = ClientEntity(v.getLong("id"),v.getString("name"),v.optString("email"),v.getString("defaultCurrency"),v.optString("note"),v.optBoolean("isActive",true))
    private fun receivableToJson(v: ReceivableEntity) = JSONObject().apply { put("id",v.id);put("clientId",v.clientId);put("title",v.title);put("amountMinor",v.amountMinor);put("receivedMinor",v.receivedMinor);put("currency",v.currency);put("rateToBaseMicros",v.rateToBaseMicros);put("expectedAtEpochDay",v.expectedAtEpochDay);put("probabilityPercent",v.probabilityPercent);put("status",v.status);put("note",v.note) }
    private fun receivableFromJson(v: JSONObject) = ReceivableEntity(v.getLong("id"),v.getLong("clientId"),v.getString("title"),v.getLong("amountMinor"),v.optLong("receivedMinor"),v.getString("currency"),v.getLong("rateToBaseMicros"),v.getLong("expectedAtEpochDay"),v.optInt("probabilityPercent",100),v.optString("status","EXPECTED"),v.optString("note"))
    private fun expenseToJson(v: PlannedExpenseEntity) = JSONObject().apply { put("id",v.id);put("title",v.title);put("amountMinor",v.amountMinor);put("currency",v.currency);put("rateToBaseMicros",v.rateToBaseMicros);put("dueAtEpochDay",v.dueAtEpochDay);put("category",v.category);put("isMandatory",v.isMandatory);put("isCompleted",v.isCompleted);put("recurrence",v.recurrence);put("note",v.note) }
    private fun expenseFromJson(v: JSONObject) = PlannedExpenseEntity(v.getLong("id"),v.getString("title"),v.getLong("amountMinor"),v.getString("currency"),v.getLong("rateToBaseMicros"),v.getLong("dueAtEpochDay"),v.getString("category"),v.optBoolean("isMandatory",true),v.optBoolean("isCompleted",false),v.optString("recurrence","NONE"),v.optString("note"))
    private fun reserveToJson(v: ReserveEntity) = JSONObject().apply { put("id",v.id);put("name",v.name);put("targetMinor",v.targetMinor);put("currentMinor",v.currentMinor);put("currency",v.currency);put("rateToBaseMicros",v.rateToBaseMicros);put("type",v.type);put("isProtected",v.isProtected) }
    private fun reserveFromJson(v: JSONObject) = ReserveEntity(v.getLong("id"),v.getString("name"),v.getLong("targetMinor"),v.getLong("currentMinor"),v.getString("currency"),v.getLong("rateToBaseMicros"),v.optString("type","CUSTOM"),v.optBoolean("isProtected",true))
}

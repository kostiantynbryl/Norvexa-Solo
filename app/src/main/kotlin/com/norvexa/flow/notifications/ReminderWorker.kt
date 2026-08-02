package com.norvexa.flow.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.norvexa.flow.NorvexaFlowApplication
import com.norvexa.flow.R
import com.norvexa.flow.domain.ReceivableStatus
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val data=(applicationContext as NorvexaFlowApplication).container.repository.snapshot(); val today=LocalDate.now().toEpochDay()
        val overdue=data.receivables.count{it.status!=ReceivableStatus.PAID&&it.status!=ReceivableStatus.CANCELLED&&it.expectedAtEpochDay<today}
        val due=data.receivables.count{it.status!=ReceivableStatus.PAID&&it.status!=ReceivableStatus.CANCELLED&&it.expectedAtEpochDay==today}
        val expenses=data.plannedExpenses.count{!it.isCompleted&&it.dueAtEpochDay<=today+1}
        if(overdue+due+expenses>0) NotificationHelper.showSummary(applicationContext,overdue,due,expenses)
        Result.success()
    }.getOrElse { Result.retry() }
}
object ReminderScheduler {
    fun schedule(context:Context){ val req=PeriodicWorkRequestBuilder<ReminderWorker>(1,TimeUnit.DAYS).setInitialDelay(3,TimeUnit.HOURS).build(); WorkManager.getInstance(context).enqueueUniquePeriodicWork("norvexa-flow-reminders",ExistingPeriodicWorkPolicy.UPDATE,req) }
}
private object NotificationHelper {
    private const val channelId="finance_reminders"
    fun showSummary(context:Context,overdue:Int,due:Int,expenses:Int){
        if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=26) manager.createNotificationChannel(NotificationChannel(channelId,"Финансовые напоминания",NotificationManager.IMPORTANCE_DEFAULT))
        val parts=buildList{if(overdue>0)add("просрочено оплат: $overdue");if(due>0)add("оплат сегодня: $due");if(expenses>0)add("ближайших расходов: $expenses")}
        val notification=NotificationCompat.Builder(context,channelId).setSmallIcon(R.drawable.ic_launcher).setContentTitle("Norvexa Flow").setContentText(parts.joinToString(" · ")).setStyle(NotificationCompat.BigTextStyle().bigText(parts.joinToString("\n"))).setAutoCancel(true).build()
        NotificationManagerCompat.from(context).notify(1001,notification)
    }
}

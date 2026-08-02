package com.norvexa.flow

import android.app.Application
import com.norvexa.flow.data.repository.AppContainer
import com.norvexa.flow.notifications.ReminderScheduler

class NorvexaFlowApplication : Application() {
    lateinit var container: AppContainer
        private set
    override fun onCreate() { super.onCreate(); container = AppContainer(this); ReminderScheduler.schedule(this) }
}

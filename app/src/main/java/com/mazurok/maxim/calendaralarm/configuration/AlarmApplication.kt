/*
 * Copyright (C) 2012 Yuriy Kulikov yuriy.kulikov.87@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mazurok.maxim.calendaralarm.configuration

import android.app.Application
import android.view.ViewConfiguration
import androidx.preference.PreferenceManager
import androidx.work.*
import com.mazurok.maxim.calendaralarm.R
import com.mazurok.maxim.calendaralarm.alert.BackgroundNotifications
import com.mazurok.maxim.calendaralarm.background.AlertServicePusher
import com.mazurok.maxim.calendaralarm.bugreports.BugReporter
import com.mazurok.maxim.calendaralarm.createNotificationChannels
import com.mazurok.maxim.calendaralarm.model.Alarms
import com.mazurok.maxim.calendaralarm.model.AlarmsScheduler
import com.mazurok.maxim.calendaralarm.presenter.ScheduledReceiver
import com.mazurok.maxim.calendaralarm.presenter.ToastPresenter
import com.mazurok.maxim.calendaralarm.workers.SyncEventsWorker
import java.util.concurrent.TimeUnit


class AlarmApplication : Application() {
    override fun onCreate() {
        val constraintsBuilder: Constraints.Builder = Constraints.Builder()
        constraintsBuilder.setRequiredNetworkType(NetworkType.CONNECTED)
        val constraints: Constraints = constraintsBuilder.build()

        val uploadWorkRequest: WorkRequest =
                PeriodicWorkRequestBuilder<SyncEventsWorker>(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
                        .setConstraints(constraints)
                        .build()

        WorkManager
                .getInstance(this)
                .enqueue(uploadWorkRequest)

        runCatching {
            ViewConfiguration::class.java
                    .getDeclaredField("sHasPermanentMenuKey")
                    .apply { isAccessible = true }
                    .setBoolean(ViewConfiguration.get(this), false)
        }

        val koin = startKoin(applicationContext)

        koin.get<BugReporter>().attachToMainThread(this)

        // must be after sContainer
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        // TODO make it lazy
        koin.get<ScheduledReceiver>().start()
        koin.get<ToastPresenter>().start()
        koin.get<AlertServicePusher>()
        koin.get<BackgroundNotifications>()

        createNotificationChannels()

        // must be started the last, because otherwise we may loose intents from it.
        val alarmsLogger = koin.logger("Alarms")
        alarmsLogger.debug { "Starting alarms" }
        koin.get<Alarms>().start()
        // start scheduling alarms after all alarms have been started
        koin.get<AlarmsScheduler>().start()

        with(koin.get<Store>()) {
            // register logging after startup has finished to avoid logging( O(n) instead of O(n log n) )
            alarms()
                    .distinctUntilChanged()
                    .subscribe { alarmValues ->
                        for (alarmValue in alarmValues) {
                            alarmsLogger.debug { "$alarmValue" }
                        }
                    }

            next()
                    .distinctUntilChanged()
                    .subscribe { next -> alarmsLogger.debug { "## Next: $next" } }
        }

        super.onCreate()
    }
}

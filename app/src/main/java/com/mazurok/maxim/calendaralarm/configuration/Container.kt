package com.mazurok.maxim.calendaralarm.configuration

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import android.os.Vibrator
import android.telephony.TelephonyManager
import com.mazurok.maxim.calendaralarm.alert.BackgroundNotifications
import com.mazurok.maxim.calendaralarm.background.AlertServicePusher
import com.mazurok.maxim.calendaralarm.background.KlaxonPlugin
import com.mazurok.maxim.calendaralarm.background.PlayerWrapper
import com.mazurok.maxim.calendaralarm.bugreports.BugReporter
import com.mazurok.maxim.calendaralarm.interfaces.IAlarmsManager
import com.mazurok.maxim.calendaralarm.logger.LogcatLogWriter
import com.mazurok.maxim.calendaralarm.logger.Logger
import com.mazurok.maxim.calendaralarm.logger.LoggerFactory
import com.mazurok.maxim.calendaralarm.logger.StartupLogWriter
import com.mazurok.maxim.calendaralarm.model.AlarmCore
import com.mazurok.maxim.calendaralarm.model.AlarmCoreFactory
import com.mazurok.maxim.calendaralarm.model.AlarmSetter
import com.mazurok.maxim.calendaralarm.model.AlarmStateNotifier
import com.mazurok.maxim.calendaralarm.model.Alarms
import com.mazurok.maxim.calendaralarm.model.AlarmsScheduler
import com.mazurok.maxim.calendaralarm.model.Calendars
import com.mazurok.maxim.calendaralarm.model.ContainerFactory
import com.mazurok.maxim.calendaralarm.model.IAlarmsScheduler
import com.mazurok.maxim.calendaralarm.persistance.DatabaseQuery
import com.mazurok.maxim.calendaralarm.persistance.PersistingContainerFactory
import com.mazurok.maxim.calendaralarm.presenter.AlarmsListActivity
import com.mazurok.maxim.calendaralarm.presenter.DynamicThemeHandler
import com.mazurok.maxim.calendaralarm.presenter.ScheduledReceiver
import com.mazurok.maxim.calendaralarm.presenter.ToastPresenter
import com.mazurok.maxim.calendaralarm.stores.SharedRxDataStoreFactory
import com.mazurok.maxim.calendaralarm.util.Optional
import com.mazurok.maxim.calendaralarm.wakelock.WakeLockManager
import com.mazurok.maxim.calendaralarm.wakelock.Wakelocks
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.koin.core.Koin
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module
import java.util.ArrayList
import java.util.Calendar

fun Scope.logger(tag: String): Logger {
    return get<LoggerFactory>().createLogger(tag)
}

fun Koin.logger(tag: String): Logger {
    return get<LoggerFactory>().createLogger(tag)
}

fun startKoin(context: Context): Koin {
    // The following line triggers the initialization of ACRA

    val module = module {
        single<DynamicThemeHandler> { DynamicThemeHandler(get()) }
        single<StartupLogWriter> { StartupLogWriter.create() }
        single<LoggerFactory> {
            Logger.factory(
                    LogcatLogWriter.create(),
                    get<StartupLogWriter>()
            )
        }
        single<BugReporter> { BugReporter(logger("BugReporter"), context, lazy { get<StartupLogWriter>() }) }
        factory<Context> { context }
        factory(named("dateFormatOverride")) { "none" }
        factory<Single<Boolean>>(named("dateFormat")) {
            Single.fromCallable {
                get<String>(named("dateFormatOverride")).let { if (it == "none") null else it.toBoolean() }
                        ?: android.text.format.DateFormat.is24HourFormat(context)
            }
        }

        single<Prefs> {
            val factory = SharedRxDataStoreFactory.create(get(), logger("preferences"))
            Prefs.create(get(named("dateFormat")), factory)
        }

        single<Store> {
            Store(
                    alarmsSubject = BehaviorSubject.createDefault(ArrayList()),
                    next = BehaviorSubject.createDefault<Optional<Store.Next>>(Optional.absent()),
                    sets = PublishSubject.create(),
                    events = PublishSubject.create())
        }

        factory { get<Context>().getSystemService(Context.ALARM_SERVICE) as AlarmManager }
        single<AlarmSetter> { AlarmSetter.AlarmSetterImpl(logger("AlarmSetter"), get(), get()) }
        factory { Calendars { Calendar.getInstance() } }
        single<AlarmsScheduler> { AlarmsScheduler(get(), logger("AlarmsScheduler"), get(), get(), get()) }
        factory<IAlarmsScheduler> { get<AlarmsScheduler>() }
        single<AlarmCore.IStateNotifier> { AlarmStateNotifier(get()) }
        single<ContainerFactory> { PersistingContainerFactory(get(), get()) }
        factory { get<Context>().contentResolver }
        single<DatabaseQuery> { DatabaseQuery(get(), get()) }
        single<AlarmCoreFactory> { AlarmCoreFactory(logger("AlarmCore"), get(), get(), get(), get(), get()) }
        single<Alarms> { Alarms(get(), get(), get(), get(), logger("Alarms")) }
        factory<IAlarmsManager> { get<Alarms>() }
        single { ScheduledReceiver(get(), get(), get(), get()) }
        single { ToastPresenter(get(), get()) }
        single { AlertServicePusher(get(), get(), get(), logger("AlertServicePusher")) }
        single { BackgroundNotifications(get(), get(), get(), get(), get()) }
        factory<Wakelocks> { get<WakeLockManager>() }
        factory<Scheduler> { AndroidSchedulers.mainThread() }
        single<WakeLockManager> { WakeLockManager(logger("WakeLockManager"), get()) }
        factory { get<Context>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
        factory { get<Context>().getSystemService(Context.POWER_SERVICE) as PowerManager }
        factory { get<Context>().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
        factory { get<Context>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
        factory { get<Context>().getSystemService(Context.AUDIO_SERVICE) as AudioManager }
        factory { get<Context>().resources }

        factory(named("volumePreferenceDemo")) {
            KlaxonPlugin(
                    log = logger("VolumePreference"),
                    playerFactory = { PlayerWrapper(get(), get(), logger("VolumePreference")) },
                    prealarmVolume = get<Prefs>().preAlarmVolume.observe(),
                    fadeInTimeInMillis = Observable.just(100),
                    inCall = Observable.just(false),
                    scheduler = get()
            )
        }
    }

    return startKoin {
        modules(module)
        modules(AlarmsListActivity.uiStoreModule)
    }.koin
}

fun overrideIs24hoursFormatOverride(is24hours: Boolean) {
    loadKoinModules(module = module(override = true) {
        factory(named("dateFormatOverride")) { is24hours.toString() }
    })
}
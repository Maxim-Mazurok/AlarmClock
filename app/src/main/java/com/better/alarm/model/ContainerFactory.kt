package com.mazurok.maxim.calendaralarm.model

import android.database.Cursor
import com.mazurok.maxim.calendaralarm.stores.RxDataStore

/**
 * Created by Yuriy on 24.06.2017.
 */
interface ContainerFactory {
    fun create(): AlarmStore
    fun create(cursor: Cursor): AlarmStore
}

interface AlarmStore : RxDataStore<AlarmValue> {
    fun delete()
}
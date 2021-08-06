package com.mazurok.maxim.calendaralarm

import android.database.Cursor
import com.mazurok.maxim.calendaralarm.model.AlarmStore
import com.mazurok.maxim.calendaralarm.model.Calendars
import com.mazurok.maxim.calendaralarm.model.ContainerFactory
import com.mazurok.maxim.calendaralarm.persistance.PersistingContainerFactory
import com.mazurok.maxim.calendaralarm.stores.InMemoryRxDataStoreFactory.Companion.inMemoryRxDataStore

/**
 * Created by Yuriy on 25.06.2017.
 */
class TestContainerFactory(private val calendars: Calendars) : ContainerFactory {
    private var idCounter: Int = 0
    val createdRecords = mutableListOf<AlarmStore>()

    override fun create(): AlarmStore {
        val inMemory = inMemoryRxDataStore(
                PersistingContainerFactory.create(
                        calendars = calendars,
                        idMapper = { _ -> idCounter++ }
                )
        )
        return object : AlarmStore {
            override var value = inMemory.value
            override fun observe() = inMemory.observe()
            override fun delete() {
                createdRecords.removeIf { it.value.id == value.id }
            }
        }
                .also { createdRecords.add(it) }
    }

    override fun create(cursor: Cursor): AlarmStore {
        throw UnsupportedOperationException()
    }
}

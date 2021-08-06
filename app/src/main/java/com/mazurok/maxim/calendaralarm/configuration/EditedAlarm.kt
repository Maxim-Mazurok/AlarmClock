package com.mazurok.maxim.calendaralarm.configuration

import com.mazurok.maxim.calendaralarm.model.AlarmValue
import com.mazurok.maxim.calendaralarm.presenter.RowHolder
import com.mazurok.maxim.calendaralarm.util.Optional

/**
 * Created by Yuriy on 09.08.2017.
 */
data class EditedAlarm(val isNew: Boolean = false,
                       val id: Int = -1,
                       val value: Optional<AlarmValue> = Optional.absent(),
                       val holder: Optional<RowHolder> = Optional.absent()) {
    fun id() = id
    val isEdited: Boolean = value.isPresent()
}

package com.mazurok.maxim.calendaralarm.wakelock

interface Wakelocks {
    fun acquireServiceLock()

    fun releaseServiceLock()
}
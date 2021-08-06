package com.mazurok.maxim.calendaralarm.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.mazurok.maxim.calendaralarm.configuration.globalInject
import com.mazurok.maxim.calendaralarm.interfaces.IAlarmsManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.*


class SyncEventsWorker(appContext: Context, workerParams: WorkerParameters) :
        Worker(appContext, workerParams) {
    private val client = OkHttpClient()

    @Serializable
    data class Event(val userId: Int, val id: Int, val title: String, val completed: Boolean)

    override fun doWork(): Result {
        try {
            getEvents()
        } catch (e: IOException) {
            Log.d("Error", e.localizedMessage)
            return Result.failure()
        }

        return Result.success()
    }

    private fun getEvents(): Array<Event> {
        val request = Request.Builder()
                .url("https://jsonplaceholder.typicode.com/todos")
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val body = response.body!!.string()

            return Json.decodeFromString(body)
        }
    }
}
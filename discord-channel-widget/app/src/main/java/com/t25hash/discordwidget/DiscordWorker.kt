package com.t25hash.discordwidget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class DiscordWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val token = DiscordStore.botToken(applicationContext)
        val channelId = DiscordStore.channelId(applicationContext)
        val messages = DiscordFetcher.fetchRecentMessages(token, channelId)
        DiscordStore.saveCache(applicationContext, messages)
        DiscordChannelWidget().updateAll(applicationContext)
        Result.success()
    }

    companion object {
        private const val WORK_NAME = "discord_channel_refresh"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = PeriodicWorkRequestBuilder<DiscordWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun runOnce(context: Context): WorkRequest {
            val request = OneTimeWorkRequestBuilder<DiscordWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
            return request
        }
    }
}

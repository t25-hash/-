package com.t25hash.tagranking

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class RankingWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val tags = RankingStore.tags(applicationContext)
        val all = mutableListOf<RankedArticle>()
        for (tag in tags) {
            all += RankingFetcher.fetchQiita(tag)
            all += RankingFetcher.fetchZenn(tag)
        }
        RankingStore.saveCache(applicationContext, all)
        TagRankingWidget().updateAll(applicationContext)
        Result.success()
    }

    companion object {
        private const val WORK_NAME = "tag_ranking_refresh"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<RankingWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun runOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<RankingWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}

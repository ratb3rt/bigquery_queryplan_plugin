package net.ratb3rt.jetbrains.bigquery_analyzer_plugin

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.bigquery.*
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import java.io.File


class BigQueryPlanViz : DumbAwareAction() {

    private val price = 6.0 / (1L shl 40)

    override fun actionPerformed(event: AnActionEvent) {
        try {
            val job_id = lastJobId()
            val query_plan = jobQueryPlan(job_id)
            val size = ""
            Notifications.Bus.notify(
                Notification(
                    Notifications.SYSTEM_MESSAGES_GROUP_ID, "Query plan viz",
                    size, NotificationType.INFORMATION
                )
            )
        } catch (e: Exception) {
            Notifications.Bus.notify(
                Notification(
                    Notifications.SYSTEM_MESSAGES_GROUP_ID, "Query plan failed",
                    e.message!!, NotificationType.ERROR
                )
            )
        }
    }

    private fun lastJobId(): String {
        return "todo"
    }

    private fun jobQueryPlan(query: String): String {
        val keyPath = SettingsState.getInstance().state?.keyPath
        val applicationDefault = SettingsState.getInstance().state?.applicationDefaultAuthentication

        if (applicationDefault == false && (keyPath == null || keyPath == "")) {
            throw Exception(
                "Please set the GCP service key path OR the Application Default Credentials " +
                        " Option in settings:\nFile | Settings | Tools | BigQuery Estimator Settings"
            )
        }

        val bigquery: BigQuery = if (applicationDefault == false) {
            if (keyPath == null || keyPath == "") {
                throw Exception("Set GCP service key path in settings:\nFile | Settings | Tools | BigQuery Estimator Settings")
            }

            val credentialsText = File(keyPath).inputStream()
            val credentials = ServiceAccountCredentials.fromStream(credentialsText)

            BigQueryOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(credentials.projectId)
                .build().service
        } else {
            BigQueryOptions.getDefaultInstance().service
        }

        val queryConfig: QueryJobConfiguration =
            QueryJobConfiguration.newBuilder(query).setDryRun(true).setUseQueryCache(false).build()
        val job: Job = bigquery.create(JobInfo.of(queryConfig))
        val statistics: JobStatistics.QueryStatistics = job.getStatistics()


        return "Something...."
    }
}
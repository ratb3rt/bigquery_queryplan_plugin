package net.ratb3rt.jetbrains.testbq

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.bigquery.*
import java.io.File
import guru.nidi.graphviz.*
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.attribute.*
import guru.nidi.graphviz.model.*
import guru.nidi.graphviz.model.Factory.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML

class TestQueryPlan {
    private fun getBigQueryClient(keyPath: String, applicationDefault: Boolean): BigQuery {
        if (!applicationDefault && keyPath == "") {
            throw Exception(
                "Please set the GCP service key path OR the Application Default Credentials " +
                        " Option in settings:\nFile | Settings | Tools | BigQuery Estimator Settings"
            )
        }
        return if (!applicationDefault) {
            if (keyPath == "") {
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
    }

    private fun getQueryPlanDetails(
        client: BigQuery,
        jobId: String,
        location: String = "US"
    ): Map<String, List<QueryStage>> {
        // TODO: pass in location properly
        // TODO: exception handling
        val parentJob = client.getJob(jobId)
        val queryPlan: Map<String, List<QueryStage>> =
            if ((parentJob.getStatistics<JobStatistics>().numChildJobs ?: 0) > 0) {
                // TODO: handle multi step queries
                mapOf(jobId to parentJob.getStatistics<JobStatistics.QueryStatistics>().queryPlan)
            } else {
                mapOf(jobId to parentJob.getStatistics<JobStatistics.QueryStatistics>().queryPlan)
            }
        return queryPlan


    }

    fun nameToColour(name: String): String? {
        val map: Map<String, String> = mapOf(
            "Input" to "#f50057",
            "Repartition" to "#4caf50",
            "Join" to "#ff6f00",
            "Sort" to "#990000",
            "Aggregate" to "#0033cc",
            "default" to "#2196f3"
        )
        val cleaned = if (name.contains(":")) {
            name.replace("+", "").split(":")[1]
        } else {
            name
        }
        return map[cleaned] ?: map["default"]
    }

    fun jobQueryPlan(jobId: String, project: String, keyPath: String, useADC: Boolean): Map<String, List<QueryStage>> {
        val client = getBigQueryClient(keyPath, useADC)
        val queryPlanFull = getQueryPlanDetails(client, jobId, "US")
        return queryPlanFull
    }

    private fun nodeName(stage: QueryStage, jobId: String): String {
        return stage.generatedId.toString() + "_" + jobId
    }

    private fun renderStage(stage: QueryStage, jobId: String): MutableNode {
        val content: StringBuilder = StringBuilder()
        val top =
            "<font color=\"white\"><font point-size=\"36\">" + stage.name + "</font><br/>" + stage.completedParallelInputs.toString() + "/" + stage.parallelInputs.toString() + " workers, " + stage.recordsRead.toString() + " records read</font>"
        content.append("<TABLE>\t\t<TR><TD COLSPAN=\"2\" COLOR=\"#FFFFFF\" BGCOLOR=\"" + nameToColour(stage.name) + "\">" + top + "</TD></TR>\n")
        for (step in stage.steps) {
            if (step.substeps.size > 0) {
                content.append(
                    "<tr><td ROWSPAN=\"", step.substeps.size.toString() +
                            "\">" + "step.kind" +  // @TODO : find the kind
                            "</td>" + createHTML().td { +step.substeps[0].replace("|", "\\|") }.toString() +
                            "</tr>\n"
                )
                if (step.substeps.size > 1) {
                    for (i in 1 until step.substeps.size) {
                        content.append(createHTML().tr { td() { +step.substeps[i].toString() } })
                    }
                }
            } else {
                content.append("<tr><td>{stepo.kind}</td><td>&nbsp;</td></tr>\n")
            }
        }
        content.append("</TABLE>")
        return mutNode(nodeName(stage, jobId)).add(Shape.PLAIN_TEXT).add(Label.html(content.toString()))
    }

    fun stagesToDot(stages: Map<String, List<QueryStage>>) {
        val myGraph = graph(directed = true)
        val shuffleOut: MutableMap<Long, Long> = mutableMapOf()
        val shuffleSpilled: MutableMap<Long, Long> = mutableMapOf()
        val myNodes: MutableMap<String, MutableNode> = mutableMapOf()
        for ((jobId, jobStages) in stages) {
            shuffleOut.clear()
            shuffleSpilled.clear()
            for (jobStage in jobStages) {
                shuffleOut[jobStage.generatedId] = jobStage.shuffleOutputBytes
                shuffleSpilled[jobStage.generatedId] = jobStage.shuffleOutputBytesSpilled
                myNodes[nodeName(jobStage, jobId)] = renderStage(jobStage, jobId)
                myGraph.add(renderStage(jobStage, jobId))
            }
            for (jobStage in jobStages) {
                for (inputStage in (jobStage.inputStages ?: emptyList())) {
                    myNodes[nodeName(jobStages[inputStage.toInt()], jobId)]!!
                        .addLink(myNodes[nodeName(jobStage, jobId)])
                        .add(Label.of(((shuffleOut[inputStage] ?: 0) / 1024 / 1024).toString() + " MB"))
// need to get label working
                    myGraph.add(myNodes[nodeName(jobStages[inputStage.toInt()], jobId)])

                    // self._dot.edge(self._node_id(job_stages[input_stage], job_id), self._node_id(stage, job_id),
                    //         label=f"{shuffle_out[job_stages[input_stage].entry_id]/1024/1024:.0f} MB")
                }
            }
        }
        myGraph.toGraphviz().render(Format.PNG).toFile(File("example/ex1.png"))
    }

    fun graphTest() {
        val myGraph = graph(directed = true)
        val tnode = mutNode("shitty").add(Color.RED)
        myGraph.add(tnode)
        //myGraph.toGraphviz().render(Format.DOT).toFile(File("example/ex1.txt"))
        //myGraph.toGraphviz().render(Format.PNG).toFile(File("example/ex1.png"))
    }
}

fun main(args: Array<String>) {

    val qp = TestQueryPlan()
    val keyPath = "/Users/ratb3rt/.secrets/amazing_pipe_bq_access.json"
    val stages = qp.jobQueryPlan("bquxjob_7f666a7c_17d3a5e7e59", "amazing-pipe-124612", keyPath, false)
    qp.stagesToDot(stages)
}

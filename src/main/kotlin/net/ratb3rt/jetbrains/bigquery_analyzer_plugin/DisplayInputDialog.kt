package net.ratb3rt.jetbrains.bigquery_analyzer_plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class DisplayInputDialog : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        if (e.project == null) return

        val wrapper = MyCustomDialog(e.project!!)

        if (wrapper.showAndGet()) {
            println("jobId retrieved")
        }
    }

}
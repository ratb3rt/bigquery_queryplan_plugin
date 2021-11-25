package net.ratb3rt.jetbrains.bigquery_analyzer_plugin
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import javax.swing.*
import javax.swing.JPanel

import com.intellij.ui.components.JBLabel

import com.intellij.util.ui.FormBuilder

import com.intellij.ui.components.JBTextField

class Settings: Configurable {

    private var form: JPanel? = null
    private val keyPath = JBTextField()
    private val applicationDefaultAuthentication = JBCheckBox("Application Default Settings", false)

    override fun createComponent(): JComponent {
        val settings = SettingsState.getInstance()
        keyPath.text = settings.state?.keyPath
        applicationDefaultAuthentication.setSelected(settings.state?.applicationDefaultAuthentication == true)

        form = FormBuilder.createFormBuilder()
            .addLabeledComponent(
                JBLabel("Use application default settings:"),
                applicationDefaultAuthentication, false
            )
            .addLabeledComponent(JBLabel("GCP key path:"), keyPath, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return form!!
    }

    override fun isModified(): Boolean {
        val settings = SettingsState.getInstance()
        return settings.state?.keyPath != this.keyPath.text || settings.state?.applicationDefaultAuthentication != this.applicationDefaultAuthentication.isSelected()
    }

    override fun apply() {
        val settings = SettingsState.getInstance()
        settings.state?.keyPath = keyPath.text
        settings.state?.applicationDefaultAuthentication = applicationDefaultAuthentication.isSelected()

    }

    override fun getDisplayName(): String {
        return "BigQuery Queryplan Viz Setttings"
    }
}
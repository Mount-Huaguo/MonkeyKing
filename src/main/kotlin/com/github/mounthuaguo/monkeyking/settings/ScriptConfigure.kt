package com.github.mounthuaguo.monkeyking.settings

import com.github.mounthuaguo.monkeyking.MonkeyBundle
import com.github.mounthuaguo.monkeyking.services.ApplicationService
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JTabbedPane

class ScriptConfigure(myProject: Project) : Configurable, Configurable.NoScroll {

    private val mainPanel = BorderLayoutPanel()
    private val tabbedPane = JBTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
    private val scriptConfigureComponent = ScriptConfigureComponent(myProject)
    private val browserPanel = MKConfigureBrowserComponent(myProject) { model, source ->
        tabbedPane.selectedIndex = 0
        scriptConfigureComponent.addScript(model, source)
    }

    init {
        tabbedPane.addTab("Scripts", scriptConfigureComponent)
        tabbedPane.addTab("Browser", browserPanel)
        tabbedPane.addChangeListener {
            println("tabbedPane.addChangeListener: $it, ${tabbedPane.selectedIndex}, ${tabbedPane.selectedComponent}")
            if (tabbedPane.selectedIndex == 1) {
                browserPanel.setupUI()
            }
        }
        mainPanel.add(tabbedPane, BorderLayout.CENTER)
    }


    override fun createComponent(): JComponent {
        return mainPanel
    }

    override fun isModified(): Boolean {
        return scriptConfigureComponent.isModified()
    }

    override fun apply() {
        scriptConfigureComponent.saveScripts()
        ApplicationService.getInstance().reload(ConfigureStateService.getInstance().getScripts())
    }

    override fun getDisplayName(): String {
        return MonkeyBundle.message("configureName")
    }


}
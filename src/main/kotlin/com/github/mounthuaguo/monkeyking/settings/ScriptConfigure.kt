package com.github.mounthuaguo.monkeyking.settings

import com.github.mounthuaguo.monkeyking.MKBundle
import com.github.mounthuaguo.monkeyking.services.ApplicationService
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class ScriptConfigure(val project: Project) : Configurable, Configurable.NoScroll {

    private val component = MKConfigureComponent(project)

    override fun createComponent(): JComponent {
        return component
    }

    override fun isModified(): Boolean {
//        return !component.isEqual()
        return true
    }

    override fun apply() {
//        component.saveScripts()
        // reload listener and actions
        ApplicationService.getInstance().reload(ConfigureStateService.getInstance().getScripts())
    }

    override fun getDisplayName(): String {
        return MKBundle.message("configureName")
    }

}
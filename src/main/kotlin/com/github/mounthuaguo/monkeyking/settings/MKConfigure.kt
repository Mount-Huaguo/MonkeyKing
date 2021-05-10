package com.github.mounthuaguo.monkeyking.settings

import com.github.mounthuaguo.monkeyking.MKBundle
import com.github.mounthuaguo.monkeyking.services.ApplicationService
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class MKConfigure : Configurable, Configurable.NoScroll {

    private val component = MKConfigureComponent()

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
        ApplicationService.getInstance().reload(MKStateService.getInstance().getScripts())
    }

    override fun getDisplayName(): String {
        return MKBundle.message("configureName")
    }

}
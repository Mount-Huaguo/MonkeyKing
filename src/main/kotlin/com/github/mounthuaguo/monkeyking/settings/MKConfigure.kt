package com.github.mounthuaguo.monkeyking.settings

import com.github.mounthuaguo.monkeyking.MKBundle
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent


class MKConfigure : Configurable {

    private val component = MKConfigureComponent()

    override fun createComponent(): JComponent? {
        return component.getPanel()
    }

    override fun isModified(): Boolean {
        return !component.isEqual()
    }

    override fun apply() {
        component.saveScripts()
//        MKState.getInstance().state
        // todo process state
    }

    override fun getDisplayName(): String {
        return MKBundle.message("configName")
    }

}
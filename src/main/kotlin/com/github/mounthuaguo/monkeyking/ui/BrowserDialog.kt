package com.github.mounthuaguo.monkeyking.ui

import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

class BrowserDialog() : DialogWrapper(false) {

    init {
        init()

        title = "Browser Scripts"
    }

    override fun createCenterPanel(): JComponent? {
        TODO("Not yet implemented")
    }

}

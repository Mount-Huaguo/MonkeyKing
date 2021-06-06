package com.github.mounthuaguo.monkeyking.jslib

import com.github.mounthuaguo.monkeyking.ui.MyToolWindowManager
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project

class Log(
    private val scriptName: String,
    val project: Project?,
) {

    private fun log(msg: String, typ: ConsoleViewContentType) {
        project ?: return
        MyToolWindowManager.getInstance().print(project, scriptName, msg, typ)
    }

    fun info(message: String) {
        log(message, ConsoleViewContentType.NORMAL_OUTPUT)
    }

    fun error(message: String) {
        log(message, ConsoleViewContentType.ERROR_OUTPUT)
    }

    fun warn(message: String) {
        log(message, ConsoleViewContentType.LOG_WARNING_OUTPUT)
    }
}
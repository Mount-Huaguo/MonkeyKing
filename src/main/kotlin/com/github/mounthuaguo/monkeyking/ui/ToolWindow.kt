package com.github.mounthuaguo.monkeyking.ui

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.services.ServiceViewToolWindowFactory
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager


class ToolWindowUtil(private val project: Project, private val tab: String) {
    fun log(str: String) {
        val toolWindow: ToolWindow =
            ToolWindowManager.getInstance(project).getToolWindow("Monkey King") ?: return
        var content = toolWindow.contentManager.findContent(tab)
        if (content != null) {
            println("content: $content")
            toolWindow.contentManager.removeContent(content, true)
        }
        val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
        content = toolWindow.contentManager.factory.createContent(consoleView.component, tab, false)
        content.isCloseable = true
        toolWindow.contentManager.addContent(content)
        toolWindow.contentManager.setSelectedContent(content)
        consoleView.print(str, ConsoleViewContentType.NORMAL_OUTPUT)
        toolWindow.show()
    }
}

class ToolWindowFactory : ServiceViewToolWindowFactory() {

    override fun shouldBeAvailable(project: Project): Boolean {
        return true
    }

}
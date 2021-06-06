package com.github.mounthuaguo.monkeyking.ui

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.services.ServiceViewToolWindowFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content


class MyToolWindowManager() {

    inner class Item(val console: ConsoleView, val content: Content)

    private val contentViews = mutableMapOf<String, Item>()

    companion object {
        @JvmStatic
        private var instance: MyToolWindowManager? = null
            get() {
                if (field == null) {
                    field = MyToolWindowManager()
                }
                return field
            }

        @JvmName("MyToolWindowManager_getInstance")
        @JvmStatic
        @Synchronized
        fun getInstance(): MyToolWindowManager {
            return requireNotNull(instance)
        }
    }

    fun print(
        project: Project?,
        tabName: String,
        msg: String,
        type: ConsoleViewContentType = ConsoleViewContentType.NORMAL_OUTPUT
    ) {
        project ?: return
        val toolWindow: ToolWindow =
            ToolWindowManager.getInstance(project).getToolWindow("Monkey King") ?: return
        synchronized(this) {
            try {
                val item = contentViews[tabName]!!
                toolWindow.contentManager.setSelectedContent(item.content)
                item.console.print(msg, type)
                toolWindow.show()
            } catch (e: Exception) {
                val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
                val content = toolWindow.contentManager.factory.createContent(consoleView.component, tabName, false)
                content.isCloseable = true
                toolWindow.contentManager.addContent(content)
                toolWindow.contentManager.setSelectedContent(content)
                consoleView.print(msg, type)
                toolWindow.show()
                contentViews[tabName] = Item(consoleView, content)
            }
        }
    }
}

class ToolWindowFactory : ServiceViewToolWindowFactory() {

    override fun shouldBeAvailable(project: Project): Boolean {
        return true
    }

}
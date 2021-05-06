package com.github.mounthuaguo.monkeyking.ui

import com.intellij.execution.services.ServiceViewToolWindowFactory
import com.intellij.openapi.project.Project


class ToolWindowFactory : ServiceViewToolWindowFactory() {

    override fun shouldBeAvailable(project: Project): Boolean {
        return true
    }

}
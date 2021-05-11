package com.github.mounthuaguo.monkeyking

import com.github.mounthuaguo.monkeyking.services.ApplicationService
import com.github.mounthuaguo.monkeyking.services.ProjectService
import com.github.mounthuaguo.monkeyking.settings.MKStateService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity


class ShortcutStartupActivity : StartupActivity {

    @Volatile
    private var registered = false

    override fun runActivity(project: Project) {
        if (!this.registered) {
            registerActions();
            this.registered = true;
        }
    }

    private fun registerActions() {
        val applicationService = ApplicationService.getInstance()
        applicationService.storeDefaultActions()
        applicationService.reload(MKStateService.getInstance().getScripts())
    }

}
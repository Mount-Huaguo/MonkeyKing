package com.github.mounthuaguo.monkeyking

import com.github.mounthuaguo.monkeyking.services.ApplicationService
import com.github.mounthuaguo.monkeyking.settings.ConfigureStateService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity


class ShortcutStartupActivity : ProjectActivity {

  @Volatile
  private var registered = false

  private fun registerActions() {
    val applicationService = ApplicationService.getInstance()
    applicationService.storeDefaultActions()
    applicationService.reload(ConfigureStateService.getInstance().getScripts())
  }

  override suspend fun execute(project: Project) {
    if (!this.registered) {
      registerActions()
      this.registered = true
    }
  }
}

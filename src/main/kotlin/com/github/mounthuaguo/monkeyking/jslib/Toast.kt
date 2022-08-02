package com.github.mounthuaguo.monkeyking.jslib

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

class Toast(val project: Project?) {

  private fun toast(message: String, typ: NotificationType) {
    NotificationGroupManager.getInstance()
      .getNotificationGroup("MonkeyKing Notification Group")
      .createNotification(message, typ)
      .notify(project)
  }

  fun info(message: String) {
    this.toast(message, NotificationType.INFORMATION)
  }

  fun error(message: String) {
    this.toast(message, NotificationType.ERROR)
  }

  fun warn(message: String) {
    this.toast(message, NotificationType.WARNING)
  }
}

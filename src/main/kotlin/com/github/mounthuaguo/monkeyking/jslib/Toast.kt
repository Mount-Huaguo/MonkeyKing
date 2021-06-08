package com.github.mounthuaguo.monkeyking.jslib

import com.intellij.codeInspection.ex.GlobalInspectionContextImpl
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project

class Toast(val project: Project?) {

    private fun toast(message: String, typ: NotificationType) {
        val success: Notification =
            GlobalInspectionContextImpl.NOTIFICATION_GROUP.createNotification(
                message,
                typ
            )
        Notifications.Bus.notify(success, project)
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

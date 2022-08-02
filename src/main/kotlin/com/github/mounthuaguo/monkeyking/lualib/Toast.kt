package com.github.mounthuaguo.monkeyking.lualib

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

class Toast(val project: Project?) : TwoArgFunction() {
  override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
    val toast = LuaTable(0, 3)
    toast["info"] = createNotify(NotificationType.INFORMATION)
    toast["error"] = createNotify(NotificationType.ERROR)
    toast["warn"] = createNotify(NotificationType.WARNING)
    arg2["toast"] = toast
    arg2["package"]["loaded"]["toast"] = toast
    return toast
  }

  private fun createNotify(notificationType: NotificationType): OneArgFunction {
    return object : OneArgFunction() {
      override fun call(arg: LuaValue): LuaValue {
        project ?: return valueOf(false)
        val message = arg.checkstring().toString()
        NotificationGroupManager.getInstance()
          .getNotificationGroup("MonkeyKing Notification Group")
          .createNotification(message, notificationType)
          .notify(project)
        return valueOf(true)
      }
    }
  }
}

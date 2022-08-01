package com.github.mounthuaguo.monkeyking.lualib

import com.github.mounthuaguo.monkeyking.ui.MyToolWindowManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform

class IdeaPlatform(
  private val scriptName: String = "Monkey King",
  private val project: Project?,
  private val actionEvent: AnActionEvent?
) {
  fun globals(): Globals {
    val global = JsePlatform.standardGlobals()
    global.load(Event(project, actionEvent))
    global.load(Dialog())
    global.load(Toast(project))
    global.load(Log(scriptName, project))
    global.load(Clipboard())
    global["print"] = Print()

    project?.let {
      global["Project"] = CoerceJavaToLua.coerce(project)
    }
    actionEvent?.let {
      global["Event"] = CoerceJavaToLua.coerce(actionEvent)
    }

    return global
  }

  inner class Print : OneArgFunction() {
    override fun call(arg: LuaValue): LuaValue {
      MyToolWindowManager.getInstance().print(project, scriptName, arg.toString() + "\n")
      return valueOf(true)
    }
  }
}

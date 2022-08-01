package com.github.mounthuaguo.monkeyking.jslib

import com.github.mounthuaguo.monkeyking.ui.MyToolWindowManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory
import java.util.function.Function
import javax.script.ScriptEngine

class Engine(
  private val scriptName: String = "Monkey King",
  private val menu: String,
  private val event: AnActionEvent,
) {

  fun getEngine(): ScriptEngine {
    val engine = NashornScriptEngineFactory().scriptEngine
    engine.put("menu", menu)
    engine.put("project", event.project)
    engine.put("editor", event.getRequiredData(CommonDataKeys.EDITOR))
    engine.put("toast", Toast(event.project))
    engine.put("log", Log(scriptName, event.project))
    engine.put("event", Event(event))
    engine.put("dialog", Dialog())
    engine.put("clipboard", Clipboard())
    engine.put("print", PrintFunc())
    engine.put("popup", Popup(event))
    return engine
  }

  private inner class PrintFunc : Function<Any, Unit> {
    override fun apply(msg: Any) {
      MyToolWindowManager.getInstance().print(event.project, scriptName, "$msg\n")
    }
  }
}

package com.github.mounthuaguo.monkeyking.services

import com.github.mounthuaguo.monkeyking.MonkeyBundle
import com.github.mounthuaguo.monkeyking.jslib.Engine
import com.github.mounthuaguo.monkeyking.lualib.IdeaPlatform
import com.github.mounthuaguo.monkeyking.settings.ScriptCacheService
import com.github.mounthuaguo.monkeyking.settings.ScriptLanguage
import com.github.mounthuaguo.monkeyking.settings.ScriptModel
import com.github.mounthuaguo.monkeyking.ui.MyToolWindowManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.WindowManager
import com.intellij.util.messages.MessageBusConnection
import org.luaj.vm2.LuaTable
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform
import java.awt.Window

class ApplicationService : Disposable {

  private val actionGroupId = MonkeyBundle.message("actionGroupId")
  private val defaultActions = mutableListOf<String>()
  private var conn: MessageBusConnection? = null

  companion object {
    fun getInstance(): ApplicationService {
      return ServiceManager.getService(ApplicationService::class.java)
    }
  }

  fun storeDefaultActions() {
    if (defaultActions.size > 0) {
      return
    }
    val actionManager = ActionManager.getInstance()
    val group = actionManager.getAction(actionGroupId) as DefaultActionGroup
    val actions = group.childActionsOrStubs

    for (action in actions) {
      if (action is Separator) {
        continue
      }
      if (action.templatePresentation.text != "") {
        defaultActions.add(action.templatePresentation.text)
      }
    }
  }

  fun reload(scripts: List<ScriptModel>) {
    reloadListener(scripts)
    reloadActions(scripts)
  }

  // reload listener
  // todo not implement
  private fun reloadListener(scripts: List<ScriptModel>) {
    conn?.let {
      conn!!.disconnect()
    }
    val createListeners = mutableListOf<ScriptModel>()
    val updateListeners = mutableListOf<ScriptModel>()
    val removeListeners = mutableListOf<ScriptModel>()
    for (script in scripts) {
      if (script.type != "listener") {
        continue
      }
    }
    if (createListeners.size == 0 && updateListeners.size == 0 && removeListeners.size == 0) {
      return
    }
    val bus = ApplicationManager.getApplication().messageBus
    conn = bus.connect()
    conn!!.subscribe(
      VirtualFileManager.VFS_CHANGES,
      object : BulkFileListener {
        override fun before(events: List<VFileEvent?>) {
          val projects = ProjectManager.getInstance().openProjects
          var activityProject: Project? = null
          for (project in projects) {
            val window: Window? = WindowManager.getInstance().suggestParentWindow(project)
            if (window != null && window.isActive) {
              activityProject = project
              break
            }
          }
          activityProject ?: return

          for (event in events) {
            if (event is VFileCreateEvent) {
              for (script in createListeners) run {
                val jse = JsePlatform.standardGlobals()
                jse["event"] = CoerceJavaToLua.coerce(event)
                jse["project"] = CoerceJavaToLua.coerce(activityProject)
                val chuck = jse.load(script.raw)
                chuck.call()
              }
            }
            // todo update and delete
          }
        }
      }
    )
  }

  // reload actions
  private fun reloadActions(scripts: List<ScriptModel>) {
    val actionManager = ActionManager.getInstance()
    val group = actionManager.getAction(actionGroupId) as DefaultActionGroup

    removeAllScriptActions(actionManager, group)
    registerAllActions(actionManager, group, scripts)
  }

  private fun removeAllScriptActions(actionManager: ActionManager, group: DefaultActionGroup) {
    val actions = group.childActionsOrStubs
    for (action in actions) {
      if (action is Separator) {
        continue
      }
      if (defaultActions.contains(action.templatePresentation.text)) {
        continue
      }
      if (action is DefaultActionGroup) {
        removeAllScriptActions(actionManager, action)
        group.remove(action, actionManager)
        continue
      }
      val id = actionManager.getId(action)
      group.remove(action, actionManager)
      actionManager.unregisterAction(id)
    }
  }

  private fun registerAllActions(
    actionManager: ActionManager,
    group: DefaultActionGroup,
    scripts: List<ScriptModel>
  ) {
    scripts.withIndex()
      .reversed()
      .forEach {
        val script = it.value
        if (script.type != "action") {
          return@forEach
        }
        if (!script.enabled) {
          return@forEach
        }
        if (script.actions.size > 1) {
          val subGroup = DefaultActionGroup.createPopupGroup {
            script.name
          }
          for (menu in script.actions) {
            val id = script.genMenuId(menu)
            val action = ScriptAction(script, menu)
            action.templatePresentation.text = menu
            action.templatePresentation.description = menu
            actionManager.registerAction(
              id,
              action,
              PluginId.getId(MonkeyBundle.getMessage("pluginId"))
            )
            subGroup.add(action, Constraints.LAST)
          }
          group.add(subGroup, Constraints.FIRST)
        } else if (script.actions.size == 1) {
          val menu = script.actions[0]
          val id = script.genMenuId(menu)
          val action = ScriptAction(script, menu)
          action.templatePresentation.text = menu
          action.templatePresentation.description = menu
          actionManager.registerAction(
            id,
            action,
            PluginId.getId(MonkeyBundle.getMessage("pluginId"))
          )
          group.add(action, Constraints.FIRST)
        }
      }
  }

  override fun dispose() {
    conn?.let {
      conn!!.disconnect()
    }
  }
}

class RepeatLastActionHandler {

  var menu: String? = null
  var script: ScriptModel? = null

  fun updateAction(menu: String, script: ScriptModel) {
    synchronized(this) {
      this.menu = menu
      this.script = script
    }
  }

  companion object {
    private var instance: RepeatLastActionHandler? = null
      get() {
        if (field == null) {
          field = RepeatLastActionHandler()
        }
        return field
      }

    @JvmName("RepeatLastAction_getInstance")
    @JvmStatic
    @Synchronized
    fun getInstance(): RepeatLastActionHandler {
      return requireNotNull(instance)
    }
  }
}

class RepeatLastAction : AnAction() {

  override fun update(e: AnActionEvent) {
    super.update(e)
    val instance = RepeatLastActionHandler.getInstance()
    e.presentation.isEnabled = instance.menu != null && instance.script != null
    e.presentation.isVisible = instance.menu != null && instance.script != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val instance = RepeatLastActionHandler.getInstance()
    if (instance.script == null || instance.menu == null) {
      return
    }
    ScriptAction(instance.script!!, instance.menu!!).actionPerformed(e)
  }
}

class ScriptAction(private val script: ScriptModel, private val menu: String) : AnAction() {

  override fun actionPerformed(actionEvent: AnActionEvent) {
    try {
      when (script.language) {
        ScriptLanguage.Lua.value -> {
          LuaScriptAction(script, menu, actionEvent).run()
        }

        ScriptLanguage.Js.value -> {
          JsScriptAction(script, menu, actionEvent).run()
        }

        else -> {
          throw Exception("Invalid script language ${script.language}")
        }
      }
      RepeatLastActionHandler.getInstance().updateAction(menu, script)
    } catch (e: Exception) {
      e.printStackTrace()
      MyToolWindowManager.getInstance().print(actionEvent.project, script.name, e.toString())
    }
  }
}

class LuaScriptAction(
  private val script: ScriptModel,
  private val menu: String,
  private val actionEvent: AnActionEvent
) {

  private fun showError(msg: String) {
    actionEvent.project ?: return
    MyToolWindowManager.getInstance().print(actionEvent.project, script.name, "$msg\n")
  }

  fun run() {
    try {
      val env = IdeaPlatform(
        scriptName = script.name,
        project = actionEvent.project,
        actionEvent = actionEvent
      ).globals()
      env["menu"] = menu
      if (script.requires.isNotEmpty()) {
        val app = ApplicationManager.getApplication()
        app.executeOnPooledThread {
          val cache = ScriptCacheService.getInstance()
          val requireTable = LuaTable()
          script.requires.forEachIndexed { index, it ->
            val source = cache.loadRepo(it)
            if (source == "") {
              app.invokeLater(
                {
                  showError("Error load require: $it")
                },
                ModalityState.any()
              )
              return@executeOnPooledThread
            }
            try {
              val chuck = env.load(source)
              requireTable["${(index + 'a'.toByte().toInt()).toChar()}"] = chuck.call()
            } catch (e: Exception) {
              e.printStackTrace()
              app.invokeLater(
                {
                  showError("Error exec require: $it")
                },
                ModalityState.any()
              )
            }
          }
          env["require"] = requireTable
          app.invokeLater(
            {
              try {
                val chuck = env.load(script.raw)
                chuck.call()
              } catch (e: Exception) {
                e.printStackTrace()
                showError("Error exec script: ${script.name}")
              }
            },
            ModalityState.any()
          )
        }
      } else {
        env["require"] = LuaTable()
        val chuck = env.load(script.raw)
        chuck.call()
      }
    } catch (e: Exception) {
      e.printStackTrace()
      showError(e.toString())
    }
  }
}

class JsScriptAction(
  private val script: ScriptModel,
  private val menu: String,
  private val actionEvent: AnActionEvent
) {
  private fun showError(msg: String) {
    MyToolWindowManager.getInstance().print(actionEvent.project, script.name, "$msg\n")
  }

  fun run() {
    try {
      Engine(script.name, menu, actionEvent).getEngine().eval(script.raw)
    } catch (e: Exception) {
      showError(e.toString())
    }
  }
}

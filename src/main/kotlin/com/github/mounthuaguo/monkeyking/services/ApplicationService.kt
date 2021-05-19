package com.github.mounthuaguo.monkeyking.services

import com.github.mounthuaguo.monkeyking.MonkeyBundle
import com.github.mounthuaguo.monkeyking.lualib.IDEA
import com.github.mounthuaguo.monkeyking.settings.ScriptLanguage
import com.github.mounthuaguo.monkeyking.settings.ScriptModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
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

    init {
        println(MonkeyBundle.message("applicationService"))
    }

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
            // todo
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
        removeAllScriptActions()
        registerAllActions(scripts)
    }

    private fun removeAllScriptActions() {
        val actionManager = ActionManager.getInstance()
        val group = actionManager.getAction(actionGroupId) as DefaultActionGroup
        val actions = group.childActionsOrStubs
        for (action in actions) {
            if (action is Separator) {
                continue
            }
            if (defaultActions.contains(action.templatePresentation.text)) {
                continue
            }
            group.remove(action)
        }
    }

    private fun registerAllActions(scripts: List<ScriptModel>) {
        val actionManager = ActionManager.getInstance()
        val group = actionManager.getAction(MonkeyBundle.message("actionGroupId")) as DefaultActionGroup

        for (script in scripts) {
            if (script.type != "action") {
                continue
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


class ScriptAction(private val script: ScriptModel, private val menu: String) : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        try {
            when (script.language) {
                ScriptLanguage.Lua.value -> {
                    LuaScriptAction(script, menu, e).run()
                }
                ScriptLanguage.Js.value -> {
                    JsScriptAction(script, menu, e).run()
                }
                else -> {
                    throw Exception("Invalid script language ${script.language}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // todo notice
        }
    }
}


class LuaScriptAction(private val script: ScriptModel, private val menu: String, private val e: AnActionEvent) {
    fun run() {
        try {
            val env = JsePlatform.standardGlobals()
            env.load(IDEA(scriptName = script.name, project = e.project, actionEvent = e))
            env["menu"] = menu
            val requireTable = LuaTable()
            for (require in script.requires) {
                val chuck = env.load(require.data)
                requireTable[1] = chuck.call()
            }
            env["require"] = requireTable
            val chuck = env.load(script.raw)
            chuck.call()
        } catch (e: Exception) {
            e.printStackTrace()

        }
    }
}


class JsScriptAction(script: ScriptModel, menu: String, e: AnActionEvent) {

    // todo
    fun run() {

    }
}

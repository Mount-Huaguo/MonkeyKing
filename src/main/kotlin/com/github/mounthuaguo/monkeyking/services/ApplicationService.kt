package com.github.mounthuaguo.monkeyking.services

import com.github.mounthuaguo.monkeyking.MKBundle
import com.github.mounthuaguo.monkeyking.settings.MKStateService
import com.github.mounthuaguo.monkeyking.settings.ScriptData
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.PluginId
import org.luaj.vm2.LuaTable
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform

class ApplicationService {

    init {
        println(MKBundle.message("applicationService"))
    }

    companion object {
        fun getInstance(): ApplicationService {
            return ServiceManager.getService(ApplicationService::class.java)
        }
    }

    fun reload(scripts: List<ScriptData>) {
        reloadListener(scripts)
        reloadActions(scripts)
    }

    // reload listener
    private fun reloadListener(scripts: List<ScriptData>) {
        // todo
    }

    // reload actions
    private fun reloadActions(scripts: List<ScriptData>) {
        removeAllScriptActions()
        registerAllActions(scripts)
    }

    private fun removeAllScriptActions() {
        val actionManager = ActionManager.getInstance()
        val group = actionManager.getAction(MKBundle.message("actionGroupId")) as DefaultActionGroup
        group.removeAll()
    }

    private fun registerAllActions(scripts: List<ScriptData>) {
        val actionManager = ActionManager.getInstance()
        val group = actionManager.getAction(MKBundle.message("actionGroupId")) as DefaultActionGroup

        for (script in scripts) {
            if (script.action != "menu") {
                continue
            }
            for (menu in script.menus) {
                val id = script.genMenuId(menu)
                val action = ScriptAction(script, menu)
                action.templatePresentation.text = menu
                action.templatePresentation.description = menu
                actionManager.registerAction(
                    id,
                    action,
                    PluginId.getId(MKBundle.getMessage("pluginId"))
                )
                group.add(action, Constraints.FIRST)
            }
        }
    }
}


class ScriptAction(private val script: ScriptData, private val menu: String) : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        try {
            if (script.language == "lua") {
                LuaScriptAction(script, menu, e).run()
            } else if (script.language == "js") {
                JsScriptAction(script, menu, e).run()
            } else {
                throw Exception("Invalid script language ${script.language}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // todo notice
        }
    }
}


class LuaScriptAction(val script: ScriptData, val menu: String, val e: AnActionEvent) {

    fun run() {
        val env = JsePlatform.standardGlobals()
        env["event"] = CoerceJavaToLua.coerce(e)
        env["menu"] = menu
        val requireTable = LuaTable()
        for (require in script.requires) {
            val chuck = env.load(require.data)
            requireTable[1] = chuck.call()
        }
        env["require"] = requireTable
        val chuck = env.load(script.raw)
        chuck.call()
    }
}


class JsScriptAction(script: ScriptData, menu: String, e: AnActionEvent) {

    // todo
    fun run() {

    }
}

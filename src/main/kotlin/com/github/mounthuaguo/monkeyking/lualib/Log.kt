package com.github.mounthuaguo.monkeyking.lualib

import com.github.mounthuaguo.monkeyking.ui.ToolWindowUtil
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

class Log(
    val scriptName: String,
    val project: Project?,
) : TwoArgFunction() {

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val log = LuaTable(0, 10)
        log["info"] = LogInfo(ConsoleViewContentType.NORMAL_OUTPUT)
        log["error"] = LogInfo(ConsoleViewContentType.ERROR_OUTPUT)
        log["warn"] = LogInfo(ConsoleViewContentType.LOG_WARNING_OUTPUT)
        arg2["log"] = log
        arg2["package"]["loaded"]["log"] = log
        return log
    }

    inner class LogInfo(private val contentType: ConsoleViewContentType) : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return try {
                ToolWindowUtil(project!!, scriptName, contentType).log(arg.checkstring().toString())
                valueOf(true)
            } catch (e: Exception) {
                valueOf(false)
            }
        }
    }


}
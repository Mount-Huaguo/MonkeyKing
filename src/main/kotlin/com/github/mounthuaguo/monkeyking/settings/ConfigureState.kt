package com.github.mounthuaguo.monkeyking.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import org.luaj.vm2.Globals
import org.luaj.vm2.lib.jse.JsePlatform
import java.net.URL
import java.util.*

data class ScriptMenu(val id: String, val name: String)
data class ScriptRequire(val uri: String, val data: String)

data class ScriptModel(val language: String = "lua", var raw: String = "") {

    var version: String = ""
    var namespace: String = ""
    var name: String = ""
    var description: String = ""
    var action: String = ""
    var topic: String = ""
    var requires: List<ScriptRequire> = listOf()
    var menus: List<String> = listOf()
    var enabled: Boolean = true

    companion object {
        @JvmStatic
        private val luaEnv = JsePlatform.standardGlobals()
    }

    init {
        parse()
    }

    // unnamed
    constructor(language: String = "lua") : this(language, "") {
        name = "unnamed"
    }

    fun validate(): String {
        if (language != "lua" && language != "js") {
            return "Language only support lua and js."
        }
        if (version == "") {
            return "Version can not be empty"
        }
        if (namespace == "") {
            return "Namespace can not be empty"
        }
        if (action != "menu" && action != "template" && action != "listener") {
            return "Action can be menu,template and listener"
        }
        if (!Regex("^[-a-zA-z0-9_ .]{1,256}$").matches(name)) {
//            return "Name can't be empty,\nless than 256 characters,\nsupport alpha,digit,dash,whitespace,underscore."
            println("script name: $name")
            return "Name format error"
        }
        val valid = LuaValidator(raw).validate()
        if (valid != "") {
            return "Syntax error"
        }
        return ""
    }

    fun genMenuId(menu: String): String {
        return (name + "_" + menu).replace(" ", "_")
    }

    private fun parse() {
        // scan headers
        // find start and end comment
        val lines = raw.split("\n")
        val headers = mutableListOf<String>()
        var begin = false
        for (line in lines) {
            if (line.contains("@start")) {
                begin = true
                continue
            }
            if (line.contains("@end")) {
                break
            }
            if (!begin) {
                continue
            }
            headers.add(line)
        }

        // parse headers
        version = parseField(headers, "version")
        namespace = parseField(headers, "namespace")
        name = parseField(headers, "name")
        description = parseField(headers, "description")
        action = parseField(headers, "action")
        topic = parseField(headers, "topic")
        menus = parseFields(headers, "menu")
        val requireRaw = parseFields(headers, "require")
        println("parse $name, $description, $action, $version, $topic, $menus, $requireRaw")
        val rs = mutableListOf<ScriptRequire>()
        for (uri in requireRaw) {
            // TODO use async request
            println("requireRaw.uri $uri")
            val raw = URL(uri).readText()
            rs.add(ScriptRequire(uri, raw))
        }
        requires = rs
    }

    fun allMenus(): List<ScriptMenu> {
        val r = mutableListOf<ScriptMenu>()
        for (menu in menus) {
            r.add(ScriptMenu(genMenuId(menu), menu))
        }
        return r
    }

    private fun parseField(headers: List<String>, field: String): String {
        for (row in headers) {
            val value = parseRow(row, field)
            if (value != "") {
                return value
            }
        }
        return ""
    }

    private fun parseFields(headers: List<String>, field: String): List<String> {
        val r = mutableListOf<String>()
        for (row in headers) {
            val value = parseRow(row, field)
            if (value != "") {
                r.add(value)
            }
        }
        return r
    }


    private fun parseRow(row: String, field: String): String {
        val prefix = if (language == "lua") {
            "--"
        } else {
            "//"
        }
        val pattern = "^\\s*$prefix\\s*@$field\\s+(.*)$"
        val match = Regex(pattern).find(row) ?: return ""
        val (fieldValue) = match.destructured
        return fieldValue
    }

}

class ConfigureState {
    var scripts: List<ScriptModel> = listOf()
    var timestamp: Long = 0
}

@State(name = "com.github.mounthuaguo.monkeyking", storages = [Storage("monkeyking.xml")])
class ConfigureStateService : PersistentStateComponent<ConfigureState> {

    var mkState = ConfigureState()

    companion object {
        fun getInstance(): ConfigureStateService {
            return ServiceManager.getService(ConfigureStateService::class.java)
        }
    }

    override fun loadState(state: ConfigureState) {
        //        XmlSerializerUtil.copyBean(state, mkState)

        // for tests
        mkState.scripts = listOf(
            ScriptModel(
                "lua", """      
-- @start 
-- @name Bson Object ID
-- @description Bson object id method suit
-- @action menu
-- @menu Insert Object ID
-- @menu Copy Timestap From Object ID
-- @end 

if menu == 'Insert Object ID'
    return 'xxxx'
end 


if menu == 'Copy Timestap From Object ID'
    return '102837464'
end

        """.trimIndent()
            )
        )
    }

    override fun getState(): ConfigureState {
        println("PersistentStateComponent getState $mkState")
        return mkState
    }

    fun getScripts(): List<ScriptModel> {
        println("PersistentStateComponent getScripts ${mkState.scripts}, timestamp: ${mkState.timestamp}")
//        return mkState.scripts
        return listOf(
            ScriptModel(
                "lua", """      
-- @start 
-- @name Bson Object ID
-- @description Bson object id method suit
-- @action menu
-- @menu Insert Object ID
-- @menu Copy Timestap From Object ID
-- @end 

if menu == 'Insert Object ID'
    return 'xxxx'
end 


if menu == 'Copy Timestap From Object ID'
    return '102837464'
end

        """.trimIndent()
            )
        )
    }

    fun setScripts(scripts: List<ScriptModel>) {
        mkState.scripts = scripts
        mkState.timestamp = Date().time
        println("PersistentStateComponent setScripts ${mkState.scripts}, timestamp: ${mkState.timestamp}")
    }

}


data class LuaValidator(private val raw: String) {
    companion object {
        @JvmStatic
        val env: Globals = JsePlatform.standardGlobals()
    }

    fun validate(): String {
        return try {
            env.load(raw)
            ""
        } catch (e: Exception) {
            e.toString()
        }
    }
}

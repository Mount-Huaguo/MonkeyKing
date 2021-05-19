package com.github.mounthuaguo.monkeyking.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import org.luaj.vm2.Globals
import org.luaj.vm2.lib.jse.JsePlatform
import java.net.URL
import java.util.*

data class ScriptMenu(val id: String, val name: String)
data class ScriptRequire(val uri: String, val data: String)

const val luaScriptModelTemplate = """
-- @start
-- @namespace       namespace.unspecified
-- @version         0.1
-- @name            Name is not specified
-- @type            action or template
-- @action          TODO
-- @end

-- insert code here


"""

const val jsScriptModelTemplate = """
// @start
// @namespace   namespace.unspecified
// @version     0.1
// @name        Name is not specified
// @type        action or template
// @action      TODO 
// @end
    
    
(function(){
    // insert code here
})()

"""

data class ScriptModel(val language: String = "lua", var raw: String = "") {

    var namespace: String = ""
    var version: String = ""
    var name: String = ""
    var description: String? = ""
    var type: String = ""
    var requires: List<ScriptRequire> = listOf()
    var actions: List<String> = listOf()
    var enabled: Boolean = true

    init {
        parse()
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
        if (type != "action" && type != "template") { // todo support listener
            return "Action can be action or template"
        }
        if (!Regex("^[-a-zA-z0-9_ .]{1,256}$").matches(name)) {
            return """
                Name can't be empty,<br/>
                less than 256 characters,<br/>
                support alpha,digit,dash,whitespace,underscore.<br/>
            """.trimIndent()
        }
        val valid = LuaValidator(raw).validate()
        if (valid != "") {
            return valid
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
        type = parseField(headers, "type")
        actions = parseFields(headers, "action")
        val requireRaw = parseFields(headers, "require")
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
        for (menu in actions) {
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

    override fun toString(): String {
        return "ScriptModel[$namespace, $version, $name, $type, $actions]"
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
        XmlSerializerUtil.copyBean(state, mkState)
    }

    override fun getState(): ConfigureState {
        println("PersistentStateComponent getState $mkState")
        return mkState
    }

    fun getScripts(): List<ScriptModel> {
        println("PersistentStateComponent getScripts ${mkState.scripts}, timestamp: ${mkState.timestamp}")
        return mkState.scripts
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

package com.github.mounthuaguo.monkeyking.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import org.luaj.vm2.Globals
import org.luaj.vm2.lib.jse.JsePlatform
import java.util.*
import javax.script.Compilable
import javax.script.ScriptEngineManager


enum class ScriptLanguage(val value: String) {
    Lua("lua"), Js("js")

}

const val luaScriptModelTemplate = """-- @start
-- @namespace       namespace.unspecified
-- @version         0.1
-- @name            Name is not specified
-- @type            action or template
-- @action          TODO
-- @end

-- insert code here


"""

const val jsScriptModelTemplate = """// @start
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
    var requires: List<String> = listOf()
    var actions: List<String> = listOf()
    var enabled: Boolean = true

    init {
        parse()
    }

    fun validate(): String {
        if (language != ScriptLanguage.Lua.value && language != ScriptLanguage.Js.value) {
            return "Language only support ${ScriptLanguage.Lua.value} and ${ScriptLanguage.Js.value}."
        }
        if (!Regex("^[vV]?[0-9.]{1,64}$").matches(version)) {
            return "Version format error"
        }
        if (!Regex("^[a-zA-z0-9_.]{1,256}$").matches(namespace)) {
            return "Namespace format error"
        }
        if (type != "action" && type != "template") { // todo support listener
            return "Type can be action or template"
        }
        if (!Regex("^[-a-zA-z0-9_ .]{1,256}$").matches(name)) {
            return """
                Name can't be empty,<br/>
                less than 256 characters,<br/>
                support alpha,digit,dash,whitespace,underscore.<br/>
            """.trimIndent()
        }
        return if (language == "lua") {
            LuaValidator(raw).validate()
        } else {
            JavascriptValidator(raw).validate()
        }
    }

    fun genMenuId(menu: String): String {
        return "${namespace}.${name}.${menu}".replace(" ", "_")
    }

    private fun parse() {
        val headers = scanHeaders()
        // parse headers
        version = parseField(headers, "version")
        namespace = parseField(headers, "namespace")
        name = parseField(headers, "name")
        description = parseField(headers, "description")
        type = parseField(headers, "type")
        actions = parseFields(headers, "action")
        requires = parseFields(headers, "require")
    }

    private fun scanHeaders(): List<String> {
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
        return headers
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

    fun trimInvalidScripts() {
        val list = mutableListOf<ScriptModel>()
        for (s in scripts) {
            if (s == null) {
                continue
            }
            if (s.validate() != "") {
                continue
            }
            list.add(s)
        }
        scripts = list.toList()
    }
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
        mkState.trimInvalidScripts()
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

interface ScriptValidator {
    fun validate(): String
}


class LuaValidator(private val raw: String) : ScriptValidator {
    companion object {
        @JvmStatic
        val env: Globals = JsePlatform.standardGlobals()
    }

    override fun validate(): String {
        return try {
            env.load(raw)
            ""
        } catch (e: Exception) {
            e.toString()
        }
    }
}


class JavascriptValidator(private val raw: String) : ScriptValidator {
    companion object {
        @JvmStatic
        val compiler: Compilable = ScriptEngineManager().getEngineByName("nashorn") as Compilable
    }

    override fun validate(): String {
        return try {
            compiler.compile(raw)
            ""
        } catch (e: Exception) {
            e.toString()
        }
    }
}

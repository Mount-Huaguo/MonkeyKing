package com.github.mounthuaguo.monkeyking.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.net.URL

data class ScriptMenu(val id: String, val name: String)
data class ScriptRequire(val uri: String, val data: String)

data class MKData(var language: String, var raw: String) {

    var id: String = ""
    var version: String = ""
    var title: String = ""
    var description: String = ""
    var action: String = ""
    var topic: String = ""
    var requires: List<ScriptRequire> = listOf()
    var menus: List<String> = listOf()
    var isValid = false

    init {
        parse()
        isValid = try {
            check()
            true
        } catch (e: Exception) {
            false
        }
    }

    // unnamed
    constructor(language: String) : this(language, "") {
        title = "unnamed"
    }

    private fun check() {
        if (language != "lua" && language != "js") {
            throw Exception("language must be lua or js")
        }
        if (action != "menu" && action != "template" && action != "listener") {
            throw Exception("action must be menu, template or listener")
        }
        if (id == "") {
            throw Exception("script id is required")
        }
        if (title == "") {
            throw Exception("script title is required")
        }
        if (title.length > 256) {
            throw Exception("script title is too long")
        }
    }

    private fun parse() {
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

        id = parseField(headers, "id")
        title = parseField(headers, "title")
        description = parseField(headers, "description")
        action = parseField(headers, "action")
        version = parseField(headers, "version")
        topic = parseField(headers, "topic")
        menus = parseFields(headers, "menu")
        val requireRaw = parseFields(headers, "require")
        val rs = mutableListOf<ScriptRequire>()
        for (uri in requireRaw) {
            // TODO use async request
            val raw = URL(uri).readText()
            rs.add(ScriptRequire(uri, raw))
        }
        requires = rs
    }

    fun allMenus(): List<ScriptMenu> {
        val r = mutableListOf<ScriptMenu>()
        for (menu in menus) {
            r.add(ScriptMenu(id + menu, menu))
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
        var prefix = "//"
        if (language == "lua") {
            prefix = "--"
        }
        val pattern = "^\\s*$prefix\\s*@$field\\s+(.*)$"
        return Regex(pattern).find(row)?.value.toString().trim()
    }

}


@State(name = "com.github.mounthuaguo.monkeyking", storages = [Storage("monkeyking.xml")])
class MKState : PersistentStateComponent<MKState> {

    private var scripts: List<MKData> = listOf()

    fun getScripts(): List<MKData> {
        return scripts
    }


    companion object {
        fun getInstance(): MKState {
            return ServiceManager.getService(MKState::class.java)
        }
    }

    override fun loadState(state: MKState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    override fun getState(): MKState {
        return this
    }

}
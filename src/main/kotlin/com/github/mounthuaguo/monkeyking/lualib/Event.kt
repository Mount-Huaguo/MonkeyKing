package com.github.mounthuaguo.monkeyking.lualib

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

class Event(
    val project: Project?,
    val actionEvent: AnActionEvent?
) : TwoArgFunction() {

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val event = LuaTable(0, 10)
        event["selection"] = Selection()
        event["document"] = DocumentWrap()
        arg2["event"] = event
        arg2["package"]["loaded"]["event"] = event
        return event
    }

    inner class Selection() : ZeroArgFunction() {
        override fun call(): LuaValue {
            actionEvent ?: return LuaTable()

            val editor = actionEvent.getRequiredData(LangDataKeys.EDITOR)
            val selectionModel: SelectionModel = editor.selectionModel
            val t = LuaTable()
            selectionModel.selectedText?.let {
                t["selectedText"] = valueOf(it)
            }
            t["start"] = selectionModel.selectionStart
            t["end"] = selectionModel.selectionEnd
            t["hasSelection"] = valueOf(selectionModel.hasSelection())
            // todo other properties and methods
            return t
        }
    }

    inner class DocumentWrap() : ZeroArgFunction() {
        override fun call(): LuaValue {
            val t = LuaTable()
            actionEvent ?: return t
            val editor = actionEvent.getRequiredData(LangDataKeys.EDITOR)
            val document: Document = editor.document
            t["text"] = document.text
            t["textLength"] = document.textLength
            t["lineCount"] = document.lineCount
            t["replace"] = Replace(document)
            // todo other properties and methods
            return t
        }

        inner class Replace(private val doc: Document) : ThreeArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue {
                return try {
                    val start = arg1.checkint()
                    val end = arg2.checkint()
                    val replace = arg3.checkstring().toString()
                    doc.replaceString(start, end, replace)
                    valueOf(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    valueOf(false)
                }
            }
        }
    }

}
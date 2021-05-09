package com.github.mounthuaguo.monkeyking.lualib

import com.intellij.codeInspection.ex.GlobalInspectionContextImpl
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.awt.datatransfer.StringSelection

class IDEA(
    val scriptName: String = "Monkey King",
    val project: Project?,
    val event: AnActionEvent?
) : TwoArgFunction() {

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val idea = LuaTable(0, 10)
        idea["copy"] = Copy()

        // log
        val log = LuaTable(0, 1)
        log["info"] = LogInfo()
        idea["log"] = log

        // notice
        val notice = LuaTable(0, 3)
        notice["info"] = createNotify(NotificationType.INFORMATION)
        notice["error"] = createNotify(NotificationType.ERROR)
        notice["warn"] = createNotify(NotificationType.WARNING)
        idea["notice"] = notice

        // event
        val event = LuaTable(0, 10)
        event["selection"] = Selection()
        event["document"] = DocumentWrap()
        idea["event"] = event

        arg2["idea"] = idea
        arg2["util"]["loaded"]["idea"] = idea
        return idea
    }

    inner class Copy : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val str = arg.checkstring().toString()
            CopyPasteManager.getInstance().setContents(StringSelection(str))
            return valueOf(true)
        }
    }

    inner class LogInfo : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {

            project ?: return valueOf(false)

            val toolWindow: ToolWindow =
                ToolWindowManager.getInstance(project).getToolWindow("Monkey King") ?: return valueOf(false)

            val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console

            var content = toolWindow.contentManager.findContent(scriptName)
            if (content == null) {
                content = toolWindow.contentManager.factory
                    .createContent(consoleView.component, scriptName, false)
                toolWindow.contentManager.addContent(content)
            }
            toolWindow.contentManager.setSelectedContent(content)
            val str = arg.checkstring().toString()
            consoleView.print(str, ConsoleViewContentType.NORMAL_OUTPUT)
            return valueOf(true)
        }
    }

    private fun createNotify(notificationType: NotificationType): OneArgFunction {
        return object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                project ?: return valueOf(false)
                val message = arg.checkstring().toString()
                val success: Notification =
                    GlobalInspectionContextImpl.NOTIFICATION_GROUP.createNotification(
                        message,
                        notificationType
                    )
                Notifications.Bus.notify(success, project)
                return valueOf(true)
            }
        }
    }

    inner class Selection() : ZeroArgFunction() {
        override fun call(): LuaValue {
            event ?: return LuaTable()

            val editor = event.getRequiredData(LangDataKeys.EDITOR)
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
            event ?: return t
            val editor = event.getRequiredData(LangDataKeys.EDITOR)
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
                    val repl = arg3.checkstring().toString()
                    doc.replaceString(start, end, repl)
                    valueOf(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    valueOf(false)
                }
            }

        }

    }


}
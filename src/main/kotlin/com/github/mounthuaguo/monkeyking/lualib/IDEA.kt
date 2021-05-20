package com.github.mounthuaguo.monkeyking.lualib

import com.github.mounthuaguo.monkeyking.ui.ToolWindowUtil
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import java.awt.datatransfer.StringSelection

class IDEA(
    val scriptName: String = "Monkey King",
    val project: Project?,
    val actionEvent: AnActionEvent?
) : TwoArgFunction() {

    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val idea = LuaTable(0, 10)

        // idea.copy('text')
        idea["copy"] = Copy()

        // log
        // idea.log.info('the info message')
        idea["log"] = LogInfo()

        // notice
        // idea.notice.info('notice info')
        // idea.notice.error('notice error')
        // idea.notice.warn('notice warn')
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

        project?.let {
            arg2["project"] = CoerceJavaToLua.coerce(project)
        }

        actionEvent?.let {
            arg2["event"] = CoerceJavaToLua.coerce(actionEvent)
        }

        arg2["package"]["loaded"]["idea"] = idea
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
            return try {
                ToolWindowUtil(project!!, scriptName).log(arg.checkstring().toString())
                valueOf(true)
            } catch (e: Exception) {
                valueOf(false)
            }
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
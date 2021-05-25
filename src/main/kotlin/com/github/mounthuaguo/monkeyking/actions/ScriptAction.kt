package com.github.mounthuaguo.monkeyking.actions

import com.github.mounthuaguo.monkeyking.ui.ScriptDialogWrapper
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl.NOTIFICATION_GROUP
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import org.luaj.vm2.lib.jse.JsePlatform
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection


class ScriptActionWrap(
    private val language: String,
    private val project: Project,
    private val editor: Editor,
    private val document: Document,
    private val selectionModel: SelectionModel,
) {

    private var sourceText: String = ""
    private var scriptText: String = ""
    private var targetText: String = ""
    private val dialog: ScriptDialogWrapper

    init {
        sourceText = if (editor.selectionModel.hasSelection()) {
            editor.selectionModel.selectedText.toString()
        } else {
            editor.document.text
        }
        dialog = ScriptDialogWrapper(language, sourceText) { typ, text ->
            handleCallBack(typ, text)
        }
    }

    fun showDialog() {
        println("showDialog")
//        targetText = execScript(sourceText, scriptText)
//        dialog.setTargetDocument(targetText)
        if (dialog.showAndGet()) {
            // todo
        }
        println("showDialog success")
    }

    private fun handleCallBack(typ: String, text: String) {
        when (typ) {
            "source" -> {
                sourceText = text
                targetText = execScript(sourceText, scriptText)
                dialog.setTargetDocument(targetText)
            }
            "script" -> {
                scriptText = text
                targetText = execScript(sourceText, scriptText)
                dialog.setTargetDocument(targetText)
            }
            "copy" -> {
                val selection = StringSelection(targetText)
                val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(selection, selection)

                val message = "Copied!"
                val success: Notification = NOTIFICATION_GROUP.createNotification(message, NotificationType.INFORMATION)
                Notifications.Bus.notify(success, project)

            }
            "replace" -> {
                var start = 0
                var end = document.text.length
                if (selectionModel.hasSelection()) {
                    start = selectionModel.selectionStart
                    end = selectionModel.selectionEnd
                }
                WriteCommandAction.runWriteCommandAction(
                    project
                ) { document.replaceString(start, end, targetText) }
                selectionModel.removeSelection()
            }
        }
    }

    private fun execScript(source: String, script: String): String {
        return if (language == "lua") {
            execLua(source, script)
        } else {
            execJs(source, script)
        }
    }


    private fun execLua(source: String, script: String): String {
        return try {
            val jse = JsePlatform.standardGlobals()
            jse["source"] = source
            val chuck = jse.load(script)
            chuck.call().checkstring().toString()
        } catch (e: Exception) {
            e.toString()
        }
    }

    private fun execJs(source: String, script: String): String {
        return try {
            val jse = JsePlatform.standardGlobals()
            jse["source"] = source
            val chuck = jse.load(script)
            chuck.call().checkstring().toString()
        } catch (e: Exception) {
            e.toString()
        }
    }

}


open class ScriptActionBase(private val language: String) : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {

        val project: Project = e.project ?: return

        val editor = e.getRequiredData(LangDataKeys.EDITOR)
        val document: Document = editor.document
        val selectionModel: SelectionModel = editor.selectionModel

        val wrap = ScriptActionWrap(language, project, editor, document, selectionModel)
        wrap.showDialog()
    }

}


class LuaScriptAction() : ScriptActionBase("lua") {
}

class JSScriptAction() : ScriptActionBase("js") {
}
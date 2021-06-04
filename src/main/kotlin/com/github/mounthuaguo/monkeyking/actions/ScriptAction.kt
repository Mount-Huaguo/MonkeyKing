package com.github.mounthuaguo.monkeyking.actions

import com.github.mounthuaguo.monkeyking.settings.ScriptCacheService
import com.github.mounthuaguo.monkeyking.ui.ScriptDialogWrapper
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl.NOTIFICATION_GROUP
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import org.luaj.vm2.LuaTable
import org.luaj.vm2.lib.jse.JsePlatform
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import javax.script.ScriptEngineManager


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
        dialog = ScriptDialogWrapper(language, sourceText) { typ, text, requires ->
            handleCallBack(typ, text, requires)
        }
    }

    fun showDialog() {
        dialog.showAndGet()
    }

    private fun handleCallBack(typ: String, text: String, requires: List<String>?) {
        when (typ) {
            "source" -> {
                sourceText = text
                execScript(sourceText, scriptText, requires) {
                    targetText = it
                    dialog.setTargetDocument(targetText)
                }
            }
            "script" -> {
                scriptText = text
                execScript(sourceText, scriptText, requires) {
                    targetText = it
                    dialog.setTargetDocument(targetText)
                }
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

    private fun execScript(
        source: String,
        script: String,
        requires: List<String>?,
        callBack: (String) -> Unit
    ) {
        if (language == "lua") {
            execLua(source, script, requires, callBack)
        } else {
            execJs(source, script, callBack)
        }
    }

    private fun execLua(source: String, script: String, requires: List<String>?, callback: (String) -> Unit) {
        try {
            val jse = JsePlatform.standardGlobals()
            jse["source"] = source
            val rt = LuaTable()
            if (requires != null && requires.isNotEmpty()) {
                val app = ApplicationManager.getApplication()
                app.executeOnPooledThread {
                    try {
                        requires.forEachIndexed { index, s ->
                            val txt = ScriptCacheService.getInstance().loadRepo(s)
                            rt["${(index + 'a'.toByte().toInt()).toChar()}"] = jse.load(txt).call()
                        }
                        jse["require"] = rt
                        app.invokeLater({
                            app.runWriteAction {
                                try {
                                    callback(jse.load(script).call().checkstring().toString())
                                } catch (e: Exception) {
                                    callback(e.toString())
                                }
                            }
                        }, ModalityState.any())
                    } catch (e: Exception) {
                        callback(e.toString())
                    }
                }
                return
            }
            jse["require"] = rt
            val chuck = jse.load(script)
            callback(chuck.call().checkstring().toString())
        } catch (e: Exception) {
            callback(e.toString())
        }
    }

    private fun execJs(source: String, script: String, callBack: (String) -> Unit) {
        try {
            val factory = ScriptEngineManager()
            val engine = factory.getEngineByName("nashorn")
            engine.put("source", source)
            val r = engine.eval(script)
            callBack(r.toString())
        } catch (se: Exception) {
            callBack(se.toString())
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
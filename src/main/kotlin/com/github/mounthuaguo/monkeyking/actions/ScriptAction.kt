package com.github.mounthuaguo.monkeyking.actions

import com.github.mounthuaguo.monkeyking.settings.ScriptCacheService
import com.github.mounthuaguo.monkeyking.ui.ScriptDialogWrapper
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl.NOTIFICATION_GROUP
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.Application
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
    private val scriptThreadManager: ScriptThreadManager = ScriptThreadManager()

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
        scriptThreadManager.run(language, source, script, requires, callBack)
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

class LuaScriptAction : ScriptActionBase("lua")

class JSScriptAction : ScriptActionBase("js")

class ScriptThreadManager : Disposable {

    private var scriptThread: ScriptThread? = null

    fun run(
        language: String,
        source: String,
        script: String,
        requires: List<String>?,
        callback: (String) -> Unit
    ) {
        cancel()
        scriptThread = ScriptThread(language, source, script, requires, callback)
        scriptThread!!.start()
    }

    private fun cancel() {
        scriptThread?.let {
            scriptThread!!.cancel()
        }
    }

    override fun dispose() {
        cancel()
    }
}

class ScriptThread(
    private val language: String,
    private val source: String,
    private val script: String,
    private val requires: List<String>?,
    private val callback: (String) -> Unit,
    private val timeout: Long = 5000
) : Thread() {

    private var thread: Thread? = null

    override fun run() {
        thread = Thread {
            ScriptEval(language, source, script, requires, callback).exec()
        }
        thread!!.start()
        sleep(timeout)
        cancel()
    }

    @SuppressWarnings("deprecated")
    fun cancel() {
        try {
            thread?.let {
                if (thread!!.isAlive) {
                    thread!!.stop()
                }
            }
        } catch (e: ThreadDeath) {
        }
    }
}

class ScriptEval(
    private val language: String,
    private val source: String,
    private val script: String,
    private val requires: List<String>?,
    private val callback: (String) -> Unit
) {

    fun exec() {
        when (language) {
            "lua" -> {
                execLua(source, script, requires, callback)
            }
            "js" -> {
                execJs(source, script, callback)
            }
            else -> {
                print("not support language: $language")
            }
        }
    }

    private fun execLua(source: String, script: String, requires: List<String>?, callback: (String) -> Unit) {
        val app = ApplicationManager.getApplication()
        try {
            val jse = JsePlatform.standardGlobals()
            jse["source"] = source
            val rt = LuaTable()
            if (requires != null && requires.isNotEmpty()) {
                try {
                    requires.forEachIndexed { index, s ->
                        val txt = ScriptCacheService.getInstance().loadRepo(s)
                        rt["${(index + 'a'.toByte().toInt()).toChar()}"] = jse.load(txt).call()
                    }
                    jse["require"] = rt
                } catch (e: Exception) {
                    callback(e.toString())
                    return
                }
            }
            jse["require"] = rt

            runWriteAction(app) {
                try {
                    callback(jse.load(script).call().checkstring().toString())
                } catch (e: Exception) {
                    callback(e.toString())
                }
            }
        } catch (e: Exception) {
            runWriteAction(app) {
                callback(e.toString())
            }
        }
    }

    private fun runWriteAction(application: Application?, fn: () -> Unit) {
        var app = application
        if (app == null) {
            app = ApplicationManager.getApplication()
        }
        app!!.invokeLater(
            {
                fn()
            },
            ModalityState.any()
        )
    }

    private fun execJs(source: String, script: String, callBack: (String) -> Unit) {
        val app = ApplicationManager.getApplication()
        try {
            val factory = ScriptEngineManager()
            val engine = factory.getEngineByName("nashorn")
            engine.put("source", source)
            val r = engine.eval(script)
            runWriteAction(app) {
                callBack(r?.toString() ?: "")
            }
        } catch (se: Exception) {
            runWriteAction(app) {
                callBack(se.toString())
            }
        }
    }
}

package com.github.mounthuaguo.monkeyking.ui

import com.github.mounthuaguo.monkeyking.MonkeyBundle
import com.github.mounthuaguo.monkeyking.settings.ConfigureStateService
import com.github.mounthuaguo.monkeyking.settings.ScriptModel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.util.AlarmFactory
import com.intellij.util.ui.components.BorderLayoutPanel
import org.jetbrains.annotations.Nullable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

private const val luaBaseTemplate = """--
-- You know what to do, Enjoy!
-- 

-- insert code here

return source

"""

private const val jsBaseTemplate = """//
// You know what to do, Enjoy!
//  

(function() {
    // insert code here
    return source
})()

"""

class ScriptDialogWrapper(
    private val language: String,
    private var sourceText: String,
    private val callback: (String, String, List<String>?) -> Unit
) :
    DialogWrapper(false) {

    private var sourceEditor: Editor? = null
    private var targetEditor: Editor? = null
    private var scriptEditor: Editor? = null
    private var panel: JPanel = BorderLayoutPanel()
    private var targetDocument: Document? = null
    private var templates: List<ScriptModel>? = null
    private var selectedScript: ScriptModel? = null
    private val alarm = AlarmFactory.getInstance().create()

    init {
        init()

        title = MonkeyBundle.getMessage("scriptDialogTitle")
    }

    private fun getTemplate(): List<ScriptModel> {
        if (templates != null) {
            return templates!!
        }
        val ts = mutableListOf<ScriptModel>()
        val scripts = ConfigureStateService.getInstance().getScripts()
        for (script in scripts) {
            if (script.language != language) {
                continue
            }
            if (script.type != "template") {
                continue
            }
            if (!script.enabled) {
                continue
            }
            ts.add(script)
        }
        templates = ts.toList()
        return templates!!
    }

    private fun sourceTextHasChanged(txt: String) {
        alarm.cancelAllRequests()
        alarm.addRequest(
            {
                callback("source", txt, selectedScript?.requires)
            },
            1000
        )
    }

    private fun scriptTextHasChanged(txt: String) {
        alarm.cancelAllRequests()
        alarm.addRequest(
            {
                callback("script", txt, selectedScript?.requires)
            },
            1000
        )
    }

    fun setTargetDocument(txt: String) {
        targetDocument ?: return
        ApplicationManager.getApplication().invokeLater(
            {
                runWriteAction {
                    targetDocument!!.setText(txt)
                }
            },
            ModalityState.any()
        )
    }

    private fun wrapEditor(editorName: String, editor: Editor): JPanel {
        val wrapPanel = JPanel(BorderLayout())
        val label = JBLabel(editorName)
        wrapPanel.add(label, BorderLayout.NORTH)
        wrapPanel.add(editor.component, BorderLayout.CENTER)
        return wrapPanel
    }

    private fun wrapSourceEditor(editorName: String, editor: Editor): JPanel {
        val wrapPanel = JPanel(BorderLayout())
        val label = JBLabel(editorName)

        val cmb: JComboBox<String> = ComboBox()
        cmb.addItem("Not Selected")
        getTemplate().forEachIndexed { i, t ->
            cmb.addItem("$i. ${t.name}")
        }
        cmb.addActionListener {
            selectTemplate(cmb.selectedItem as String)
        }
        val northPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        northPanel.add(label)
        northPanel.add(cmb)

        wrapPanel.add(northPanel, BorderLayout.NORTH)
        wrapPanel.add(editor.component, BorderLayout.CENTER)
        return wrapPanel
    }

    private fun selectTemplate(name: String) {
        getTemplate().forEachIndexed { i, t ->
            if ("$i. ${t.name}" == name) {
                selectedScript = t
                ApplicationManager.getApplication().runWriteAction {
                    scriptEditor?.document?.setText(t.raw)
                }
            }
        }
    }

    private fun createEditor(filename: String, document: Document, showLineMakerArea: Boolean): Editor {
        val fileExtension: String = FileUtilRt.getExtension(filename)
        val editor: Editor = EditorFactory.getInstance().createEditor(
            document,
            null,
            FileTypeManager.getInstance().getFileTypeByExtension(fileExtension),
            false
        )
        if (!showLineMakerArea) {
            editor.settings.isLineNumbersShown = false
            editor.settings.isLineMarkerAreaShown = false
            editor.settings.isFoldingOutlineShown = false
            editor.settings.isAutoCodeFoldingEnabled = false
        }
        return editor
    }

    @Nullable
    override fun createCenterPanel(): JComponent {
        val template = if (language == "js") {
            jsBaseTemplate
        } else {
            luaBaseTemplate
        }
        val middleSplitter = JBSplitter(false, 0.65f, 0.2f, 0.8f)
        val leftSplitter = JBSplitter(true, 0.5f, 0.2f, 0.8f)

        val factory = EditorFactory.getInstance()
        val sourceDocument: Document = factory.createDocument(sourceText)
        sourceDocument.setReadOnly(false)
        sourceDocument.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                sourceTextHasChanged(event.document.text)
            }
        })

        sourceEditor = createEditor("plain.txt", sourceDocument, false)

        targetDocument = factory.createDocument("")
        targetDocument!!.setReadOnly(false)
        targetEditor = createEditor("plain.txt", targetDocument!!, false)

        val scriptDocument: Document = factory.createDocument(template)
        scriptDocument.setReadOnly(false)
        scriptDocument.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                scriptTextHasChanged(event.document.text)
            }
        })
        scriptEditor = createEditor("script.$language", scriptDocument, true)

        leftSplitter.firstComponent = wrapEditor("Input", sourceEditor!!)
        leftSplitter.secondComponent = wrapEditor("Output", targetEditor!!)
        middleSplitter.firstComponent = wrapSourceEditor("Script: ", scriptEditor!!)
        middleSplitter.secondComponent = leftSplitter

        panel.add(middleSplitter, BorderLayout.CENTER)
        panel.preferredSize = Dimension(800, 600)
        return panel
    }

    // custom actions
    override fun createActions(): Array<Action> {

        val cancel = object : DialogWrapperAction("Cancel") {
            override fun doAction(e: ActionEvent?) {
                close(1)
            }
        }

        val copy = object : DialogWrapperAction("Copy") {
            override fun doAction(e: ActionEvent?) {
                targetEditor ?: return
                callback("copy", targetEditor!!.document.text, selectedScript?.requires)
                close(1)
            }
        }

        val replace = object : DialogWrapperAction("Replace") {
            override fun doAction(e: ActionEvent?) {
                targetEditor ?: return
                callback("replace", targetEditor!!.document.text, selectedScript?.requires)
                close(1)
            }
        }

        return arrayOf(cancel, copy, replace)
    }
}

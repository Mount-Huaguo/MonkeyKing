package com.github.mounthuaguo.monkeyking.ui

import com.github.mounthuaguo.monkeyking.MonkeyBundle
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.components.BorderLayoutPanel
import org.jetbrains.annotations.Nullable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel


private const val luaBaseTemplate = """--
-- todo
-- 

-- insert code here

return source

"""

private const val jsBaseTemplate = """//
//
//  

(function() {
    // insert code here
    return source
})()

"""

class ScriptDialogWrapper(
    private val language: String,
    private var sourceText: String,
    private val callback: (String, String) -> Unit
) :
    DialogWrapper(false) {

    private var sourceEditor: Editor? = null
    private var targetEditor: Editor? = null
    private var scriptEditor: Editor? = null
    private var panel: JPanel = BorderLayoutPanel()

    private var targetDocument: Document? = null

    init {
        init()

        println("init ScriptDialog")

        title = MonkeyBundle.getMessage("scriptDialogTitle")
    }

    private fun sourceTextHasChanged(txt: String) {
        callback("source", txt)
    }

    private fun scriptTextHasChanged(txt: String) {
        callback("script", txt)
    }

    fun setTargetDocument(txt: String) {
        targetDocument ?: return
        targetDocument!!.setText(txt)
    }

    private fun wrapEditor(editorName: String, editor: Editor): JPanel {
        val wrapPanel = JPanel(BorderLayout())
        val label = JBLabel(editorName)
        wrapPanel.add(label, BorderLayout.NORTH)
        wrapPanel.add(editor.component, BorderLayout.CENTER)
        return wrapPanel
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
        println("createCenterPanel")

        val template = if (language == "js") {
            jsBaseTemplate
        } else {
            luaBaseTemplate
        }
        val middleSplitter = JBSplitter(false, 0.5f, 0.2f, 0.8f)
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

        leftSplitter.firstComponent = wrapEditor("Source", sourceEditor!!)
        leftSplitter.secondComponent = wrapEditor("Target", targetEditor!!)
        middleSplitter.firstComponent = wrapEditor("Script", scriptEditor!!)
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

        val copy = object : DialogWrapperAction("Copy to Clipboard") {
            override fun doAction(e: ActionEvent?) {
                targetEditor ?: return
                callback("copy", targetEditor!!.document.text)
                close(1)
            }
        }

        val replace = object : DialogWrapperAction("Replace") {
            override fun doAction(e: ActionEvent?) {
                targetEditor ?: return
                callback("replace", targetEditor!!.document.text)
                close(1)
            }
        }

        return arrayOf(cancel, copy, replace)
    }

}
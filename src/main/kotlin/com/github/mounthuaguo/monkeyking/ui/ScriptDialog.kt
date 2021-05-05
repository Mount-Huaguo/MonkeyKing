package com.github.mounthuaguo.monkeyking.ui

import com.github.mounthuaguo.monkeyking.MKBundle
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.ui.components.JBLabel
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel

class ScriptDialog(
    language: String,
    sourceText: String,
    private var template: String,
    private val callback: (String, String) -> Unit
) :
    DialogWrapper(false) {

    private val sourceEditor: Editor
    private val targetEditor: Editor
    private val scriptEditor: Editor
    private val panel: JPanel

    private val targetDocument: Document

    init {
        title = MKBundle.getMessage("scriptDialogTitle")

        if (template == null) {
            template = MKBundle.getMessage("luaTemplate")
            if (language == "js") {
                template = MKBundle.getMessage("jsTemplate")
            }
        }

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
        targetDocument.setReadOnly(true)
        targetEditor = createEditor("plain.txt", targetDocument, false)

        val scriptDocument: Document = factory.createDocument(template)
        scriptDocument.setReadOnly(false)
        scriptDocument.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                scriptTextHasChanged(event.document.text)
            }
        })
        scriptEditor = createEditor("script.$language", scriptDocument, true)

        panel = JPanel(GridLayout(1, 2, 10, 10))

        val leftPanel = JPanel(GridLayout(2, 1, 10, 10))
        leftPanel.add(wrapEditor("Source", sourceEditor))
        leftPanel.add(wrapEditor("Target", targetEditor))
        panel.add(leftPanel)
        panel.add(wrapEditor("Script", scriptEditor))
        panel.preferredSize = Dimension(800, 600)

    }

    private fun sourceTextHasChanged(txt: String) {
        callback("source", txt)
    }

    private fun scriptTextHasChanged(txt: String) {
        callback("script", txt)
    }

    fun setTargetDocument(txt: String) {
        targetDocument.setText(txt)
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
        val sourceEditor: Editor = EditorFactory.getInstance().createEditor(
            document,
            null,
            FileTypeManager.getInstance().getFileTypeByExtension(fileExtension),
            false
        )
        if (!showLineMakerArea) {
            sourceEditor.settings.isLineNumbersShown = false
            sourceEditor.settings.isLineMarkerAreaShown = false
            sourceEditor.settings.isFoldingOutlineShown = false
            sourceEditor.settings.isAutoCodeFoldingEnabled = false
        }
        return sourceEditor
    }


    override fun createCenterPanel(): JComponent {
        return panel
    }

    // custom actions
    override fun createLeftSideActions(): Array<Action> {

        val copy = object : DialogWrapperAction("Copy to Clipboard") {
            override fun doAction(e: ActionEvent?) {
                callback("copy", targetEditor.document.text)
            }
        }

        val replace = object : DialogWrapperAction("Replace") {
            override fun doAction(e: ActionEvent?) {
                callback("replace", targetEditor.document.text)
            }
        }

        return arrayOf(copy, replace)
    }

}
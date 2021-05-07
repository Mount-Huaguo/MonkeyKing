package com.github.mounthuaguo.monkeyking.settings

import com.github.mounthuaguo.monkeyking.ui.BrowserDialog
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.EditorTextField
import com.intellij.ui.SingleSelectionModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel


class MKConfigureComponent {

    private val mainPanel: JPanel = JBPanel<JBPanel<*>>();
    private val scriptTable: JBTable = JBTable()
    private val scripts = mutableListOf<MKData>()
    private val state = MKState.getInstance()
    private val editor = EditorPanel() {
        // todo lose focus
    }
    private var selectRow = -1;

    init {

        // table
        scripts.addAll(state.getScripts())

        println("state.getScripts(), ${state.getScripts()}")

        scriptTable.setShowColumns(true)
        scriptTable.model = object : AbstractTableModel() {

            override fun getRowCount(): Int {
                return scripts.size
            }

            override fun getColumnCount(): Int {
                return 3
            }

            override fun getColumnName(index: Int): String {
                when (index) {
                    0 -> return "NAME"
                    1 -> return "LANGUAGE"
                    2 -> return "DESCRIPTION"
                }
                return "exception"
            }

            override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
                when (columnIndex) {
                    0 -> return scripts[rowIndex].title
                    1 -> return scripts[rowIndex].language
                    2 -> return scripts[rowIndex].description
                }
                return "error"
            }

        }
        scriptTable.rowHeight = 24
        scriptTable.dragEnabled = false
        scriptTable.isStriped = true
        scriptTable.selectionModel = SingleSelectionModel()
        scriptTable.setShowGrid(false)
        val selectionModel: ListSelectionModel = scriptTable.selectionModel
        selectionModel.addListSelectionListener {
            println("ListSelectionListener ${scriptTable.selectedRow}")
            processChangedScript()
            reselectTable()
        }

        val toolbarDecorator: ToolbarDecorator = ToolbarDecorator.createDecorator(scriptTable)
        // add
        val action1: AnAction = object : DumbAwareAction(AllIcons.General.Add) {
            override fun actionPerformed(e: AnActionEvent) {
                println("action1 actionPerformed ${e}")

                scripts.add(MKData("lua"))
                scriptTable.removeRowSelectionInterval(selectRow, selectRow)
                refreshTable()
                scriptTable.setRowSelectionInterval(scripts.size, scripts.size)
            }
        }

        // browser
        val action2: AnAction = object : DumbAwareAction(AllIcons.Actions.Show) {
            override fun actionPerformed(e: AnActionEvent) {
                println("e.project, ${e.project}")
                if (BrowserDialog().showAndGet()) {
                    // todo
                }
            }
        }

        // edit
        val action3: AnAction = object : DumbAwareAction(AllIcons.Actions.Edit) {
            override fun actionPerformed(e: AnActionEvent) {
                println(e)
            }
        }

        val action4: AnAction = object : DumbAwareAction(AllIcons.General.Remove) {
            override fun actionPerformed(e: AnActionEvent) {
                println(e)
            }
        }

        val ag = DefaultActionGroup(action1, action2, action4)
        toolbarDecorator.setActionGroup(ag)
        toolbarDecorator.setToolbarPosition(ActionToolbarPosition.RIGHT)


        val tablePanel = toolbarDecorator.createPanel()
        tablePanel.minimumSize = Dimension(100, 500)
        tablePanel.maximumSize = Dimension(1200, 600)

        mainPanel.layout = BorderLayout()
        mainPanel.add(tablePanel, BorderLayout.NORTH)
        mainPanel.add(editor, BorderLayout.CENTER)
    }

    private fun reselectTable() {
        selectRow = scriptTable.selectedRow
        val d = scripts[selectRow]
        editor.setScriptData(d)
    }

    fun getPanel(): JPanel {
        return mainPanel
    }

    private fun processChangedScript() {
        val d = editor.getScriptData() ?: return
        scripts[selectRow] = d
    }

    fun refreshTable() {
        scriptTable.repaint()
    }

    fun saveScripts() {
//        val m = mutableMapOf<String, Unit>()
//        for (s in scripts) {
//            if (!s.isValid()) {
//                return
//            }
//            // id duplicate
//            if (m.containsKey(s.id)) {
//                return
//            }
//            m[s.id] = Unit
//        }
        state.setScript(scripts)
        state.state
    }

    fun isEqual(): Boolean {
        if (state.getScripts().size != scripts.size) {
            return false
        }
        val m = mutableMapOf<String, Unit>()
        val ss = state.getScripts()
        for (s in ss) {
            m[s.raw] = Unit
        }
        for (s in scripts) {
            if (!m.containsKey(s.raw)) {
                return false
            }
        }
        return true
    }


    //
    private class EditorPanel(focusLostFunc: () -> Unit) : JBPanel<JBPanel<*>>(BorderLayout()) {

        var script: MKData? = null

        private val editor = EditorTextField()
        private val cmb = ComboBox<String>()

        init {
            val headerPanel = JBPanel<JBPanel<*>>(BorderLayout())
            val lang = JBLabel("Language")
            headerPanel.add(lang, BorderLayout.WEST)
            cmb.addItem("lua")
            cmb.addItem("js")
            headerPanel.add(cmb, BorderLayout.CENTER)

            cmb.addItemListener { event ->
                script?.language = event.item.toString()
                languageChanged()
                println("item selected ${event}")
            }

            // editor
            editor.setOneLineMode(false)

            editor.document.addDocumentListener(object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    script?.raw = event.document.text
                }
            })

            editor.addFocusListener(object : FocusListener {

                override fun focusGained(e: FocusEvent?) {
                    // do nothing
                }

                override fun focusLost(e: FocusEvent?) {
                    focusLostFunc()
                }

            })

            add(headerPanel, BorderLayout.NORTH)
            add(editor, BorderLayout.CENTER)
        }

        fun setScriptData(s: MKData) {
            script = s
            refresh()
        }

        fun getScriptData(): MKData? {
            return script
        }

        fun refresh() {
            cmb.item = script?.language
            editor.text = script?.raw.toString()
        }

        // todo highlight
        fun languageChanged() {

        }
    }


}
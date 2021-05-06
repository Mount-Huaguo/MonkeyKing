package com.github.mounthuaguo.monkeyking.settings

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.EditorTextField
import com.intellij.ui.SingleSelectionModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel

class MKConfigureComponent {

    private val mainPanel: JPanel = JPanel();
    private val scriptTable: JBTable = JBTable()
    private val scripts: List<MKData> = listOf()
    private var selectRow = -1;

    init {

        // table

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
                    0 -> return "标题"
                    1 -> return "描述"
                    2 -> return "元数据"
                }
                return "错误"
            }

            override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
                when (columnIndex) {
                    0 -> return scripts[rowIndex].title
                    1 -> return scripts[rowIndex].description
                    2 -> return scripts[rowIndex].raw
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
            println("scriptTable.selectedRow ${scriptTable.selectedRow}")
            selectRow = scriptTable.selectedRow
        }

        val toolbarDecorator: ToolbarDecorator = ToolbarDecorator.createDecorator(scriptTable)
        val action1: AnAction = object : DumbAwareAction(AllIcons.General.Add) {
            override fun actionPerformed(e: AnActionEvent) {

            }
        }

        val action2: AnAction = object : DumbAwareAction(AllIcons.Actions.Search) {
            override fun actionPerformed(e: AnActionEvent) {
                println(e)
            }
        }

        val action3: AnAction = object : DumbAwareAction(AllIcons.General.Remove) {
            override fun actionPerformed(e: AnActionEvent) {
                println(e)
            }
        }

        val ag = DefaultActionGroup(action1, action2, action3)
        toolbarDecorator.setActionGroup(ag)
        toolbarDecorator.setToolbarPosition(ActionToolbarPosition.RIGHT)

        // editor
//        val fileExtension: String = FileUtilRt.getExtension("script.lua")
//        val editorFactory: EditorFactory = EditorFactory.getInstance()
//        val document: Document = editorFactory.createDocument("")
//        document.setReadOnly(false)
//        document.addDocumentListener(object : DocumentListener {
//            override fun documentChanged(event: DocumentEvent) {
//
//            }
//        })
//
//        val editor: Editor = editorFactory.createEditor(
//            document,
//            null,
//            FileTypeManager.getInstance().getFileTypeByExtension(fileExtension),
//            false
//        )

        val editor = EditorTextField()
        editor.setOneLineMode(false)

        val tablePanel = toolbarDecorator.createPanel()
        tablePanel.minimumSize = Dimension(100, 500)
        tablePanel.maximumSize = Dimension(1200, 600)

        mainPanel.layout = BorderLayout()
        mainPanel.add(tablePanel, BorderLayout.NORTH)
        mainPanel.add(editor, BorderLayout.CENTER)


//        println("ServiceManager.getService(ProjectService::class.java) ${ServiceManager.getService(ProjectService::class.java)}")

    }


    fun getPanel(): JPanel {
        return mainPanel
    }

}
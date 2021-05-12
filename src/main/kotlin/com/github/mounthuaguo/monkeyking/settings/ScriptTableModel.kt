package com.github.mounthuaguo.monkeyking.settings

import com.intellij.ui.components.CheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Component
import javax.swing.DefaultCellEditor
import javax.swing.JCheckBox
import javax.swing.JTable
import javax.swing.border.EmptyBorder
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellRenderer


class ScriptTableModel(private var scripts: MutableList<ScriptModel>) : AbstractTableModel() {

    override fun getRowCount(): Int {
        return scripts.size
    }

    override fun getColumnCount(): Int {
        return 1
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        return scripts[rowIndex]
    }

    override fun getColumnName(column: Int): String {
        return "SCRIPTS"
    }

    override fun isCellEditable(row: Int, column: Int): Boolean {
        return true
    }


    fun setScripts(s: MutableList<ScriptModel>) {
        scripts = s
        fireTableDataChanged()
    }

    fun getScripts(): List<ScriptModel> {
        return scripts
    }

    fun addScripts(s: ScriptModel) {
        scripts.add(s)
        fireTableDataChanged()
    }

    fun remove(s: ScriptModel) {
        scripts.remove(s)
        fireTableDataChanged()
    }

    fun removeAt(row: Int) {
        scripts.removeAt(row)
        fireTableRowsDeleted(row, row)
    }

    fun updateScriptEnabled(row: Int, enabled: Boolean) {
        scripts[row].enabled = enabled
        fireTableDataChanged()
    }


    fun replace(s: ScriptModel) {
        // todo
    }

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return ScriptTableModelCell::class.java
    }

}

class ScriptTableModelCell(private val checkBoxValueChanged: (index: Int, checked: Boolean) -> Unit) :
    TableCellRenderer,
    DefaultCellEditor(JCheckBox()) {

    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val data = value as ScriptModel
        val nameLabel = JBLabel(data.name)
        val languageLabel = JBLabel("Language: " + data.language)
        val descLabel = JBLabel(data.description)
        val panel = BorderLayoutPanel()
        panel.addToTop(nameLabel)
        panel.addToCenter(languageLabel)
        panel.addToBottom(descLabel)
        val checkbox = CheckBox("", data.enabled)
        panel.addToRight(checkbox)
        panel.border = EmptyBorder(0, 10, 0, 10)
        return panel
    }

    override fun getTableCellEditorComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        row: Int,
        column: Int
    ): Component {
        val data = value as ScriptModel
        val nameLabel = JBLabel(data.name)
        val languageLabel = JBLabel("Language: " + data.language)
        val descLabel = JBLabel(data.description)
        val panel = BorderLayoutPanel()
        panel.addToTop(nameLabel)
        panel.addToCenter(languageLabel)
        panel.addToBottom(descLabel)
        val checkbox = CheckBox("", data.enabled)
        panel.addToRight(checkbox)
        panel.border = EmptyBorder(0, 10, 0, 10)

        checkbox.addActionListener {
            checkBoxValueChanged(row, checkbox.isSelected)
        }
        return panel
    }

}

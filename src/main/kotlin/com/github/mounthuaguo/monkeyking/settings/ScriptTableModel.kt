package com.github.mounthuaguo.monkeyking.settings

import com.intellij.ui.components.CheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.ItemRemovable
import com.intellij.util.ui.StartupUiUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.DefaultCellEditor
import javax.swing.JCheckBox
import javax.swing.JTable
import javax.swing.border.EmptyBorder
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellRenderer


class ScriptTableModel(private var scripts: MutableList<ScriptModel>) : AbstractTableModel(), ItemRemovable {

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

    fun scriptAt(row: Int): ScriptModel {
        return scripts[row]
    }

    fun remove(s: ScriptModel) {
        scripts.remove(s)
        fireTableDataChanged()
    }

    fun removeAt(row: Int) {
        println("removeAt: $row, ${scripts.size}")
        scripts.removeAt(row)
        fireTableRowsDeleted(row, row)
    }

    fun updateScriptEnabled(row: Int, enabled: Boolean) {
        scripts[row].enabled = enabled
        fireTableDataChanged()
    }

    fun updateScript(row: Int, script: ScriptModel) {
        scripts[row] = script
        fireTableRowsUpdated(row, row)
    }

    fun replace(s: ScriptModel) {
        // todo
    }

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return ScriptTableModelCell::class.java
    }

    override fun removeRow(idx: Int) {
        scripts.removeAt(idx);
        fireTableRowsDeleted(idx, idx);
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
        val panel = BorderLayoutPanel()
        val leftPanel = BorderLayoutPanel()
        leftPanel.addToCenter(nameLabel)
        leftPanel.addToBottom(languageLabel)
        val labelFont: Font = StartupUiUtil.getLabelFont()
        languageLabel.font = labelFont.deriveFont(labelFont.size2D - 2.0f)
        languageLabel.foreground = Color.gray
        leftPanel.border = EmptyBorder(5, 0, 5, 0)
        panel.addToCenter(leftPanel)
        val checkbox = CheckBox("", data.enabled)
        panel.addToRight(checkbox)
        panel.border = EmptyBorder(0, 10, 0, 10)
        if (data.validate() != "") {
            nameLabel.foreground = Color.RED
        }
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
        val panel = BorderLayoutPanel()
        val leftPanel = BorderLayoutPanel()
        leftPanel.addToCenter(nameLabel)
        leftPanel.addToBottom(languageLabel)
        leftPanel.border = EmptyBorder(5, 0, 5, 0)
        val labelFont: Font = StartupUiUtil.getLabelFont()
        languageLabel.font = labelFont.deriveFont(labelFont.size2D - 2.0f)
        languageLabel.foreground = Color.gray
        panel.addToCenter(leftPanel)
        val checkbox = CheckBox("", data.enabled)
        panel.addToRight(checkbox)
        panel.border = EmptyBorder(0, 10, 0, 10)
        if (data.validate() != "") {
            nameLabel.foreground = Color.RED
        }

        checkbox.addActionListener {
            checkBoxValueChanged(row, checkbox.isSelected)
        }
        return panel
    }

}

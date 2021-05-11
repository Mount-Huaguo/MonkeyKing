package com.github.mounthuaguo.monkeyking.settings

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Component
import javax.swing.JTable
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

    fun setScripts(s: MutableList<ScriptModel>) {
        scripts = s
    }

    fun getScripts(): List<ScriptModel> {
        return scripts
    }

    fun addScripts(s: ScriptModel) {
        scripts.add(s)
    }

    fun remove(s: ScriptModel) {
        scripts.remove(s)
    }

    fun replace(s: ScriptModel) {
        // todo
    }

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return ScriptTableModelCell::class.java
    }

}

class ScriptTableModelCell : TableCellRenderer {
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
        val leftPanel = BorderLayoutPanel()
        leftPanel.addToTop(nameLabel)
        leftPanel.addToCenter(languageLabel)
        leftPanel.addToBottom(descLabel)
        return leftPanel
    }
}

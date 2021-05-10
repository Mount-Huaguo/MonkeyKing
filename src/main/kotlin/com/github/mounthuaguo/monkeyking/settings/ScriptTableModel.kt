package com.github.mounthuaguo.monkeyking.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.CardLayout
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellRenderer

class ScriptTableModel(private var scripts: MutableList<ScriptModel>) : AbstractTableModel() {

    override fun getRowCount(): Int {
        return scripts.size
    }

    override fun getColumnCount(): Int {
        return 2
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        return scripts[rowIndex]
    }

    override fun getColumnName(column: Int): String? {
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
        return if (columnIndex == 0) {
            ScriptTableModelCell::class.java
        } else {
            ScriptTableModelControlCell::class.java
        }
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
        val component = JPanel(GridBagLayout())
        val nameLabel = JBLabel(data.name)
        val languageLabel = JBLabel("Language: " + data.language)
        val descLabel = JBLabel(data.description)
        val checkbox = JBCheckBox()
        val useButton = JButton("use")
        val leftPanel = BorderLayoutPanel()
        val rightPanel = JPanel(CardLayout())

        leftPanel.addToTop(nameLabel)
        leftPanel.addToCenter(languageLabel)
        leftPanel.addToBottom(descLabel)

        rightPanel.add(checkbox, "checkbox")
        rightPanel.add(useButton, "use")
        val layout = rightPanel.layout as CardLayout
        layout.show(rightPanel, "use")
//        layout.show(rightPanel, "checkbox")

        val constraints = GridBagConstraints()
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.gridx = 0
        constraints.gridy = 0
        constraints.gridheight = 1
        constraints.gridwidth = 6
        constraints.weightx = 1.0
        constraints.weighty = 1.0
        component.add(leftPanel, constraints)

        constraints.gridx = 6
        constraints.gridy = 0
        constraints.gridheight = 1
        constraints.gridwidth = 1
        constraints.weightx = 0.0
        constraints.weighty = 0.0
        component.add(rightPanel, constraints)

        return component
    }

}


class ScriptTableModelControlCell : TableCellRenderer {

    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        return JButton("use")
    }

}
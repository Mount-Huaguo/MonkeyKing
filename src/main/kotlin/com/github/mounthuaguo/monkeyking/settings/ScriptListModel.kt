package com.github.mounthuaguo.monkeyking.settings

import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import javax.swing.*

class ScriptListModel(private var scripts: MutableList<ScriptModel>) :
    AbstractListModel<ScriptModel>() {
    override fun getSize(): Int {
        return scripts.size
    }

    override fun getElementAt(index: Int): ScriptModel? {
        return scripts[index]
    }

    fun addScript(script: ScriptModel) {
        scripts.add(script)
        fireIntervalAdded(scripts, 0, scripts.size)
    }

    fun removeScript(script: ScriptModel) {
        scripts.remove(script)
        fireIntervalAdded(scripts, 0, scripts.size)
    }

    fun removeLastScript() {
        val index = scripts.size - 1
        scripts.removeAt(index)
        fireIntervalRemoved(this, index, index)
    }
}

val _selectColor = Color(245, 245, 245)
val _borderColor = Color(240, 240, 240)

class ScriptListCell<T> : ListCellRenderer<T> {

    override fun getListCellRendererComponent(
        list: JList<out T>?,
        value: T,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val panel = BorderLayoutPanel()
        panel.background = if (isSelected) {
            _selectColor
        } else {
            Color.WHITE
        }
        panel.border = BorderFactory.createMatteBorder(0, 0, 1, 0, _borderColor)
        val obj = value as ScriptModel
        val label = JLabel(obj.name + obj.name + obj.name)
        panel.add(label, BorderLayout.CENTER)
        return panel
    }

}
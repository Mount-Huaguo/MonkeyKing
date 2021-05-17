package com.github.mounthuaguo.monkeyking.settings

import com.intellij.icons.AllIcons
import java.awt.Component
import javax.swing.AbstractListModel
import javax.swing.JCheckBox
import javax.swing.JList
import javax.swing.ListCellRenderer

class ScriptListModel(scripts: List<ScriptModel>) : AbstractListModel<JCheckBox>() {
    private val myScriptModels = scripts.toMutableList()

    override fun getSize(): Int {
        return myScriptModels.size
    }

    override fun getElementAt(index: Int): JCheckBox {
        return JCheckBox(myScriptModels[index].name, AllIcons.FileTypes.JavaScript, true)
    }

}

class ScriptListModelCell : ListCellRenderer<JCheckBox> {

    override fun getListCellRendererComponent(
        list: JList<out JCheckBox>?,
        value: JCheckBox?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
//        value?.let {
//            value.isSelected = index % 2 == 0
//        }
        return value!!
    }

}
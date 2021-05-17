package com.github.mounthuaguo.monkeyking.settings

import java.awt.Component
import java.awt.GridBagLayout
import javax.swing.*

class ScriptListModel(scripts: List<ScriptModel>) : AbstractListModel<JCheckBox>() {
    private val myScriptModels = scripts.toMutableList()

    override fun getSize(): Int {
        return myScriptModels.size
    }

    override fun getElementAt(index: Int): JCheckBox {
        return JCheckBox()
//        return JCheckBox(myScriptModels[index].name, AllIcons.FileTypes.JavaScript, true)
    }

}

//class ScriptListModelCell : ListCellRenderer<JCheckBox> {
//
//    override fun getListCellRendererComponent(
//        list: JList<out JCheckBox>?,
//        value: JCheckBox?,
//        index: Int,
//        isSelected: Boolean,
//        cellHasFocus: Boolean
//    ): Component {
////        val panel = JPanel(GridBagLayout())
////        panel.border = BorderFactory.createEmptyBorder();
////        panel.isOpaque = true
////        panel.add(value!!, BorderLayout.EAST)
////        val label = JBLabel(AllIcons.FileTypes.Css)
////        label.isOpaque = true
////        panel.add(label, BorderLayout.WEST)
////        val l = JBLabel("hello world hello world hello world")
////        l.maximumSize = JBUI.size(60, 20)
////        l.isOpaque = true
////        panel.add(l, BorderLayout.CENTER)
////        value.isOpaque = true
////        return panel
//    }
//
//}
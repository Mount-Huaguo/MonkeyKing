package com.github.mounthuaguo.monkeyking.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*


class InputModel(
    val type: String,
    val field: String,
    var options: Array<String>? = null,
) {
    private var valuedPanel: ValuedPanel? = null

    constructor(field: String) : this("text", field) {}

    fun compontent(): JComponent {
        when (type) {
            "text" -> {
                valuedPanel = TextFieldValuePanel()
            }
            "radio" -> {
                valuedPanel = RadioValuedPanel(options!!)
            }
            "checkbox" -> {
                valuedPanel = CheckBoxValuedPanel(options!!)
            }
            "dropdown" -> {
                valuedPanel = DropdownValuedPanel(options!!)
            }
            else -> {
                return JPanel()
            }
        }
        return valuedPanel!!.component()
    }

    fun value(): Any {
        valuedPanel ?: return ""
        return valuedPanel!!.value()
    }
}

interface ValuedPanel {
    fun value(): Any
    fun component(): JComponent
}

private class TextFieldValuePanel : ValuedPanel {
    private var field = JTextField()

    override fun value(): Any {
        return field.text
    }

    override fun component(): JComponent {
        return field
    }
}

private class DropdownValuedPanel(options: Array<String>) : ValuedPanel {

    private val cmb: JComboBox<String> = ComboBox()

    init {
        for (option in options) {
            cmb.addItem(option)
        }
    }

    override fun value(): Any {
        return cmb.selectedItem!!
    }

    override fun component(): JComponent {
        return cmb
    }

}

private class CheckBoxValuedPanel(val options: Array<String>) : ValuedPanel {

    val checkboxes = mutableListOf<JCheckBox>()

    override fun value(): Any {
        val l = mutableListOf<String>()
        checkboxes.forEachIndexed { index, box ->
            if (box.isSelected) {
                l.add(options[index])
            }
        }
        return l
    }

    override fun component(): JComponent {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        for (option in options) {
            val box = JCheckBox(option)
            checkboxes.add(box)
            panel.add(box)
        }
        return panel
    }

}

private class RadioValuedPanel(val options: Array<String>) : ValuedPanel {

    val group = ButtonGroup()

    override fun value(): Any {
        return group.selection.actionCommand
    }

    override fun component(): JComponent {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        options.forEachIndexed { index, s ->
            val radio = JRadioButton(s, index == 0)
            radio.actionCommand = s
            group.add(radio)
            panel.add(radio)
        }
        return panel
    }
}


class FormDialog(private val inputs: Array<InputModel>) : DialogWrapper(false) {

    init {
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())

        inputs.forEachIndexed { idx, item ->
            val label = JLabel(item.field)
            panel.add(
                label, GridBagConstraints(
                    0, idx, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0
                )
            )
            val valuePanel = item.compontent()
            panel.add(
                valuePanel, GridBagConstraints(
                    1, idx, 2, 1, 1.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.insetsLeft(10), 0, 0
                )
            )
        }
        return panel
    }
}
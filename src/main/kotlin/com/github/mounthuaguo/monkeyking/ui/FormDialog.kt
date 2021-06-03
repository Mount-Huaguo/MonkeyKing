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
    var default: Any? = null,
) {
    private var valuedPanel: ValuedPanel? = null

    fun component(): JComponent {
        when (type) {
            "text" -> {
                valuedPanel = TextFieldValuePanel(default)
            }
            "radio" -> {
                valuedPanel = RadioValuedPanel(options!!, default)
            }
            "checkbox" -> {
                valuedPanel = CheckBoxValuedPanel(options!!, default)
            }
            "dropdown" -> {
                valuedPanel = DropdownValuedPanel(options!!, default)
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

    override fun toString(): String {
        return "InputModel: [$type, $field, $options]"
    }

}

interface ValuedPanel {
    fun value(): Any
    fun component(): JComponent
}

private class TextFieldValuePanel(default: Any?) : ValuedPanel {
    private var field = JTextField()

    init {
        default?.let {
            field.text = default.toString()
        }
    }

    override fun value(): Any {
        return field.text
    }

    override fun component(): JComponent {
        return field
    }
}

private class DropdownValuedPanel(options: Array<String>, default: Any?) : ValuedPanel {

    private val cmb: JComboBox<String> = ComboBox()

    init {
        for (option in options) {
            cmb.addItem(option)
        }
        cmb.selectedItem = default.toString()
    }

    override fun value(): Any {
        return cmb.selectedItem!!
    }

    override fun component(): JComponent {
        return cmb
    }

}

private class CheckBoxValuedPanel(val options: Array<String>, val default: Any?) : ValuedPanel {

    val checkboxes = mutableListOf<JCheckBox>()

    override fun value(): Any {
        val l = mutableListOf<String>()
        checkboxes.forEachIndexed { index, box ->
            if (box.isSelected) {
                l.add(options[index])
            }
        }
        return l.toTypedArray()
    }

    override fun component(): JComponent {
        val d = mutableListOf<String>()
        when (default) {
            is Array<*> -> {
                default.forEach {
                    d.add(it as String)
                }
            }
            else -> {
                default?.let {
                    d.add(default.toString())
                }
            }
        }

        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        for (option in options) {
            val box = JCheckBox(option, d.contains(option))
            checkboxes.add(box)
            panel.add(box)
        }
        return panel
    }

}

private class RadioValuedPanel(val options: Array<String>, val default: Any?) : ValuedPanel {

    val group = ButtonGroup()

    override fun value(): Any {
        return group.selection.actionCommand
    }

    override fun component(): JComponent {
        var idx = 0
        default?.let {
            options.forEachIndexed { i, t ->
                if (t == default.toString()) {
                    idx = i
                }
            }
        }
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        options.forEachIndexed { index, s ->
            val radio = JRadioButton(s, index == idx)
            radio.actionCommand = s
            group.add(radio)
            panel.add(radio)
        }
        return panel
    }
}


class FormDialog(private val inputs: Array<InputModel>) : DialogWrapper(false) {

    private var focusComponent: JComponent? = null

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
            val valuePanel = item.component()
            panel.add(
                valuePanel, GridBagConstraints(
                    1, idx, 2, 1, 1.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.insetsLeft(10), 0, 0
                )
            )
            if (focusComponent == null && (valuePanel is JTextField)) {
                focusComponent = valuePanel
            }
        }
        return panel
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return focusComponent
    }

    fun values(): Map<String, Any> {
        val m = mutableMapOf<String, Any>()
        for (input in inputs) {
            m[input.field] = input.value()
        }
        return m.toMap()
    }

}
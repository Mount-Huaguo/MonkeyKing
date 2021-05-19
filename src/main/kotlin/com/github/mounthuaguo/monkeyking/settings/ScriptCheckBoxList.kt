package com.github.mounthuaguo.monkeyking.settings

import com.github.mounthuaguo.monkeyking.MonkeyIcons
import com.intellij.ui.CheckBoxList
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class ScriptCheckBoxList : CheckBoxList<ScriptModel>() {
    override fun adjustRendering(
        rootComponent: JComponent,
        checkBox: JCheckBox,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ): JComponent {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createEmptyBorder()

        panel.add(
            checkBox,
            GridBagConstraints(
                0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0
            )
        )
        val sm = checkBox.getClientProperty("model") as ScriptModel

        val icon = if (sm.language == "lua") JLabel(MonkeyIcons.LUA) else JLabel(MonkeyIcons.JS)
        icon.isOpaque = true
        icon.preferredSize = Dimension(25, -1)
        icon.horizontalAlignment = SwingConstants.CENTER
        icon.background = getBackground(selected)

        panel.add(
            icon,
            GridBagConstraints(
                1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0
            )
        )

        val label = JLabel(sm.name)
        label.isOpaque = true
        label.preferredSize = Dimension(100, -1)
        label.background = getBackground(selected)
        if (sm.validate() != "") {
            label.foreground = Color.red
        }
        panel.add(
            label,
            GridBagConstraints(
                2, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0
            )
        )

        panel.background = getBackground(false)
        if (!checkBox.isOpaque) {
            checkBox.isOpaque = true
        }
        checkBox.border = null
        checkBox.isSelected = sm.enabled
        return panel
    }
}
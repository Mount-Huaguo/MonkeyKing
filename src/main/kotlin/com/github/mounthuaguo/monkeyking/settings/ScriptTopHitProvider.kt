package com.github.mounthuaguo.monkeyking.settings

import com.intellij.ide.ui.OptionsSearchTopHitProvider
import com.intellij.ide.ui.search.BooleanOptionDescription
import com.intellij.ide.ui.search.OptionDescription
import java.util.*

// todo
class ScriptTopHitProvider : OptionsSearchTopHitProvider.ApplicationLevelProvider {

    override fun getId(): String {
        return "mk"
    }

    override fun getOptions(): MutableCollection<OptionDescription> {
        val options: Collection<BooleanOptionDescription> = ArrayList()

        return Collections.unmodifiableCollection(options);
    }
}
package net.dankito.richtexteditor.android.command

import net.dankito.richtexteditor.Color
import net.dankito.richtexteditor.Icon
import net.dankito.richtexteditor.JavaScriptExecutorBase
import net.dankito.richtexteditor.android.AndroidIcon
import net.dankito.richtexteditor.android.R
import net.dankito.richtexteditor.command.CommandName


class SetTextBackgroundColorCommand(defaultColor: Color = Color.Transparent, icon: Icon = AndroidIcon(R.drawable.ic_format_color_fill_white_48dp), showColorInCommandView: Boolean = true)
    : SetColorCommand(defaultColor, showColorInCommandView, CommandName.BACKCOLOR, icon) {

    override fun applySelectedColor(executor: JavaScriptExecutorBase, color: Color) {
        executor.setTextBackgroundColor(color)
    }

}
package net.dankito.richtexteditor.command

import net.dankito.richtexteditor.Icon
import net.dankito.richtexteditor.JavaScriptExecutorBase


abstract class AlignCenterCommandBase(icon: Icon) : ActiveStateToolbarCommand(CommandName.JUSTIFYCENTER, icon) {

    override fun executeCommand(executor: JavaScriptExecutorBase) {
        executor.setJustifyCenter()
    }

}
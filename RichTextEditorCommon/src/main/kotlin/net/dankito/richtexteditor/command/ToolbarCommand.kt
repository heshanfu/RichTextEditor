package net.dankito.richtexteditor.command

import net.dankito.richtexteditor.CommandView
import net.dankito.richtexteditor.Icon
import net.dankito.richtexteditor.JavaScriptExecutorBase


abstract class ToolbarCommand(val command: CommandName,
                              val icon: Icon,
                              val style: ToolbarCommandStyle = ToolbarCommandStyle(),
                              val commandExecutedListener: (() -> Unit)? = null) {


    var executor: JavaScriptExecutorBase? = null
        set(value) {
            field = value

            value?.addCommandStatesChangedListener { commandStatesUpdated(it) }
        }

    var commandView: CommandView? = null

    var isExecutable: Boolean = true


    fun commandInvoked() {
        executor?.let {
            executeCommand(it)
        }

        commandExecutedListener?.invoke()
    }

    abstract protected fun executeCommand(executor: JavaScriptExecutorBase)


    private fun commandStatesUpdated(commandStates: Map<CommandName, CommandState>) {
        commandStates[command]?.let { commandState ->
            commandView?.let { commandView ->
                showCommandExecutableState(commandView, commandState.executable)

                if(commandState.value != "") {
                    commandValueChanged(commandView, commandState.value)
                }
            }
        }
    }

    private fun showCommandExecutableState(commandView: CommandView, executable: Boolean) {
        this.isExecutable = executable
        commandView.setIsEnabled(executable)

        setIconTintColorToExecutableState(commandView, executable)
    }

    protected open fun setIconTintColorToExecutableState(commandView: CommandView, isExecutable: Boolean) {
        // currently only ActiveStateToolbarCommand needs this feature
    }

    protected open fun commandValueChanged(commandView: CommandView, commandValue: Any) {

    }

}
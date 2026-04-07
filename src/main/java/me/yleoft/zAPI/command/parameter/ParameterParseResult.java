package me.yleoft.zAPI.command.parameter;

/**
 * Represents the result of parsing a command parameter.
 * Contains the remaining arguments to be processed and whether further command dispatching should be stopped.
 */
public class ParameterParseResult {

    final String[] remainingArgs;
    final boolean stopFurtherDispatch;

    /**
     * Constructs a new ParameterParseResult with the specified remaining arguments and dispatching behavior.
     *
     * @param remainingArgs       An array of remaining arguments to be processed by subsequent parameters or subcommands.
     * @param stopFurtherDispatch A boolean indicating whether further command dispatching should be stopped after this parameter is parsed.
     */
    public ParameterParseResult(String[] remainingArgs, boolean stopFurtherDispatch) {
        this.remainingArgs = remainingArgs;
        this.stopFurtherDispatch = stopFurtherDispatch;
    }

    /**
     * Gets the remaining arguments after parsing the current parameter.
     *
     * @return An array of remaining arguments to be processed by subsequent parameters or subcommands.
     */
    public String[] getRemainingArgs() {
        return remainingArgs;
    }

    /**
     * Indicates whether further command dispatching should be stopped after this parameter is parsed.
     * If true, the command execution will not proceed to any subsequent parameters or subcommands.
     *
     * @return true if further dispatching should be stopped, false otherwise
     */
    public boolean shouldStopFurtherDispatch() {
        return stopFurtherDispatch;
    }

}

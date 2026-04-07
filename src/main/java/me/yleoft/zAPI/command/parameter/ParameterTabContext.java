package me.yleoft.zAPI.command.parameter;

/**
 * ParameterTabContext is a class that holds the context for tab completion of command parameters.
 * It contains the parameter for which tab completion is being performed and the arguments that have been entered for that parameter so far.
 */
public class ParameterTabContext {

    final Parameter parameter;
    final String[] parameterArgsSoFar;

    /**
     * Constructs a new ParameterTabContext with the given parameter and arguments.
     *
     * @param parameter the parameter for which tab completion is being performed
     * @param parameterArgsSoFar the arguments that have been entered for the parameter so far
     */
    public ParameterTabContext(Parameter parameter, String[] parameterArgsSoFar) {
        this.parameter = parameter;
        this.parameterArgsSoFar = parameterArgsSoFar;
    }

    /**
     * Get the parameter for which tab completion is being performed.
     *
     * @return the parameter for which tab completion is being performed
     */
    public Parameter getParameter() {
        return parameter;
    }

    /**
     * Get the arguments that have been entered for the parameter so far.
     *
     * @return the arguments that have been entered for the parameter so far
     */
    public String[] getParameterArgsSoFar() {
        return parameterArgsSoFar;
    }

}

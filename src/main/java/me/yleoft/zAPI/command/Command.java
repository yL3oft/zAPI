package me.yleoft.zAPI.command;

public interface Command extends CommandBasis {

    default double cooldownTime() {
        return 0D;
    }

}

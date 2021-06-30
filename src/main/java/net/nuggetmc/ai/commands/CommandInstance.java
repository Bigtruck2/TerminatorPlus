package net.nuggetmc.ai.commands;

public abstract class CommandInstance {

    protected final CommandHandler commandHandler;

    public CommandInstance(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }
}

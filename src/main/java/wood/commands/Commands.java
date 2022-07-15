package wood.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public abstract class Commands<T> {

    protected String name, description;
    protected boolean isEmbed;

    abstract public void runCommand(long userId, SlashCommandInteractionEvent event);
    abstract public void addCommand(JDA jda);
    abstract public String getDescription();

}

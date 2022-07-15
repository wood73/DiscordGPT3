package wood.handler;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import wood.Main;
import wood.commands.Chat;
import wood.commands.Commands;
import wood.commands.Model;
import wood.commands.Prompt;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class CommandHandler extends ListenerAdapter {

    public static final String MODEL_CMD = "model", PROMPT_CMD = "prompt", CHAT_CMD = "chat";

    public static final Map<String, Commands> commandMap = new HashMap<>() {{
            put(MODEL_CMD, new Model());
            put(PROMPT_CMD, new Prompt());
            put(CHAT_CMD, new Chat());
        }};

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        new Thread(() ->
                commandMap.get(event.getName())
                        .runCommand(event.getUser().getIdLong(), event)
        ).start();
    }

    /**
     * Checks which commands are registered on the Discord server,
     * registering any new ones, and unregistering any old ones.
     */
    public static void checkAndSetSlashCommands() {
        JDA jda = Main.jda;
        List<Command> detectedCommands = jda.retrieveCommands().complete();
        List<String> detectedCommandNames = detectedCommands.stream()
                .map(cmd -> cmd.getName())
                .collect(Collectors.toList());

        // Add commands that are not in detectedCommands
        commandMap.keySet().stream()
                .filter(cmdName -> !detectedCommandNames.contains(cmdName))
                .forEach(cmdName -> {
                    commandMap.get(cmdName).addCommand(jda);
                    log.info("Added new slash command: " + cmdName);
                });

        // Remove commands that are no longer in commandMap
        detectedCommands.stream()
                .filter(cmd -> !commandMap.containsKey(cmd.getName()))
                .forEach(cmd -> {
                    jda.deleteCommandById(cmd.getId()).complete();
                    log.info("Removed slash command: " + cmd.getName());
                });
    }
}
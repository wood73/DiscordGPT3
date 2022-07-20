package wood.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import wood.Settings;
import wood.util.GPTRequest;

// TODO make buttons for user to select model
public class Model extends Commands {

    /** The name of the required argument inside the command (must be lowercase, and without whitespace). */
    private final String commandOptionName = "model";
    /** The description of the required argument inside the command. Must not exceed 100 characters. */
    private final String commandOptionDescription = "e.g. davinci, text-davinci-002, curie, text-curie-001, babbage, "
            + "text-babbage-001, ada, text-ada-001";

    public Model() {
        super.name = "model";
        super.description = "The default language model to use for OpenAI API calls";
    }

    @Override
    public void runCommand(long userId, SlashCommandInteractionEvent event) {
        String modelArg = event.getOption(commandOptionName).getAsString();
        boolean validModel = true;
        switch(modelArg.toLowerCase()) {
            case "davinci":
                Settings.model = GPTRequest.davinci;
                break;
            case "curie":
                Settings.model = GPTRequest.curie;
                break;
            case "babbage":
                Settings.model = GPTRequest.babbage;
                break;
            case "ada":
                Settings.model = GPTRequest.ada;
                break;
            case "text-davinci-001":
            case "text-davinci-002":
                Settings.model = GPTRequest.inDavinci;
                break;
            case "text-curie-001":
                Settings.model = GPTRequest.inCurie;
                break;
            case "text-babbage-001":
                Settings.model = GPTRequest.inBabbage;
                break;
            case "text-ada-001":
                Settings.model = GPTRequest.inAda;
                break;
            default:
                validModel = false;
        }

        if(validModel)
            event.reply("Model set to " + Settings.model).queue();
        else
            event.reply("'" + modelArg + "' is an Invalid model.\nValid models are: davinci, curie, babbage, ada"
                    + ", text-davinci-002, text-curie-001, text-babbage-001, text-ada-001").setEphemeral(true).queue();
    }

    @Override
    public void addCommand(JDA jda) {
        Command cmd = jda.upsertCommand(name, description)
                .addOption(OptionType.STRING, commandOptionName, commandOptionDescription, true)
                .complete();
    }

    @Override
    public String getDescription() {
        return description;
    }

}
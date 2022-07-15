package wood.commands;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import wood.discord_threads.PromptThread;
import wood.util.Util;
import wood.util.UtilGPT;

import java.util.HashMap;
import java.util.Map;

public class Chat extends Commands {
    /** Map of each thread created by `/chat` to its prompt related data */
    @Getter
    private static final Map<Long, PromptThread> threadMap = new HashMap<>();

    public static final String MODAL_ID = "chat-modal";
    private static final String MODAL_MODEL_ID = "model", MODAL_NAME_ID = "name",
            MODAL_DESCRIPTION_ID = "description";

    public Chat() {
        super.name = "chat";
        super.description = "Opens a modal to initialize a GPT-3 chatbot";
    }

    /**
     * Runs when '/chat' is used.  Creates a modal for the user to initialize a chatbot.
     * When the user submits the modal, readModal() is called.
     * @param userId The user's ID.
     * @param event The event that triggered the command.
     */
    @Override
    public void runCommand(long userId, SlashCommandInteractionEvent event) {

        event.reply("/" + name + " is currently under development.").setEphemeral(true).queue();
        return;
        /*

        // Verify the command isn't used inside a thread
        if(event.getChannelType().isThread()) {
            event.reply("/" + name + " can't be used inside of a thread.").setEphemeral(true).queue();
            return;
        }

        TextInput model = TextInput.create(MODAL_MODEL_ID, "Language Model", TextInputStyle.SHORT)
                .setValue(Settings.model)
                .build();

        TextInput chatbotName = TextInput.create(MODAL_NAME_ID, "Chatbot Name", TextInputStyle.SHORT)
                .setPlaceholder("AI")
                .build();

        TextInput chatbotDescription = TextInput.create(MODAL_NAME_ID, "Chatbot Description", TextInputStyle.SHORT)
                .setPlaceholder("An AI assistant who is helpful, creative, and clever.")
                .build();

        Modal modal = Modal.create(MODAL_ID, "Chatbot Interface")
                .addActionRows(ActionRow.of(model), ActionRow.of(chatbotName), ActionRow.of(chatbotDescription))
                .build();

        event.replyModal(modal).queue();

        */
    }

    /**
     * Called from ModalHandler when the `chat` modal is submitted.
     * @param event
     */
    public void readModal(ModalInteractionEvent event) {
        // Get the model (lowercase) and prompt from the modal.
        String model = event.getValues().stream()
                .filter(v -> v.getId().equals(MODAL_MODEL_ID))
                .findFirst().get().getAsString().toLowerCase();

        // Verify that the model is valid
        if(!UtilGPT.isValidModel(model)) {
            event.reply("'" + model + "' is an Invalid model.\nValid models are: " + Util.listModels())
                    .setEphemeral(true).queue();
            return;
        }

        String chatbotName = event.getValues().stream()
                .filter(v -> v.getId().equals(MODAL_NAME_ID))
                .findFirst().get().getAsString();

        String chatbotDescription = event.getValues().stream()
                .filter(v -> v.getId().equals(MODAL_DESCRIPTION_ID))
                .findFirst().get().getAsString();

        // create a new discord thread for the chatbot
        ThreadChannel threadChannel = event.getTextChannel().createThreadChannel(chatbotName).complete();
        threadChannel.sendMessage(chatbotName).queue();

        //TODO add the thread to the list of threads created by /chat
    }

    public void registerMessage(long threadID, String message, MessageReceivedEvent event) {

    }

    @Override
    public void addCommand(JDA jda) {
        jda.upsertCommand(name, description).complete();
    }

    @Override
    public String getDescription() {
        return super.description;
    }

    public static boolean isChatThread(long threadID) {
        return threadMap.containsKey(threadID);
    }
}

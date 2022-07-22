package wood.commands;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import wood.Settings;
import wood.discord_threads.ChatThread;
import wood.util.GPTRequest;
import wood.util.GPTUtil;

import java.util.HashMap;
import java.util.Map;

public class Chat extends Commands {
    /** Map of each thread created by `/chat` to its prompt related data */
    @Getter
    private static final Map<Long, ChatThread> threadMap = new HashMap<>();

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

        TextInput chatbotDescription = TextInput.create(MODAL_DESCRIPTION_ID, "Chatbot Description", TextInputStyle.PARAGRAPH)
                .setPlaceholder("An AI assistant who is helpful, creative, and clever.")
                .build();

        Modal modal = Modal.create(MODAL_ID, "Chatbot Interface")
                .addActionRows(ActionRow.of(model), ActionRow.of(chatbotName), ActionRow.of(chatbotDescription))
                .build();

        event.replyModal(modal).queue();

    }

    /**
     * Called from ModalHandler when the `chat-modal` modal is submitted.
     * Creates a new thread with the settings specified in the modal, and sends a greeting in the thread
     * @param event
     */
    public void readModal(ModalInteractionEvent event) {
        // Get the model (lowercase) and prompt from the modal.
        String model = event.getValues().stream()
                .filter(v -> v.getId().equals(MODAL_MODEL_ID))
                .findFirst().get().getAsString().toLowerCase();

        // Verify that the model is valid
        if(!GPTUtil.isValidModel(model)) {
            event.reply("'" + model + "' is an Invalid model.\nValid models are: " + GPTUtil.listModels())
                    .setEphemeral(true).queue();
            return;
        }

        String chatbotName = event.getValues().stream()
                .filter(v -> v.getId().equals(MODAL_NAME_ID))
                .findFirst().get().getAsString();

        String chatbotDescription = event.getValues().stream()
                .filter(v -> v.getId().equals(MODAL_DESCRIPTION_ID))
                .findFirst().get().getAsString();

        // create a new discord thread for the chatbot, and add it to the threadMap of all chatbots.
        ThreadChannel threadChannel = event.getTextChannel().createThreadChannel(chatbotName).complete();
        ChatThread chatThread = new ChatThread(threadChannel, model, chatbotName, chatbotDescription);
        threadMap.put(threadChannel.getIdLong(), chatThread);

        // modal gives an error (in the Discord UI) if no reply is given
        event.reply("Chat-bot thread created").setEphemeral(true).queue();

        // generate the chatbot's first message
        String prompt = "Description of " + chatbotName + ": " + chatbotDescription + "\n\n"
                + "The following is a conversation in a Discord server with " + chatbotName + ". "
                + chatbotName + " starts with a greeting.\n" + chatThread.getChatbotDisplayName();
        String completion = new GPTRequest.GPTRequestBuilder(
                model, prompt, Settings.chatCompletionTokens, true)
                .build().request(true);
        completion = completion.replaceFirst("[\\s\\n]*", ""); // remove leading whitespace or newline

        // send and log the first message
        String message = chatThread.getChatbotDisplayName() + completion;
        threadChannel.sendMessage(message).queue();
        chatThread.registerMessage(message, prompt + completion);
    }

    /**
     * Called when a user sends a message in a thread created by /chat
     * Has the chat-bot reply to the user.
     * @param threadID The ID of the thread the message was sent in.
     * @param message The message that was sent.
     * @param event
     */
    public void registerMessage(long threadID, String message, MessageReceivedEvent event) {
        ChatThread chatThread = threadMap.get(threadID);

        // format the message for how it'll be sent to GPT-3
        String authorNameFormatted = ChatThread.handleNamePrefix + event.getAuthor().getName() + ChatThread.handleNameSuffix;
        String messageFormatted = authorNameFormatted + message;
        chatThread.registerMessage(message, messageFormatted);

        // send the message to GPT-3
        String prompt = chatThread.getChatHistoryWithinTokenLimit() + chatThread.getChatbotDisplayName();
        String completion = new GPTRequest.GPTRequestBuilder(
                chatThread.getModel(), prompt, Settings.chatCompletionTokens, true)
                .build().request(true);
        completion = completion.replaceFirst("[\\s\\n]*", ""); // remove leading whitespace or newline

        // send and log the message
        String response = chatThread.getChatbotDisplayName() + completion;
        event.getThreadChannel().sendMessage(response).queue();
        chatThread.registerMessage(response, response);
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

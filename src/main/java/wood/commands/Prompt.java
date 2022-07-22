package wood.commands;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import wood.Settings;
import wood.discord_threads.PromptThread;
import wood.util.DiscordUtil;
import wood.util.GPTRequest;
import wood.util.GPTUtil;

import java.util.HashMap;
import java.util.Map;

public class Prompt extends Commands {
    /** Map of each thread created by `/prompt` to its prompt related data */
    @Getter private static final Map<Long, PromptThread> threadMap = new HashMap<>();

    public static final String MODAL_ID = "prompt-modal";
    private static final String MODAL_MODEL_ID = "model", MODAL_PROMPT_ID = "prompt";

    public Prompt() {
        super.name = "prompt";
        super.description = "Opens a modal to create a prompt for GPT-3";
    }

    /**
     * Runs when '/prompt' is used.  Creates a modal for the user to input a prompt.
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

        TextInput body = TextInput.create(MODAL_PROMPT_ID, "Prompt", TextInputStyle.PARAGRAPH)
                .build();

        Modal modal = Modal.create(MODAL_ID, "GPT-3 Interface")
                .addActionRows(ActionRow.of(model), ActionRow.of(body))
                .build();

        event.replyModal(modal).queue();

    }

    /**
     * Called from ModalHandler when the `prompt` modal is submitted.
     * Creates a new thread with the settings specified in the modal, and sends a message in the thread
     * containing both the prompt, and a GPT-3 completion.
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

        String prompt = event.getValues().stream()
                .filter(v -> v.getId().equals(MODAL_PROMPT_ID))
                .findFirst().get().getAsString();

        // if the prompt is too long, cancel the command
        int promptTokens = prompt.length() / 4 + Settings.promptCompletionTokens;
        double promptCost = GPTUtil.tokensToUSD(promptTokens, model);
        if(promptCost > Settings.maxCostPerAPIRequest) {
            int maxTokens = GPTUtil.usdToTokens(Settings.maxCostPerAPIRequest, model) - Settings.promptCompletionTokens;
            // include the prompt in the ephemeral reply so the data isn't lost
            event.reply(String.format("The prompt is too long - the maximum prompt size for the %s model is %d tokens"
                + " (roughly %d characters).%n%nGiven prompt:%n```%n%s%n```", model, maxTokens, maxTokens * 4, prompt))
                    .setEphemeral(true).queue();
            return;
        }

        // create a name for the thread
        String threadNamePrompt = "Given the following prompt: \"" + prompt +
                "\"\nA creative, yet very short title for the prompt is:";
        String threadName = Settings.gptGeneratedThreadNames ? new GPTRequest.GPTRequestBuilder(
                GPTUtil.convertToInstructModel(Settings.model), threadNamePrompt, 7, true)
                .frequencyPenalty(.76).build().request()
                : Settings.defaultThreadName;

        // create a new discord thread
        ThreadChannel threadChannel = event.getTextChannel().createThreadChannel(threadName).complete();

        // modal gives an error (in the Discord UI) if no reply is given
        event.reply("Thread created").setEphemeral(true).queue();

        threadChannel.sendTyping().deadline(System.currentTimeMillis()).queue();

        String completion = new GPTRequest.GPTRequestBuilder(model, prompt, Settings.promptCompletionTokens)
                .build().request(true);
        completion = DiscordUtil.addDiscordUnderline(completion);
        Message firstMsg = threadChannel.sendMessage(prompt + completion).complete();

        // add the thread to the map of threads created by /prompt
        threadMap.put(threadChannel.getIdLong(), new PromptThread(threadChannel.getIdLong(), model, firstMsg));
    }

    /**
     * Called when a user sends a message in a thread created by /prompt
     * @param threadID The ID of the thread the message was sent in.
     * @param message The message that was sent.
     * @param event
     */
    public void registerMessage(long threadID, String message, MessageReceivedEvent event) {
        event.getMessage().delete().queue();

        PromptThread thread = threadMap.get(threadID);
        thread.concatenateToPrompt(message);

        // remove discord underlines from the prompt
        String prompt = thread.getPrompt().replaceAll("__", "");

        // if the prompt is too long, don't make the API request
        int tokens = prompt.length() / 4 + Settings.promptCompletionTokens;
        double promptCost = GPTUtil.tokensToUSD(tokens, thread.getModel());
        if(promptCost > Settings.maxCostPerAPIRequest) {
            int maxTokens = GPTUtil.usdToTokens(Settings.maxCostPerAPIRequest, thread.getModel()) - Settings.promptCompletionTokens;

            event.getChannel().sendMessage(String.format("The prompt has gotten too long - the maximum prompt size for the %s"
                    + " model is %d tokens (roughly %d characters).  Edit the prompt using the /edit command.",
                    thread.getModel(), maxTokens, maxTokens * 4)).queue();
            return;
        }
        else { // Make an API request using the prompt, and add the completion to the first message in the thread
            event.getChannel().sendTyping().deadline(System.currentTimeMillis()).queue();

            String completion = new GPTRequest.GPTRequestBuilder(
                    thread.getModel(), prompt, Settings.promptCompletionTokens,true)
                    .build().request(true);

            completion = DiscordUtil.addDiscordUnderline(completion);
            thread.concatenateToPrompt(completion);
            thread.getMessage().editMessage(thread.getPrompt()).queue();
        }
    }

    @Override
    public void addCommand(JDA jda) {
        jda.upsertCommand(name, description).complete();
    }

    @Override
    public String getDescription() {
        return super.description;
    }

    public static boolean isPromptThread(long threadID) {
        return threadMap.containsKey(threadID);
    }
}

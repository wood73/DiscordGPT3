package wood.discord_threads;

import lombok.Getter;
import net.dv8tion.jda.api.entities.ThreadChannel;
import wood.Settings;
import wood.util.GPTUtil;

import java.util.ArrayList;
import java.util.List;

public class ChatThread {

    private final ThreadChannel threadChannel;
    @Getter private final long threadID;
    @Getter private final String model;

    /** The name of this chatbot */
    private final String chatbotName;

    /** The description of this chatbot */
    private final String chatbotDescription;

    /** Raw string content of the discord messages */
    private final List<String> messages = new ArrayList<>();
    /** Discord messages formatted for GPT-3 to generate better responses */
    private final List<String> gptFormattedMsgs = new ArrayList<>();
    /** A list of gptFormattedMessages that fit within Settings.maxCostPerAPIRequest */
    private final List<String> gptFormattedMsgsLimited = new ArrayList<>();
    /** The number of tokens inside gptFormattedMsgsLimited */
    private int tokensInGPTFormattedMsgsLimited = 0;

    /** What precedes and follows names in the chat */
    public final static String handleNamePrefix = "[", handleNameSuffix = "]> ";
    /** The chatbot's name formatted with handleNamePrefix and handleNameSuffix */
    @Getter public final String chatbotDisplayName;

    /** Whether the users have been notified that the prompt is being shortened to fit within Settings.maxCostPerAPIRequest limit */
    private boolean hasNotifiedUserOfPromptShortening = false;

    public ChatThread(ThreadChannel threadChannel, String model, String chatBotName, String chatBotDescription) {
        this.threadChannel = threadChannel;
        this.threadID = threadChannel.getIdLong();
        this.model = model;
        this.chatbotName = chatBotName;
        this.chatbotDescription = chatBotDescription;
        this.chatbotDisplayName = handleNamePrefix + chatBotName + handleNameSuffix;
    }

    public void registerMessage(String message, String gptFormattedMsg) {
        messages.add(message);
        gptFormattedMsgs.add(gptFormattedMsg);
        gptFormattedMsgsLimited.add(gptFormattedMsg);
        tokensInGPTFormattedMsgsLimited += GPTUtil.countTokens(gptFormattedMsg);

        // keep removing the 2nd message from gptFormattedMsgsLimited until it costs under Settings.maxCostPerAPIRequest
        while(Settings.maxCostPerAPIRequest < GPTUtil.tokensToUSD(tokensInGPTFormattedMsgsLimited, model)) {
            tokensInGPTFormattedMsgsLimited -= GPTUtil.countTokens(gptFormattedMsgsLimited.remove(1)) + 1; // +1 for the \n between messages (\n ~= 1 token)

            // send only one notification per chat thread
            if(!hasNotifiedUserOfPromptShortening) {
                threadChannel.sendMessage("`To prevent the chat-bot's memory from exceeding its limit,"
                        + " the oldest messages will be forgotten as needed.`").queue();
                hasNotifiedUserOfPromptShortening = true;
            }
        }
    }

    /**
     * @return The chat history of this thread that doesn't exceed Settings.maxCostPerAPIRequest -
     *         if the chat history is too long, the oldest messages will have been removed (excluding the first)
     */
    public String getChatHistoryWithinTokenLimit() {
        return gptFormattedMsgsLimited.stream().reduce("", (a, b) -> a + (a.length() == 0 ? "" : "\n") + b) + "\n";
    }

}

package wood.discord_threads;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Message;
import wood.util.Util;

//TODO make thread data persistent
public class PromptThread {

    @Getter
    private final long threadID;

    /** The model to use in the thread */
    @Getter @Setter
    private String model;

    /** The ever-changing prompt; both the user's input, and GPT-3 completions are appended to this. */
    @Getter private String prompt;

    /** The Message object of the first message in this thread */
    @Getter private final Message message;

    public PromptThread(long threadID, String model, Message message) {
        this.threadID = threadID;
        this.model = model;
        this.prompt = message.getContentRaw();
        this.message = message;
    }

    /**
     * Concatenates the given text to the prompt. Both the user's input and GPT-3 completions are appended.
     * @param text The text to append to the prompt
     */
    public void concatenateToPrompt(String text) {
        // if prompt doesn't end with a space or newline, and text starts with an alphabetic character, add a space between them
        if(!Util.endsWith(prompt, "[\\s\\n]") && Util.startsWith(text, "[a-zA-Z]"))
            prompt += " " + text;
        else
            prompt += text;
    }

}

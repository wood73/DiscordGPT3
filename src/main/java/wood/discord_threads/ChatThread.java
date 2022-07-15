package wood.discord_threads;

import java.util.ArrayList;
import java.util.List;

// rough outline
public class ChatThread {

    private final long threadID;
    private final String model;
    private final List<String> messages = new ArrayList<>();

    public ChatThread(long threadID, String model, String message) {
        this.threadID = threadID;
        this.model = model;
        this.messages.add(message);
    }

    public long getThreadID() {
        return threadID;
    }

    public String getModel() {
        return model;
    }

    public List<String> getMessages() {
        return messages;
    }

}

package wood.util;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;

import java.util.stream.IntStream;

import wood.util.StringUtil;

@Slf4j
public class DiscordUtil {

    public static boolean isFirstMessageInThread(GenericMessageReactionEvent event, Message msg) {
        if(event.getChannelType().isThread()) {
            return msg.getId().equals(
                    event.getChannel().getHistoryFromBeginning(1).complete().getRetrievedHistory().get(0).getId());
        }
        else {
            log.error("Message is not in a thread.");
            return false;
        }
    }

    /**
     * @param text The string to underline
     * @return The underlined string - in each line of the string, an underline will be added between the first and last
     *          non-whitespace characters.
     */
    public static String addDiscordUnderline(String text) {
        String[] lines = text.split("\n");
        IntStream.range(0, lines.length).forEach(i -> {
            if(StringUtil.contains(lines[i], "\\S")) {
                int firstNonWhitespaceIndex = StringUtil.indexOf(lines[i], "\\S").get(),
                        lastNonWhitespaceIndex = StringUtil.lastIndexOf(lines[i], "\\S").get();
                lines[i] = StringUtil.leadingSpaces(lines[i]) + "__" + lines[i].substring(firstNonWhitespaceIndex,
                        lastNonWhitespaceIndex + 1) + "__" + StringUtil.trailingSpaces(lines[i]);
            }
        });
        return String.join("\n", lines);
    }

    public static Message getFirstMessageInThread(ThreadChannel thread) {
        return thread.getHistoryFromBeginning(1).complete().getRetrievedHistory().get(0);
    }

    public static boolean isFirstMessageInThread(GenericMessageEvent event, Message msg) {
        if(event.getChannelType().isThread()) {
            return msg.getId().equals(
                    event.getChannel().getHistoryFromBeginning(1).complete().getRetrievedHistory().get(0).getId());
        }
        else {
            log.error("Message is not in a thread.");
            return false;
        }
    }

}

package wood.handler;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import wood.commands.Chat;
import wood.commands.Prompt;
import wood.util.DiscordUtil;


public class MessageHandler extends ListenerAdapter {

    private final Prompt promptCmd;
    private final Chat chatCmd;


    public MessageHandler(Prompt promptCmd, Chat chatCmd) {
        this.promptCmd = promptCmd;
        this.chatCmd = chatCmd;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getAuthor().isBot()) {
            boolean isInPromptThread = event.getChannelType().isThread() &&
                    Prompt.isPromptThread(event.getThreadChannel().getIdLong());
            boolean isEphemeral = event.getMessage().isEphemeral();

            // if the bot that sent a message is in a /prompt thread, and it both isn't the first message and isn't ephemeral,
            // add emoji reaction allowing user to delete it (ReactionHandler).
            if(isInPromptThread && !isEphemeral && !DiscordUtil.isFirstMessageInThread(event, event.getMessage())) {
                event.getMessage().addReaction(Emoji.fromUnicode(ReactionHandler.trashEmoji)).queue();
            }
            return;
        }

        // If a user sends a message in a /prompt thread, handle it in the Prompt class
        // else if sent inside a /chat thread, handle it in the Chat class.
        if(event.getChannelType().isThread() && Prompt.isPromptThread(event.getThreadChannel().getIdLong())) {
            promptCmd.registerMessage(event.getThreadChannel().getIdLong(), event.getMessage().getContentDisplay(), event);
        }
        else if(event.getChannelType().isThread() && Chat.isChatThread(event.getThreadChannel().getIdLong())) {
            chatCmd.registerMessage(event.getThreadChannel().getIdLong(), event.getMessage().getContentDisplay(), event);
        }
    }

}

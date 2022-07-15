package wood;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import wood.commands.Chat;
import wood.commands.Prompt;
import wood.handler.CommandHandler;
import wood.handler.MessageHandler;
import wood.handler.ModalHandler;
import wood.handler.ReactionHandler;
import wood.services.DiscordTokenService;
import wood.services.OpenAIKeyService;

import javax.security.auth.login.LoginException;

@Slf4j
public class Main {

    public static JDA jda;

    public static void main(String[] args) {
        OpenAIKeyService.load(args); // load OpenAI key from either args or file, and set UtilGPT.apiKey
        String token = DiscordTokenService.load(args); // load Discord token from either args or file

        try {

            JDABuilder builder = JDABuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.GUILD_MESSAGE_REACTIONS);

            builder.addEventListeners(
                    new CommandHandler(),
                    new MessageHandler((Prompt)CommandHandler.commandMap.get(CommandHandler.PROMPT_CMD),
                                        (Chat)CommandHandler.commandMap.get(CommandHandler.CHAT_CMD)),
                    new ModalHandler((Prompt)CommandHandler.commandMap.get(CommandHandler.PROMPT_CMD),
                                        (Chat)CommandHandler.commandMap.get(CommandHandler.CHAT_CMD)),
                    new ReactionHandler());
            builder.setStatus(OnlineStatus.ONLINE);
            jda = builder.build().awaitReady();
            log.info("DiscordGPT3 successfully started");

            CommandHandler.checkAndSetSlashCommands();

        } catch(LoginException e) {
            log.error("Invalid Discord bot Token", e);
        } catch (Exception e) {
            log.error("Error connecting to discord", e);
        }
    }

}

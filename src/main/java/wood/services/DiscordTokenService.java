package wood.services;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Slf4j
public class DiscordTokenService {

    public static final String TOKEN_FILE = "discord_token.txt";

    /**
     * Loads the bot token - if token is given in command line arguments (-token <token>) then it is saved to
     * TOKEN_FILE.  If it isn't given as an argument, it'll look for it inside TOKEN_FILE
     * @param args Command line arguments
     * @return Discord bot token, either extracted from command line arguments, or from the file system at
     *         TokenService.TOKEN_FILE
     * @throws IllegalArgumentException if -token is passed as a command line argument, but no token is given
     * @throws Exception if the token doesn't exist from either the command line or the file system
     */
    public static String load(String[] args) throws IllegalArgumentException, Exception {

        String argsToken = "";
        boolean foundArgsToken = false;
        for(int i = 0; i < args.length; i++) {
            if(args[i].equalsIgnoreCase("-token")) {
                // if -token both isn't the last argument, and the next argument isn't another command
                if(i+1 != args.length && !args[i+1].startsWith("-")) {
                    argsToken = args[i + 1];
                    foundArgsToken = true;
                }
                else {
                    throw new IllegalArgumentException("error: -token requires an argument. Usage: -token <discord_bot_token>");
                }
            }
        }

        if(foundArgsToken) {
            writeTokenFile(argsToken);
            log.info("Token saved to '" + new File(TOKEN_FILE).getAbsoluteFile().getAbsolutePath() + "'");
            return argsToken;
        }
        else {
            Optional<String> token = readTokenFile();

            if(token.isPresent()) {
                return token.get();
            }
            else {
                throw new Exception("error: no bot token found. To set it, use the -token command line argument."
                        + " Usage: -token <your_token>");
            }
        }
    }

    /**
     * Loads the token from FileService.TOKEN_FILE
     * @return Optional.empty() if the file doesn't exist (or is empty),
     *      otherwise it'll return FileService.TOKEN_FILE's contents
     */
    public static Optional<String> readTokenFile() {
        File tokenFile = new File(TOKEN_FILE);

        if(tokenFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(tokenFile.toPath());

                if (lines.size() == 0) //will be 0 if the file is empty
                    return Optional.empty();
                else
                    return Optional.of(lines.get(0).trim());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return Optional.empty();
    }

    /**
     * Overwrite token file if it exists, or create it if it doesn't
     * @param token discord bot token
     */
    public static void writeTokenFile(String token) {
        try(PrintWriter pw = new PrintWriter(TOKEN_FILE)) {
            pw.print(token);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}

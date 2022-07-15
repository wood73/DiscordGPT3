package wood.services;

import lombok.extern.slf4j.Slf4j;
import wood.util.Util;

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
     * TOKEN_FILE.  If it isn't given as an argument, it'll look for it inside TOKEN_FILE - if this fails, a
     * RuntimeException is thrown
     * @param args Command line arguments
     * @return Discord bot token, either extracted from command line arguments, or from the file system at
     *         TokenService.TOKEN_FILE
     */
    public static String load(String[] args) {

        //argsToken will be an empty String if `-token <token>` wasn't passed as a command line argument
        String argsToken = IntStream.range(0, args.length).boxed().reduce("", (a, i) -> {
            //throw error if -token is last argument, or if the argument after -token is another command
            if((args[i].equalsIgnoreCase("-token") && i+1 == args.length) ||
                    (args[i].equalsIgnoreCase("-token") && i+1 != args.length && args[i+1].startsWith("-"))) {

                String runtimeError = "error: -token requires an argument. Usage: -token <your_token>";
                log.error(runtimeError);
                throw new RuntimeException(runtimeError);
            }
            else { //if -token command is found, set the accumulator 'a' = next argument, else don't change 'a'
                return args[i].equalsIgnoreCase("-token") ? args[i+1] : a;
            }}, (a,i)->""); // combiner used to allow the 2 parameters inside the accumulator (a & i) to be different types


        if(argsToken.length() > 0) {
            writeTokenFile(argsToken);
            return argsToken;
        }
        else {
            Optional<String> token = readTokenFile();

            if(token.isPresent()) {
                return token.get();
            }
            else {
                Util.runtimeException("error: no bot token found. To set it, use the -token command line argument. "
                        + "Usage: -token <your_token>");
                return null; // unreachable code
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

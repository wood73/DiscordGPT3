package wood.services;

import lombok.extern.slf4j.Slf4j;
import wood.util.GPTRequest;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Slf4j
public class OpenAIKeyService {

    public static final String KEY_FILE = "OpenAI_key.txt";

    /**
     * Loads the OpenAI key, and if it is valid, sets UtilGPT.apiKey
     * If the key is passed as a command line argument (-AIkey <OpenAI_API_Key>) then it is saved to
     * KEY_FILE.  If it isn't given as an argument, it'll look for it inside KEY_FILE
     * @param args Command line arguments
     * @throws IllegalArgumentException if -AIkey is passed as a command line argument, but no key is given
     * @throws Exception if the key is invalid, or doesn't exist from either the command line or the file system
     */
    public static void load(String[] args) throws IllegalArgumentException, Exception {
        String argsKey = "";
        boolean foundArgsKey = false;
        for(int i = 0; i < args.length; i++) {
            if(args[i].equalsIgnoreCase("-AIkey")) {
                // if -AIkey both isn't the last argument, and the next argument isn't another command
                if(i+1 != args.length && !args[i+1].startsWith("-")) {
                    argsKey = args[i + 1];
                    foundArgsKey = true;
                }
                else {
                    throw new IllegalArgumentException("error: -AIkey requires an argument. Usage: -AIkey <OpenAI_API_Key>");
                }
            }
        }

        if(foundArgsKey) {
            // if the key is valid, set it and write it to the file
            if(GPTRequest.testAndSetApiKey(argsKey)) {
                writeKeyFile(argsKey);
                log.info("OpenAI API key saved to '" + new File(KEY_FILE).getAbsoluteFile().getAbsolutePath() + "'");
            }
            else {
                throw new IllegalArgumentException("error: Invalid OpenAI API key passed as a command line argument: '" + argsKey
                        + "'. Usage: -AIkey <OpenAI_API_Key>");
            }
        }
        else { // look for the key in the file
            Optional<String> key = readKeyFile();

            if(key.isPresent()) {
                // if the key is invalid, throw an exception - if the key is valid, testAndSetApiKey will set it
                if(!GPTRequest.testAndSetApiKey(key.get())) {
                    throw new Exception("Invalid OpenAI API key found in file: '" + key.get() + "'. "
                        + "Usage: -AIkey <OpenAI_API_Key>");
                }
            }
            else { // key both not in file, and not passed as a command line argument
                throw new Exception("error: no OpenAI API key found. To set it, use the -AIkey command line argument. "
                    + "Usage: -AIkey <OpenAI_API_Key>");
            }
        }
    }

    /**
     * Loads the key from KEY_FILE
     * @return Optional.empty() if the file doesn't exist (or is empty),
     *      otherwise it'll return KEY_FILE's contents
     */
    public static Optional<String> readKeyFile() {
        File tokenFile = new File(KEY_FILE);

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
     * Overwrite key file if it exists, or create it if it doesn't
     * @param key OpenAI API key
     */
    public static void writeKeyFile(String key) {
        try(PrintWriter pw = new PrintWriter(KEY_FILE)) {
            pw.print(key);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}

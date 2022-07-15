package wood.services;

import lombok.extern.slf4j.Slf4j;
import wood.util.Util;
import wood.util.UtilGPT;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Slf4j
public class OpenAIKeyService {

    public static final String KEY_FILE = "OpenAI_key.txt";

    /**
     * Loads the OpenAI key, and if it is valid, sets UtilGPT.apiKey; if it's invalid, throws a RuntimeException
     * If the key is passed as a command line argument (-AIkey <OpenAI_API_Key>) then it is saved to
     * KEY_FILE.  If it isn't given as an argument, it'll look for it inside KEY_FILE
     * @param args Command line arguments
     */
    public static void load(String[] args) {

        // argsKey will be an empty String if `-AIkey <OpenAI_API_Key>` wasn't passed as a command line argument
        String argsKey = IntStream.range(0, args.length).boxed().reduce("", (a, i) -> {
            // throw error if -AIkey is last argument, or if the argument after -AIkey is another command
            if((args[i].equalsIgnoreCase("-AIkey") && i+1 == args.length) ||
                    (args[i].equalsIgnoreCase("-AIkey") && i+1 != args.length && args[i+1].startsWith("-"))) {

                Util.runtimeException("error: -AIkey requires an argument. Usage: -AIkey <OpenAI_API_Key>");
                return null; // unreachable code
            }
            else { // if -AIkey command is found, set the accumulator 'a' = next argument, else don't change 'a'
                return args[i].equalsIgnoreCase("-AIkey") ? args[i+1] : a;
            }}, (a,i)->""); // combiner used to allow the 2 parameters inside the accumulator (a & i) to be different types

        // if `-AIkey <OpenAI_API_Key>` was passed as a command line argument
        if(argsKey.length() > 0) {
            // if the key is valid, set it and write it to the file
            if(UtilGPT.setAndTestApiKey(argsKey)) {
                writeKeyFile(argsKey);
            }
            else {
                Util.runtimeException("Invalid OpenAI API key passed as a command line argument: '" + argsKey
                        + "'. Usage: -AIkey <OpenAI_API_Key>");
            }
        }
        else {
            Optional<String> key = readKeyFile();

            if(key.isPresent()) {
                // if the key is invalid, throw a RuntimeException
                if(!UtilGPT.setAndTestApiKey(key.get())) {
                    Util.runtimeException("Invalid OpenAI API key found in file: '" + key.get() + "'. "
                        + "Usage: -AIkey <OpenAI_API_Key>");
                }
            }
            else {
                Util.runtimeException("error: no OpenAI API key found. To set it, use the -AIkey command line argument. "
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

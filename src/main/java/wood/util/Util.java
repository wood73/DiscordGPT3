package wood.util;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Slf4j
public class Util {

    /**
     * Check if a string contains at least one match of a given regex
     * @param text The string to test
     * @param regex The regex to match
     * @return  True if the string contains at least one match of the regex
     */
    public static boolean contains(String text, String regex) {
        return Pattern.compile(regex).matcher(text).find();
    }

    /**
     * Check if the beginning of a string matches a given regex
     * @param text The string to test
     * @param regex The regex to match
     * @return  True if the string starts with the given regex
     */
    public static boolean startsWith(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() && matcher.start() == 0;
    }

    /**
     * Check if the end of a string matches a given regex
     * @param text The string to test
     * @param regex The regex to match
     * @return  True if the string ends with the given regex
     */
    public static boolean endsWith(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        while(matcher.find()) {
            if(matcher.end() == text.length()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the first occurrence of the given regex in the given string
     * @param text The string to search in
     * @param regex The regex to match
     * @return  Optional.empty() if no match was found. Otherwise, it'll return the index of the first character
     *          inside the first matching regex in the string.
     */
    public static Optional<Integer> indexOf(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);

        Optional<Integer> firstIndex = Optional.empty();
        if(matcher.find()) {
            firstIndex = Optional.of(matcher.start());
        }
        return firstIndex;
    }

    /**
     * Finds the last occurrence of the given regex in the given string
     * @param text The string to search in
     * @param regex The regex to match
     * @return  Optional.empty() if no match was found. Otherwise, it'll return the index of the final character
     *          inside the last matching regex in the string.
     */
    public static Optional<Integer> lastIndexOf(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);

        Optional<Integer> lastIndex = Optional.empty();
        while(matcher.find()) {
            lastIndex = Optional.of(matcher.end() - 1);
        }
        return lastIndex;
    }

    /**
     * Finds the last occurrence of the given regex in the given substring
     * @param text The string to search in
     * @param regex The regex to match
     * @param startIndex The index (inclusive) to start searching from
     * @return  Optional.empty() if no match was found in the substring. Otherwise, it'll return the index of
     *          the final character inside the last matching regex in the substring.
     */
    public static Optional<Integer> lastIndexOf(String text, String regex, int startIndex) {
        Optional<Integer> lastIndexInSubstring = lastIndexOf(text.substring(startIndex), regex);

        if(lastIndexInSubstring.isPresent())
            return Optional.of(lastIndexInSubstring.get() + startIndex);
        else
            return Optional.empty();
    }

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
            if(contains(lines[i], "\\S")) {
                int firstNonWhitespaceIndex = indexOf(lines[i], "\\S").get(),
                        lastNonWhitespaceIndex = lastIndexOf(lines[i], "\\S").get();
                lines[i] = leadingSpaces(lines[i]) + "__" + lines[i].substring(firstNonWhitespaceIndex,
                        lastNonWhitespaceIndex + 1) + "__" + trailingSpaces(lines[i]);
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

    /**
     * @param text The string to extract the leading spaces from
     * @return The leading spaces of the given string
     */
    public static String leadingSpaces(String text) {
        if(text.length() > 0 && text.charAt(0) == ' ')
            return contains(text, "\\S") ? text.substring(0, indexOf(text, "\\S").get()) : text;
        else
            return "";
    }

    /**
     * @param text The string to extract the trailing spaces from
     * @return The trailing spaces of the given string
     */
    public static String trailingSpaces(String text) {
        if(text.length() > 0 && text.charAt(text.length() - 1) == ' ')
            return contains(text, "\\S") ? text.substring(lastIndexOf(text, "\\S").get() + 1) : text;
        else
            return "";
    }

    /** @return A ', ' separated string of model names. */
    public static String listModels() {
        return UtilGPT.davinci + ", " + UtilGPT.curie + ", " + UtilGPT.babbage + ", " + UtilGPT.ada + ", " +
                UtilGPT.inDavinci + ", " + UtilGPT.inCurie + ", " + UtilGPT.inBabbage + ", " + UtilGPT.inAda;
    }

    /**
     * Converts USD to tokens based on the language model.
     * @param usd The amount of USD to convert to tokens.
     * @param model The language model that the tokens will be based on.
     * @return The number of tokens for the model that equate to the given USD.
     */
    public static int usdToTokens(double usd, String model) {
        double priceDavinciToken = .06/1000, priceCurieToken = .006/1000, priceBabbageToken = .0012/1000,
                priceAdaToken = .0008/1000;

        switch(model) {
            case UtilGPT.davinci:
            case UtilGPT.inDavinci:
                return (int) (usd / priceDavinciToken);
            case UtilGPT.curie:
            case UtilGPT.inCurie:
                return (int) (usd / priceCurieToken);
            case UtilGPT.babbage:
            case UtilGPT.inBabbage:
                return (int) (usd / priceBabbageToken);
            case UtilGPT.ada:
            case UtilGPT.inAda:
                return (int) (usd / priceAdaToken);
            default:
                throw new RuntimeException("Invalid model: " + model);
        }
    }

    /**
     * Converts tokens to USD based on the language model.
     * @param tokens The number of tokens to convert to USD.
     * @param model The language model that the tokens are based on.
     * @return The USD that equates to the given tokens and model.
     */
    public static double tokensToUSD(int tokens, String model) {
        double priceDavinciToken = .06/1000, priceCurieToken = .006/1000, priceBabbageToken = .0012/1000,
                priceAdaToken = .0008/1000;

        switch(model) {
            case UtilGPT.davinci:
            case UtilGPT.inDavinci:
                return tokens * priceDavinciToken;
            case UtilGPT.curie:
            case UtilGPT.inCurie:
                return tokens * priceCurieToken;
            case UtilGPT.babbage:
            case UtilGPT.inBabbage:
                return tokens * priceBabbageToken;
            case UtilGPT.ada:
            case UtilGPT.inAda:
                return tokens * priceAdaToken;
            default:
                throw new RuntimeException("Invalid model: " + model);
        }
    }

    /**
     * Logs the message, then ends the program using a RuntimeException
     * @param message What to log and print inside the RuntimeException
     */
    public static void runtimeException(String message) {
        log.error(message);
        throw new RuntimeException(message);
    }

    public static String convertToInstructModel(String model) {
        switch(model.toLowerCase()) {
            case UtilGPT.davinci:
            case UtilGPT.inDavinci:
                return UtilGPT.inDavinci;
            case UtilGPT.curie:
            case UtilGPT.inCurie:
                return UtilGPT.inCurie;
            case UtilGPT.babbage:
            case UtilGPT.inBabbage:
                return UtilGPT.inBabbage;
            case UtilGPT.ada:
            case UtilGPT.inAda:
                return UtilGPT.inAda;
            default:
                throw new RuntimeException("Invalid model: " + model);
        }
    }

}

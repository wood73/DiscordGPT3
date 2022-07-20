package wood.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

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

}

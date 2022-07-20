package wood.util;

import java.util.Arrays;

public class GPTUtil {

    /**
     * Approximately counts the number of tokens in the text.
     *
     * Token calculation currently used is imperfect:
     * 1 token = about 4 english chars - https://help.openai.com/en/articles/4936856-what-are-tokens-and-how-to-count-them
     *
     * @param text The text to count tokens in.
     * @return Approximately the number of tokens in the text.
     */
    public static int countTokens(String text) {
        return (int)Math.ceil(text.length() / 4.0);
    }

    /** @param model The model to check.
     *  @return Case-sensitive check if the model is valid. */
    public static boolean isValidModel(String model) {
        return Arrays.asList(GPTRequest.davinci, GPTRequest.curie, GPTRequest.babbage, GPTRequest.ada,
                GPTRequest.inDavinci, GPTRequest.inCurie, GPTRequest.inBabbage, GPTRequest.inAda).contains(model);
    }

    /** @return A ', ' separated string of model names. */
    public static String listModels() {
        return GPTRequest.davinci + ", " + GPTRequest.curie + ", " + GPTRequest.babbage + ", " + GPTRequest.ada + ", " +
                GPTRequest.inDavinci + ", " + GPTRequest.inCurie + ", " + GPTRequest.inBabbage + ", " + GPTRequest.inAda;
    }

    public static String convertToInstructModel(String model) {
        switch(model.toLowerCase()) {
            case GPTRequest.davinci:
            case GPTRequest.inDavinci:
                return GPTRequest.inDavinci;
            case GPTRequest.curie:
            case GPTRequest.inCurie:
                return GPTRequest.inCurie;
            case GPTRequest.babbage:
            case GPTRequest.inBabbage:
                return GPTRequest.inBabbage;
            case GPTRequest.ada:
            case GPTRequest.inAda:
                return GPTRequest.inAda;
            default:
                throw new RuntimeException("Invalid model: " + model);
        }
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
            case GPTRequest.davinci:
            case GPTRequest.inDavinci:
                return (int) (usd / priceDavinciToken);
            case GPTRequest.curie:
            case GPTRequest.inCurie:
                return (int) (usd / priceCurieToken);
            case GPTRequest.babbage:
            case GPTRequest.inBabbage:
                return (int) (usd / priceBabbageToken);
            case GPTRequest.ada:
            case GPTRequest.inAda:
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
            case GPTRequest.davinci:
            case GPTRequest.inDavinci:
                return tokens * priceDavinciToken;
            case GPTRequest.curie:
            case GPTRequest.inCurie:
                return tokens * priceCurieToken;
            case GPTRequest.babbage:
            case GPTRequest.inBabbage:
                return tokens * priceBabbageToken;
            case GPTRequest.ada:
            case GPTRequest.inAda:
                return tokens * priceAdaToken;
            default:
                throw new RuntimeException("Invalid model: " + model);
        }
    }

}

package wood.util;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import wood.util.Util;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** A Utility class for the OpenAI GPT-3 Api Client in Java - https://github.com/TheoKanning/openai-java
 *  An OpenAI API key is required to use this class.
 */
@Slf4j
public class UtilGPT {

    // ----------- static fields -----------

    /** The OpenAI API key to use for all requests. Can set using the setAndTestApiKey method. */
    public static String apiKey = "";

    /** If the tokens used in the API request should include both the 'tokens' field,
     *  and the number of tokens in the prompt.
     *  Token calculation currently used is imperfect:
     * 1 token = about 4 english chars - https://help.openai.com/en/articles/4936856-what-are-tokens-and-how-to-count-them
     */
    @Setter private static boolean automaticallyIncludePromptTokens = true;

    /** counter for how many tokens have been used by each language model (irrespective of Base series vs Instruct) */
    private static int davinciTokenCounter = 0, curieTokenCounter = 0, babbageTokenCounter = 0, adaTokenCounter = 0;

    /** Whether data about API usage should be logged. Default is true. */
    @Setter private static boolean logData = true;

    /** Base series models */
    public static final String davinci = "davinci", curie = "curie", babbage = "babbage", ada = "ada";

    /** Instruct series models */
    public static final String inDavinci = "text-davinci-002", inCurie = "text-curie-001",
            inBabbage = "text-babbage-001", inAda = "text-ada-001";


    // ----------- instance fields -----------

    /** Language Model to use for this API request */
    @Getter private String model;

    /** The prompt to use for this API request */
    @Getter private String prompt;

    /** If automaticallyIncludePromptTokens is true, then the number of tokens used in the API request will be
     * this field added to the number of tokens in the prompt. */
    @Getter private int tokens;

    /** Echo back the prompt in addition to the completion.  False by default. */
    private boolean echoPrompt = false;

    private final OpenAiService service;
    private final CompletionRequest.CompletionRequestBuilder completionRequestBuilder;

    /**
     * Sets UtilGPT.apiKey, and tests if the API key is valid
     * API key validity is tested by a 1 token API request to the Ada model.
     * @param apiKey An OpenAI API key
     * @return Whether the API key is valid
     */
    public static boolean setAndTestApiKey(String apiKey) {
        try {
            UtilGPT.apiKey = apiKey;
            new UtilGPT(ada, "", 1).request();
            return true;
        }catch(Exception e) {
            return false;
        }
    }

    /**
     * Starts to build an API request for the given language model
     *
     * @param model, Language model to use for this API request. Valid Base Series models:
     *               UtilGPT.davinci, UtilGPT.curie, UtilGPT.babbage, UtilGPT.ada
     *               Valid Instruct Series models:
     *               UtilGPT.inDavinci, UtilGPT.inCurie, UtilGPT.inBabbage, UtilGPT.inAda
     * @param prompt, Prompt sent to the language model
     * @param tokens, If automaticallyIncludePromptTokens is false, it'll set the total number of tokens
     *                that the language model will use in this API request, between both the prompt, and the generated text.
     *                If automaticallyIncludePromptTokens is true, then the number of tokens in the prompt
     *                will be added to the total number of tokens when the API request method is called.
     */
    public UtilGPT(String model, String prompt, int tokens) {
        this.model = model;
        this.prompt = prompt;
        this.tokens = tokens;

        service = new OpenAiService(apiKey);
        completionRequestBuilder = CompletionRequest.builder()
                .prompt(prompt);

        completionRequestBuilder.maxTokens(tokens);

        // set default values
        completionRequestBuilder.temperature(0.7);
        completionRequestBuilder.topP(1.0);
        completionRequestBuilder.frequencyPenalty(0.0);
        completionRequestBuilder.presencePenalty(0.0);
        completionRequestBuilder.echo(echoPrompt);
    }

    /**
     * Makes an API request using the current UtilGPT settings.
     * @return If echoPrompt is true, returns the prompt + completion, else the completion is returned.
     */
    public String request() {
        int tokensToUse = automaticallyIncludePromptTokens ? prompt.length() / 4 + tokens : tokens;
        completionRequestBuilder.maxTokens(tokensToUse);

        log.info("prompt: " + prompt);
        log.info("tokens: " + tokensToUse);

        if(logData)
            logTokenUsage(tokensToUse);

        CompletionRequest completionRequest = completionRequestBuilder.build();
        List<CompletionChoice> outputList = service.createCompletion(model, completionRequest).getChoices();

        return outputList.get(0).getText();
    }

    /**
     * Makes an API request using the current UtilGPT settings.
     * @param endAtPunctuationMark Whether the completion should be cut off after the last punctuation mark
     * @return If echoPrompt is true, returns the prompt + completion, else the completion is returned.
     */
    public String request(boolean endAtPunctuationMark) {
        String output = request();

        if(endAtPunctuationMark) {
            // get the index of the last punctuation mark inside the completion (omitting the prompt)
            Optional<Integer> lastPunctuationIndex = Util.lastIndexOf(output, "[.!?]",
                    echoPrompt ? prompt.length() : 0);

            if(lastPunctuationIndex.isPresent())
                return output.substring(0, lastPunctuationIndex.get() + 1);
        }

        return output;
    }

    /** @param prompt, Prompt sent to the language model
     *  @return This UtilGPT, for chaining */
    public UtilGPT setPrompt(String prompt) {
        this.prompt = prompt;
        completionRequestBuilder.prompt(prompt);
        return this;
    }

    /**
     * @param model, Language model to use for this API request. Valid Base Series models:
     *               UtilGPT.davinci, UtilGPT.curie, UtilGPT.babbage, UtilGPT.ada
     *               Valid Instruct Series models:
     *               UtilGPT.inDavinci, UtilGPT.inCurie, UtilGPT.inBabbage, UtilGPT.inAda
     * @return This UtilGPT, for chaining
     */
    public UtilGPT setModel(String model) {
        this.model = model;
        return this;
    }

    /**
     * @param tokens, If automaticallyIncludePromptTokens is false, it'll set the total number of tokens
     *                that the language model will use in this API request, between both the prompt, and the generated text.
     *                If automaticallyIncludePromptTokens is true, then the number of tokens in the prompt
     *                will be added to the total number of tokens when the API request method is called.
     * @return This UtilGPT, for chaining
     */
    public UtilGPT setTokens(int tokens) {
        this.tokens = tokens;
        return this;
    }

    /** @param temperature (default .7) a value 0-1 with 1 being very creative, 0 being very factual/deterministic
     *  @return This UtilGPT, for chaining
     */
    public UtilGPT setTemperature(double temperature) {
        completionRequestBuilder.temperature(temperature);
        return this;
    }

    /** @param topP (default 1) between 0-1 where 1.0 means "use all tokens in the vocabulary"
     *               while 0.5 means "use only the 50% most common tokens"
     *  @return This UtilGPT, for chaining
     */
    public UtilGPT setTopP(double topP) {
        completionRequestBuilder.topP(topP);
        return this;
    }

    /** @param frequencyPenalty (default 0) 0-1, lowers the chances of a word being selected again
     *                           the more times that word has already been used
     *  @return This UtilGPT, for chaining
     */
    public UtilGPT setFrequencyPenalty(double frequencyPenalty) {
        completionRequestBuilder.frequencyPenalty(frequencyPenalty);
        return this;
    }

    /** @param presencePenalty (default 0) 0-1, lowers the chances of topic repetition
     *  @return This UtilGPT, for chaining
     */
    public UtilGPT setPresencePenalty(double presencePenalty) {
        completionRequestBuilder.presencePenalty(presencePenalty);
        return this;
    }

    /** @param bestOf (default 1), queries GPT-3 this many times, then selects the 'best' generation to return
     *  @return This UtilGPT, for chaining
     */
    public UtilGPT setBestOf(int bestOf) {
        completionRequestBuilder.bestOf(bestOf);
        return this;
    }

    /**
     * set the stop sequence, the String that GPT-3 will stop generating after
     *     (can have 4 stop sequences max)
     * @param stopSequences The Strings that GPT-3 will stop generating after (can have 4 stop sequences max)
     * @return This UtilGPT, for chaining
     */
    public UtilGPT setStopSequences(List<String> stopSequences) {
        if(stopSequences.size() > 4) {
            throw new IllegalArgumentException("Can only have 4 stop sequences max");
        }
        completionRequestBuilder.stop(stopSequences);
        return this;
    }

    /** @param echoPrompt Whether to echo back the prompt in addition to the completion.
     *  @return This UtilGPT, for chaining
     */
    public UtilGPT setEchoPrompt(boolean echoPrompt) {
        this.echoPrompt = echoPrompt;
        completionRequestBuilder.echo(echoPrompt);
        return this;
    }

    /**
     * Logs the token usage every time request() is called.
     * @param numTokens The number of tokens used in this API request.
     */
    private void logTokenUsage(int numTokens) {
        switch(model) {
            case davinci:
            case inDavinci:
                davinciTokenCounter += numTokens;
                break;
            case curie:
            case inCurie:
                curieTokenCounter += numTokens;
                break;
            case babbage:
            case inBabbage:
                babbageTokenCounter += numTokens;
                break;
            case ada:
            case inAda:
                adaTokenCounter += numTokens;
                break;
        }

        log.info(String.format("Total tokens used:%n%s%s%s%s-----------------------------------------%n",
                davinciTokenCounter > 0 ? "Davinci: " + davinciTokenCounter + " token" + (davinciTokenCounter > 1 ? "s\n" : "\n") : "",
                curieTokenCounter > 0 ? "Curie: " + curieTokenCounter + " token" + (curieTokenCounter > 1 ? "s\n" : "\n") : "",
                babbageTokenCounter > 0 ? "Babbage: " + babbageTokenCounter + " token" + (babbageTokenCounter > 1 ? "s\n" : "\n") : "",
                adaTokenCounter > 0 ? "Ada: " + adaTokenCounter + " token" + (adaTokenCounter > 1 ? "s\n" : "\n") : ""));
    }

    /** @param model The model to check.
     *  @return Case-sensitive check if the model is valid. */
    public static boolean isValidModel(String model) {
        return Arrays.asList(davinci, curie, babbage, ada, inDavinci, inCurie, inBabbage, inAda).contains(model);
    }

}
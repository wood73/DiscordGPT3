package wood.util;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A wrapper class for com.theokanning.openai. Requires GPTRequest.apiKey to be set. */
@Slf4j
public class GPTRequest {

    // ----------- static fields -----------

    /** The OpenAI API key to use for all requests. Can set using the testAndSetApiKey method. */
    public static String apiKey = "";

    /** Language models */
    public static final String davinci = "davinci", curie = "curie", babbage = "babbage", ada = "ada",
            inDavinci = "text-davinci-002", inCurie = "text-curie-001", inBabbage = "text-babbage-001", inAda = "text-ada-001";


    /** counter for how many tokens have been used by each language model (irrespective of Base series vs Instruct) */
    private static int davinciTokenCounter = 0, curieTokenCounter = 0, babbageTokenCounter = 0, adaTokenCounter = 0;


    // ----------- instance fields -----------

    private final OpenAiService service;
    private final CompletionRequest completionRequest;
    private final CompletionRequest.CompletionRequestBuilder completionRequestBuilder;

    /** The prompt to use for this API request */
    @Getter private final String prompt;

    /** Language Model to use for this API request */
    @Getter private final String model;

    /** Maximum number of tokens use in the API request (including the prompt). */
    @Getter private final int maxTokens;

    /** (default .7) a value 0-1 with 1 being very creative, 0 being very factual/deterministic */
    @Getter private final double temperature;

    /** (default 1) between 0-1 where 1.0 means "use all tokens in the vocabulary"
     *  while 0.5 means "use only the 50% most common tokens" */
    @Getter private final double topP;

    /** (default 0) 0-1, lowers the chances of a word being selected again the more times that word has already been used */
    @Getter private final double frequencyPenalty;

    /** (default 0) 0-1, lowers the chances of topic repetition */
    @Getter private final double presencePenalty;

    /** Echo back the prompt in addition to the completion. */
    @Getter private final boolean echoPrompt;

    /** (default 1), queries GPT-3 this many times, then selects the 'best' generation to return */
    @Getter private final int bestOf;

    /** The Strings that GPT-3 will stop generating after (can have 4 stop sequences max) */
    @Getter private final List<String> stopSequences;

    public GPTRequest(GPTRequestBuilder builder) {
        this.prompt = builder.prompt;
        this.model = builder.model;
        this.maxTokens = builder.maxTokens;
        this.temperature = builder.temperature;
        this.topP = builder.topP;
        this.frequencyPenalty = builder.frequencyPenalty;
        this.presencePenalty = builder.presencePenalty;
        this.echoPrompt = builder.echoPrompt;
        this.bestOf = builder.bestOf;
        this.stopSequences = builder.stopSequences;

        service = new OpenAiService(apiKey);
        completionRequestBuilder = CompletionRequest.builder()
                .prompt(prompt);

        completionRequestBuilder.maxTokens(maxTokens);
        completionRequestBuilder.temperature(temperature);
        completionRequestBuilder.topP(topP);
        completionRequestBuilder.frequencyPenalty(frequencyPenalty);
        completionRequestBuilder.presencePenalty(presencePenalty);
        completionRequestBuilder.echo(echoPrompt);
        if(stopSequences != null)
            completionRequestBuilder.stop(stopSequences);

        completionRequest = completionRequestBuilder.build();
    }

    /**
     * Tests the API key, and sets it if it's valid
     * API key validity is tested by a 1 token API request to the Ada model.
     * @param apiKey An OpenAI API key
     * @return Whether the API key is valid
     */
    public static boolean testAndSetApiKey(String apiKey) {
        String originalAPIKey = GPTRequest.apiKey;
        try {
            GPTRequest.apiKey = apiKey;
            new GPTRequestBuilder(ada, "", 1, false).build().request();
            return true;
        }catch(Exception e) {
            GPTRequest.apiKey = originalAPIKey;
            return false;
        }
    }

    /**
     * Makes an OpenAI API request.
     * @return If echoPrompt is true, returns the prompt + completion, else the completion is returned.
     */
    public String request() {
        logTokenUsage(maxTokens);
        List<CompletionChoice> outputList = service.createCompletion(model, completionRequest).getChoices();
        return outputList.get(0).getText();
    }

    /**
     * Makes an OpenAI API request.
     * @param endAtLastPunctuationMark Whether the completion should be cut off after the last punctuation mark
     * @return If echoPrompt is true, returns the prompt + completion, else the completion is returned.
     */
    public String request(boolean endAtLastPunctuationMark) {
        String output = request();

        if(endAtLastPunctuationMark) {
            // get the index of the last punctuation mark inside the completion (omitting the prompt)
            Optional<Integer> lastPunctuationIndex = StringUtil.lastIndexOf(output, "[.!?]",
                    echoPrompt ? prompt.length() : 0);

            if(lastPunctuationIndex.isPresent())
                return output.substring(0, lastPunctuationIndex.get() + 1);
        }

        return output;
    }

    public static class GPTRequestBuilder {

        /** Language Model to use for this API request */
        @Getter
        private String model;

        /** The prompt to use for this API request */
        @Getter private String prompt;

        /** Maximum number of tokens use in the API request (including the prompt). */
        @Getter private int maxTokens;

        /** (default false) Echo back the prompt in addition to the completion. */
        @Getter private boolean echoPrompt;

        /** (default .7) a value 0-1 with 1 being very creative, 0 being very factual/deterministic */
        @Getter private double temperature;

        /** (default 1) between 0-1 where 1.0 means "use all tokens in the vocabulary"
         *  while 0.5 means "use only the 50% most common tokens" */
        @Getter private double topP;

        /** (default 0) 0-1, lowers the chances of a word being selected again the more times that word has already been used */
        @Getter private double frequencyPenalty;

        /** (default 0) 0-1, lowers the chances of topic repetition */
        @Getter private double presencePenalty;

        /** (default 1), queries GPT-3 this many times, then selects the 'best' generation to return */
        @Getter private int bestOf;

        /** The Strings that GPT-3 will stop generating after (can have 4 stop sequences max) */
        @Getter private List<String> stopSequences;

        /**
         * Starts to build an API request for the given language model
         *
         * @param model Language model to use for this API request. Valid Base Series models:
         *               UtilGPT.davinci, UtilGPT.curie, UtilGPT.babbage, UtilGPT.ada
         *               Valid Instruct Series models:
         *               UtilGPT.inDavinci, UtilGPT.inCurie, UtilGPT.inBabbage, UtilGPT.inAda
         * @param prompt Prompt sent to the language model
         * @param maxTokens Maximum number of tokens use in the API request
         */
        public GPTRequestBuilder(String model, String prompt, int maxTokens) {
            this.model = model;
            this.prompt = prompt;
            this.maxTokens = maxTokens;
            this.temperature = .7;
            this.topP = 1;
            this.frequencyPenalty = 0;
            this.presencePenalty = 0;
            this.echoPrompt = false;
            this.bestOf = 1;
        }

        /**
         * Starts to build an API request for the given language model
         *
         * @param model Language model to use for this API request. Valid Base Series models:
         *               UtilGPT.davinci, UtilGPT.curie, UtilGPT.babbage, UtilGPT.ada
         *               Valid Instruct Series models:
         *               UtilGPT.inDavinci, UtilGPT.inCurie, UtilGPT.inBabbage, UtilGPT.inAda
         * @param prompt Prompt sent to the language model
         * @param maxTokens Maximum number of tokens use in the API request
         * @param addPromptTokensToMaxTokens Whether the number of tokens in the prompt should be added to maxTokens
         */
        public GPTRequestBuilder(String model, String prompt, int maxTokens, boolean addPromptTokensToMaxTokens) {
            this.model = model;
            this.prompt = prompt;
            this.maxTokens = addPromptTokensToMaxTokens ? maxTokens + GPTUtil.countTokens(prompt) : maxTokens;
            this.temperature = .7;
            this.topP = 1;
            this.frequencyPenalty = 0;
            this.presencePenalty = 0;
            this.echoPrompt = false;
            this.bestOf = 1;
        }

        public GPTRequest build() {
            return new GPTRequest(this);
        }

        /** @param prompt Prompt sent to the language model
         *  @return This GPTRequestBuilder, for chaining */
        public GPTRequestBuilder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        /** @param prompt Prompt sent to the language model
         *  @param maxTokens Maximum number of tokens use in the API request
         *  @param addPromptTokensToMaxTokens Whether the number of tokens in the prompt should be added to maxTokens
         *  @return This GPTRequestBuilder, for chaining */
        public GPTRequestBuilder promptAndTokens(String prompt, int maxTokens, boolean addPromptTokensToMaxTokens) {
            this.prompt = prompt;
            this.maxTokens = addPromptTokensToMaxTokens ? maxTokens + GPTUtil.countTokens(prompt) : maxTokens;
            return this;
        }

        /**
         * @param model Language model to use for this API request. Valid Base Series models:
         *               UtilGPT.davinci, UtilGPT.curie, UtilGPT.babbage, UtilGPT.ada
         *               Valid Instruct Series models:
         *               UtilGPT.inDavinci, UtilGPT.inCurie, UtilGPT.inBabbage, UtilGPT.inAda
         * @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder model(String model) {
            this.model = model;
            return this;
        }

        /**
         * @param maxTokens The total number of tokens use in the API request (including the prompt).
         * @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        /** @param temperature (default .7) a value 0-1 with 1 being very creative, 0 being very factual/deterministic
         *  @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        /** @param topP (default 1) between 0-1 where 1.0 means "use all tokens in the vocabulary"
         *               while 0.5 means "use only the 50% most common tokens"
         *  @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder topP(double topP) {
            this.topP = topP;
            return this;
        }

        /** @param frequencyPenalty (default 0) 0-1, lowers the chances of a word being selected again
         *                           the more times that word has already been used
         *  @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder frequencyPenalty(double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        /** @param presencePenalty (default 0) 0-1, lowers the chances of topic repetition
         *  @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder presencePenalty(double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        /** @param bestOf (default 1), queries GPT-3 this many times, then selects the 'best' generation to return
         *  @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder bestOf(int bestOf) {
            this.bestOf = bestOf;
            return this;
        }

        /**
         * set the stop sequence, the String that GPT-3 will stop generating after
         *     (can have 4 stop sequences max)
         * @param stopSequences The Strings that GPT-3 will stop generating after (can have 4 stop sequences max)
         * @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder stopSequences(List<String> stopSequences) {
            if(stopSequences.size() > 4)
                throw new IllegalArgumentException("Can only have 4 stop sequences max");
            else
                this.stopSequences = stopSequences;
            return this;
        }

        /** @param echoPrompt Whether to echo back the prompt in addition to the completion.
         *  @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder echoPrompt(boolean echoPrompt) {
            this.echoPrompt = echoPrompt;
            return this;
        }

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

}

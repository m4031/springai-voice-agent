package com.voiceagent.configuration;

import com.google.genai.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes a Google GenAI SDK {@link Client} for the audio parts of the pipeline.
 *
 * <p>Spring AI's google-genai starter only provides a {@code ChatModel}; it has no
 * {@code TranscriptionModel} / {@code TextToSpeechModel}. So speech-to-text and
 * text-to-speech talk to the GenAI SDK directly through this client, reusing the same
 * API key as the chat model.
 *
 * <p>The key comes from {@code spring.ai.google.genai.api-key}, which in the gemini
 * profile is wired to the {@code GEMINI_API_KEY} environment variable.
 */
@Configuration
public class GeminiClientConfig {

    private static final Logger log = LoggerFactory.getLogger(GeminiClientConfig.class);

    @Bean
    Client geminiClient(@Value("${spring.ai.google.genai.api-key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("${")) {
            throw new IllegalStateException(
                    "Gemini API key is not set. Resolved spring.ai.google.genai.api-key to '" + apiKey
                            + "'. Set the GEMINI_API_KEY environment variable in your run configuration.");
        }

        // Trim stray whitespace/newlines that often sneak in via env vars and make
        // Google reject an otherwise-valid key with API_KEY_INVALID.
        String key = apiKey.trim();

        log.info("Gemini client initialised with API key (length={}, prefix={}...)",
                key.length(), key.substring(0, Math.min(4, key.length())));

        return Client.builder().apiKey(key).build();
    }
}

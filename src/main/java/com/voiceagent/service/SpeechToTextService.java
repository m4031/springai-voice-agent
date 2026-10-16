package com.voiceagent.service;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Speech-to-text via Gemini's multimodal API (the audio is sent as an inline input
 * part and the model returns the transcription as text). Gemini has no Spring AI
 * {@code TranscriptionModel}, so this calls the Google GenAI SDK directly.
 */
@Service
public class SpeechToTextService {

    private static final String DEFAULT_MIME_TYPE = "audio/wav";

    private static final String PROMPT = """
            Transcribe the following audio to plain text.
            Return only the transcription, with no extra commentary.
            """;

    private final Client client;
    private final String model;

    public SpeechToTextService(@Qualifier("geminiClient") Client client,
                               @Value("${app.gemini.stt.model}") String model) {
        this.client = client;
        this.model = model;
    }

    public String transcribe(Resource audio, String contentType) {
        byte[] bytes;
        try {
            bytes = audio.getContentAsByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read audio for transcription", e);
        }

        String mimeType = StringUtils.hasText(contentType) ? contentType : DEFAULT_MIME_TYPE;

        Content content = Content.fromParts(
                Part.fromText(PROMPT),
                Part.fromBytes(bytes, mimeType)
        );

        GenerateContentResponse response = client.models.generateContent(model, content, null);

        String text = response.text();
        return text == null ? "" : text.trim();
    }
}

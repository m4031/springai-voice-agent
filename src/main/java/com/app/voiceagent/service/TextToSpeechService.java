package com.app.voiceagent.service;

import com.google.genai.Client;
import com.google.genai.types.Blob;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.PrebuiltVoiceConfig;
import com.google.genai.types.SpeechConfig;
import com.google.genai.types.VoiceConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Text-to-speech via Gemini's TTS model. Gemini has no Spring AI
 * {@code TextToSpeechModel}, so this calls the Google GenAI SDK directly, requesting
 * an AUDIO response modality. The model returns raw 16-bit mono PCM, which is wrapped
 * in a WAV container here so the bytes are directly playable.
 */
@Service
public class TextToSpeechService {

    private static final int DEFAULT_SAMPLE_RATE = 24_000;

    private final Client client;
    private final String model;
    private final String voice;

    public TextToSpeechService(@Qualifier("geminiClient") Client client,
                               @Value("${app.gemini.tts.model}") String model,
                               @Value("${app.gemini.tts.voice}") String voice) {
        this.client = client;
        this.model = model;
        this.voice = voice;
    }

    public byte[] generateSpeech(String text) {
        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseModalities("AUDIO")
                .speechConfig(SpeechConfig.builder()
                        .voiceConfig(VoiceConfig.builder()
                                .prebuiltVoiceConfig(PrebuiltVoiceConfig.builder()
                                        .voiceName(voice)
                                        .build())
                                .build())
                        .build())
                .build();

        GenerateContentResponse response = client.models.generateContent(model, text, config);

        Blob audio = extractAudio(response);
        byte[] pcm = audio.data().orElseThrow(
                () -> new IllegalStateException("Gemini TTS response contained no audio bytes"));
        int sampleRate = parseSampleRate(audio.mimeType().orElse(null));

        return pcmToWav(pcm, sampleRate);
    }

    private Blob extractAudio(GenerateContentResponse response) {
        List<Part> parts = response.parts();
        if (parts != null) {
            for (Part part : parts) {
                Optional<Blob> inline = part.inlineData();
                if (inline.isPresent() && inline.get().data().isPresent()) {
                    return inline.get();
                }
            }
        }
        throw new IllegalStateException("Gemini TTS returned no audio data");
    }

    /** Parses the sample rate from a mime type such as {@code audio/L16;codec=pcm;rate=24000}. */
    private int parseSampleRate(String mimeType) {
        if (mimeType != null) {
            for (String segment : mimeType.split(";")) {
                String trimmed = segment.trim();
                if (trimmed.startsWith("rate=")) {
                    try {
                        return Integer.parseInt(trimmed.substring("rate=".length()).trim());
                    } catch (NumberFormatException ignored) {
                        // fall through to default
                    }
                }
            }
        }
        return DEFAULT_SAMPLE_RATE;
    }

    /** Wraps raw 16-bit mono PCM in a minimal 44-byte WAV (RIFF) header. */
    private byte[] pcmToWav(byte[] pcm, int sampleRate) {
        int channels = 1;
        int bitsPerSample = 16;
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataLen = pcm.length;

        ByteBuffer buffer = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(36 + dataLen);
        buffer.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        buffer.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(16);                   // PCM header size
        buffer.putShort((short) 1);          // audio format = PCM
        buffer.putShort((short) channels);
        buffer.putInt(sampleRate);
        buffer.putInt(byteRate);
        buffer.putShort((short) blockAlign);
        buffer.putShort((short) bitsPerSample);
        buffer.put("data".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(dataLen);
        buffer.put(pcm);
        return buffer.array();
    }
}

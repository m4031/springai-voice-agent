package com.springai.voiceagent.service;

import com.springai.voiceagent.dto.VoiceResponse;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class VoiceAgentService {

    private final SpeechToTextService speechToTextService;
    private final OrderAgentService orderAgentService;
    private final TextToSpeechService textToSpeechService;

    public VoiceAgentService( SpeechToTextService speechToTextService,
                              OrderAgentService orderAgentService,
                              TextToSpeechService textToSpeechService) {
        this.speechToTextService = speechToTextService;
        this.orderAgentService = orderAgentService;
        this.textToSpeechService = textToSpeechService;
    }

    public VoiceResponse process(Resource audio, String contentType) {
        String transcript = speechToTextService.transcribe(audio, contentType);

        String response = orderAgentService.ask(transcript);

        byte[] audioResponse = textToSpeechService.generateSpeech(response);

        String base64Audio = Base64.getEncoder() .encodeToString(audioResponse);

        return new VoiceResponse( transcript, response, base64Audio );
    }
}

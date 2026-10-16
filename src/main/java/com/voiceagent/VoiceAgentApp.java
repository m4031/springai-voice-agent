package com.voiceagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VoiceAgentApp {

	public static void main(String[] args) {
		String apiKey = System.getenv("GEMINI_API_KEY");
//		System.out.println("apiKey=>" + apiKey);

		SpringApplication.run(VoiceAgentApp.class, args);
	}

}

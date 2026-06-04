package com.ragdemo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

/**
 * 语音服务 - 阿里云百炼
 *
 * - ASR: fun-asr (语音转文字)
 * - TTS: cosyvoice-v3.5-plus (文字转语音)
 */
@Service
public class SpeechService {

    private static final Logger log = LoggerFactory.getLogger(SpeechService.class);

    private final String apiKey;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public SpeechService(@Value("${speech.api.key:${QWEN_API_KEY}}") String apiKey) {
        this.apiKey = apiKey;
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    /**
     * 语音转文字 - 使用阿里云 FunASR
     * OGG格式语音 → 文字
     */
    public String speechToText(byte[] audioData, String filename) throws IOException, InterruptedException {
        log.info("Transcribing audio: {} ({} bytes)", filename, audioData.length);

        String base64Audio = Base64.getEncoder().encodeToString(audioData);

        String payload = """
        {
            "model": "fun-asr",
            "input": {
                "audio_base64": "%s"
            }
        }
        """.formatted(base64Audio);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/compatible-mode/v1/audio/transcriptions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("FunASR API error: {} - {}", response.statusCode(), response.body());
            return null;
        }

        JsonNode root = mapper.readTree(response.body());
        String text = root.path("text").asText();
        if (text == null || text.isBlank()) {
            text = root.path("output").path("text").asText(null);
        }
        if (text == null || text.isBlank()) {
            text = root.path("result").path("transcription").asText(null);
        }
        log.info("Transcription result: {}", text != null ? text.substring(0, Math.min(50, text.length())) : "null");
        return text;
    }

    /**
     * 文字转语音 - 使用阿里云 CosyVoice
     * 文字 → OGG音频
     */
    public byte[] textToSpeech(String text, String voice) throws IOException, InterruptedException {
        if (voice == null || voice.isBlank()) {
            voice = "longxiaochun"; // 龙小春（女声，自然中文）
        }

        log.info("Generating TTS ({} chars) with voice: {}", text.length(), voice);

        String payload = """
        {
            "model": "cosyvoice-v3.5-plus",
            "input": {
                "text": "%s"
            },
            "voice": "%s",
            "format": "ogg_opus"
        }
        """.formatted(text.replace("\"", "\\\"").replace("\n", " ").trim(), voice);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/compatible-mode/v1/audio/speech"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        // 注意：TTS 返回的是二进制音频，不是 JSON
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            String errBody = new String(response.body());
            log.error("CosyVoice API error: {} - {}", response.statusCode(), errBody);
            return null;
        }

        log.info("TTS generated: {} bytes", response.body().length);
        return response.body();
    }

    public byte[] textToSpeech(String text) throws IOException, InterruptedException {
        return textToSpeech(text, null);
    }
}

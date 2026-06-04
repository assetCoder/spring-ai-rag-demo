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
 * 多模态服务 - 图片理解（通过 OpenAI 兼容 API）
 */
@Service
public class VisionService {

    private static final Logger log = LoggerFactory.getLogger(VisionService.class);

    private final String apiKey;
    private final String visionModel;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public VisionService(@Value("${vision.api.key:${DEEPSEEK_API_KEY}}") String apiKey,
                         @Value("${vision.api.url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}") String apiUrl,
                         @Value("${vision.model:qwen-vl-plus}") String visionModel) {
        this.apiKey = apiKey;
        this.visionModel = visionModel;
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        log.info("Vision service initialized: model={}, baseUrl={}", visionModel, apiUrl);
    }

    public String getApiUrl() {
        return "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    }

    /**
     * 用图片URL分析图片
     */
    public String analyzeImageUrl(String imageUrl, String prompt) throws IOException, InterruptedException {
        String body = buildPayload(imageUrl, prompt, "url");
        return callVisionApi(body);
    }

    /**
     * 用Base64图片数据分析图片
     */
    public String analyzeImageBase64(byte[] imageBytes, String prompt) throws IOException, InterruptedException {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String dataUri = "data:image/jpeg;base64," + base64;
        String body = buildPayload(dataUri, prompt, "base64");
        return callVisionApi(body);
    }

    private String buildPayload(String imageData, String prompt, String type) {
        String imageContent;
        if ("url".equals(type)) {
            imageContent = """
                {"type": "image_url", "image_url": {"url": "%s"}}
            """.formatted(imageData.replace("\"", "\\\""));
        } else {
            imageContent = """
                {"type": "image_url", "image_url": {"url": "%s"}}
            """.formatted(imageData);
        }

        return """
        {
            "model": "%s",
            "messages": [
                {
                    "role": "user",
                    "content": [
                        %s,
                        {"type": "text", "text": "%s"}
                    ]
                }
            ],
            "max_tokens": 1000
        }
        """.formatted(visionModel, imageContent, prompt.replace("\"", "\\\"").replace("\n", "\\n"));
    }

    private String callVisionApi(String payload) throws IOException, InterruptedException {
        log.info("Calling vision API with model: {}", visionModel);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getApiUrl()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Vision API error: {} - {}", response.statusCode(), response.body());
            return "图片分析失败（API返回 " + response.statusCode() + "）";
        }

        JsonNode root = mapper.readTree(response.body());
        String content = root.path("choices").get(0).path("message").path("content").asText();
        log.info("Vision API response received ({} chars)", content.length());
        return content;
    }
}

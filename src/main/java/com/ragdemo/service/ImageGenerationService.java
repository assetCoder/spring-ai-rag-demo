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
 * 图片生成服务 - 使用通义万相/千问 Image 模型
 */
@Service
public class ImageGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationService.class);

    private final String apiKey;
    private final String model;
    private final String apiUrl;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public ImageGenerationService(
            @Value("${image.api.key:${QWEN_API_KEY}}") String apiKey,
            @Value("${image.model:qwen-image-2.0-pro}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/images/generations";
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    /**
     * 文生图，返回图片字节数组
     */
    public byte[] generateImage(String prompt) throws IOException, InterruptedException {
        log.info("Generating image with model={}, prompt={}", model, prompt);

        String payload = """
        {
            "model": "%s",
            "prompt": "%s",
            "n": 1,
            "size": "1024x1024"
        }
        """.formatted(model, prompt.replace("\"", "\\\"").replace("\n", " "));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Image generation API error: {} - {}", response.statusCode(), response.body());
            return null;
        }

        JsonNode root = mapper.readTree(response.body());

        // 阿里云百炼的返回格式：data[0].b64_json 或 data[0].url
        JsonNode data = root.path("data").get(0);

        // 先尝试 b64_json
        String b64Json = data.path("b64_json").asText(null);
        if (b64Json != null && !b64Json.isBlank()) {
            log.info("Image generated as base64 ({} chars)", b64Json.length());
            return Base64.getDecoder().decode(b64Json);
        }

        // 否则尝试 url
        String imageUrl = data.path("url").asText(null);
        if (imageUrl != null && !imageUrl.isBlank()) {
            log.info("Image generated, downloading from URL");
            var dlReq = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .GET()
                    .build();
            var dlResp = client.send(dlReq, HttpResponse.BodyHandlers.ofByteArray());
            return dlResp.body();
        }

        log.error("No image data in response: {}", response.body());
        return null;
    }
}

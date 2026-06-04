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
import java.time.Duration;

/**
 * 图片生成服务 - 阿里云百炼 Qwen-Image 2.0 Pro
 * <p>
 * 使用同步接口（Multimodal Generation）：
 * POST https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation
 */
@Service
public class ImageGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationService.class);

    private final String apiKey;
    private final String model;
    private final ObjectMapper mapper;
    private final HttpClient client;

    public ImageGenerationService(
            @Value("${image.api.key:${QWEN_API_KEY}}") String apiKey,
            @Value("${image.model:qwen-image-2.0-pro}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.mapper = new ObjectMapper();
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * 文生图，返回图片字节数组
     */
    public byte[] generateImage(String prompt) throws IOException, InterruptedException {
        log.info("Generating image with model={}, prompt={}", model, prompt);

        String payload = """
        {
            "model": "%s",
            "input": {
                "messages": [
                    {
                        "role": "user",
                        "content": [
                            {"text": "%s"}
                        ]
                    }
                ]
            },
            "parameters": {
                "size": "1024*1024",
                "n": 1,
                "prompt_extend": true,
                "watermark": false
            }
        }
        """.formatted(model,
                prompt.replace("\"", "\\\"")
                        .replace("\n", " ")
                        .replace("\\", "\\\\"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Image generation API error: {} - {}", response.statusCode(), response.body());
            return null;
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode choices = root.path("output").path("choices");

        if (choices.isArray() && choices.size() > 0) {
            JsonNode content = choices.get(0).path("message").path("content");
            if (content.isArray() && content.size() > 0) {
                String imageUrl = content.get(0).path("image").asText(null);
                if (imageUrl != null && !imageUrl.isBlank()) {
                    log.info("Image generated, downloading from URL");
                    return downloadImage(imageUrl);
                }
            }
        }

        log.error("No image URL in response: {}", response.body());
        return null;
    }

    private byte[] downloadImage(String url) throws IOException, InterruptedException {
        HttpRequest dlReq = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<byte[]> dlResp = client.send(dlReq, HttpResponse.BodyHandlers.ofByteArray());
        log.info("Downloaded image: {} bytes", dlResp.body().length);
        return dlResp.body();
    }
}

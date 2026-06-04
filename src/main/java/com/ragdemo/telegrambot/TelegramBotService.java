package com.ragdemo.telegrambot;

import com.ragdemo.service.ImageGenerationService;
import com.ragdemo.service.OrchestratorService;
import com.ragdemo.service.VisionService;
import com.ragdemo.service.SpeechService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class TelegramBotService implements LongPollingSingleThreadUpdateConsumer, SpringLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);

    // 图片生成触发关键词
    private static final java.util.regex.Pattern DRAW_PATTERN =
            java.util.regex.Pattern.compile(
                    "(画|生成|创建|绘制|创作|设计|画一张|生成一张)" +
                    ".*(图片|图像|图|画|照片|插画|海报|logo|图标)", 
                    java.util.regex.Pattern.CASE_INSENSITIVE);

    private final OrchestratorService orchestrator;
    private final VisionService visionService;
    private final SpeechService speechService;
    private final ImageGenerationService imageGenService;
    private final String botToken;
    private final String botFileUrl;
    private TelegramClient client;
    private final HttpClient httpClient;

    // 用于多模态结果注入记忆，注意这不是线程安全的
    // 但在单线程 long polling + 顺序处理的模式下够用

    public TelegramBotService(OrchestratorService orchestrator,
                              VisionService visionService,
                              SpeechService speechService,
                              ImageGenerationService imageGenService,
                              @Value("${telegram.bot.token}") String botToken) {
        this.orchestrator = orchestrator;
        this.visionService = visionService;
        this.speechService = speechService;
        this.imageGenService = imageGenService;
        this.botToken = botToken;
        this.botFileUrl = "https://api.telegram.org/bot" + botToken;
        this.httpClient = HttpClient.newHttpClient();
    }

    @PostConstruct
    public void init() {
        this.client = new OkHttpTelegramClient(botToken);
        log.info("Telegram bot initialized");
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage()) {
            return;
        }

        var message = update.getMessage();
        var chatId = message.getChatId().toString();
        var msgId = message.getMessageId();

        // 1. 图片消息
        if (message.hasPhoto()) {
            log.info("Received photo from {}", chatId);
            new Thread(() -> handlePhoto(chatId, message.getPhoto(), message.getCaption())).start();
            return;
        }

        // 2. 语音消息
        if (message.hasVoice()) {
            log.info("Received voice from {}", chatId);
            new Thread(() -> handleVoice(chatId, message.getVoice())).start();
            return;
        }

        // 3. 文字消息
        if (message.hasText()) {
            var text = message.getText();
            log.info("Received text from {}: {}", chatId, text);
            new Thread(() -> handleText(chatId, text)).start();
        }
    }

    // ======== 文字消息处理 ========

    private void handleText(String chatId, String text) {
        try {
            // 检查是否是图片生成请求
            if (DRAW_PATTERN.matcher(text).find()) {
                handleImageGeneration(chatId, text);
                return;
            }

            sendTyping(chatId);

            String reply = callOrchestratorWithTimeout(text);

            sendText(chatId, reply);
            log.info("Sent text reply to {}", chatId);

        } catch (Exception e) {
            log.error("Error handling text from {}", chatId, e);
            sendText(chatId, "抱歉，处理消息时出错了：" + e.getMessage());
        }
    }

    /** 图片生成处理 */
    private void handleImageGeneration(String chatId, String prompt) {
        try {
            sendText(chatId, "🎨 正在生成图片，请稍候...");

            byte[] imageBytes = imageGenService.generateImage(prompt);

            if (imageBytes == null || imageBytes.length == 0) {
                sendText(chatId, "图片生成失败，请检查 API 配置或稍后再试。");
                return;
            }

            // 发送图片
            InputFile photo = new InputFile()
                    .setMedia(new java.io.ByteArrayInputStream(imageBytes), "generated.jpg");
            client.execute(org.telegram.telegrambots.meta.api.methods.send.SendPhoto.builder()
                    .chatId(chatId)
                    .photo(photo)
                    .caption("🎨 " + prompt)
                    .build());
            log.info("Sent generated image to {} ({} bytes)", chatId, imageBytes.length);

        } catch (Exception e) {
            log.error("Error generating image", e);
            sendText(chatId, "生成图片时出错了：" + e.getMessage());
        }
    }

    // ======== 图片消息处理 ========

    private void handlePhoto(String chatId, List<PhotoSize> photos, String caption) {
        try {
            sendTyping(chatId);

            // 取最大尺寸的图片
            PhotoSize largest = photos.stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElseThrow(() -> new RuntimeException("图片为空"));

            // 下载图片
            byte[] imageBytes = downloadFile(largest.getFileId());
            log.info("Downloaded photo: {} bytes", imageBytes.length);

            // 用 Vision API 分析
            String userPrompt = caption != null && !caption.isBlank() ? caption : "请详细描述这张图片的内容";
            String visionResult = visionService.analyzeImageBase64(imageBytes, userPrompt);

            // 注入记忆：把"看到图片"这件事记入对话历史
            injectImageMemory(userPrompt, visionResult);

            // 用 DeepSeek 基于上下文生成回复（如果用户还写了文字，结合上下文回复）
            String textToSend = "🔍 **图片分析结果：**\n\n" + visionResult;
            if (caption != null && !caption.isBlank()) {
                // 用户附带了文字说明 → 结合上下文，让 DeepSeek 生成更自然的回复
                sendTyping(chatId);
                String contextualReply = callOrchestratorWithTimeout(
                        "（我刚刚发了一张图片）" + caption +
                        "\n\n图片分析结果：\n" + visionResult);
                textToSend = contextualReply;
            }

            sendText(chatId, textToSend);
            log.info("Sent vision + contextual reply to {}", chatId);

        } catch (Exception e) {
            log.error("Error handling photo from {}", chatId, e);
            sendText(chatId, "抱歉，分析图片时出错了：" + e.getMessage());
        }
    }

    /**
     * 把图片分析结果注入对话记忆
     */
    private void injectImageMemory(String userText, String visionResult) {
        orchestrator.injectMemory(
                "[用户上传了一张图片]" + (userText != null ? " 用户说: " + userText : ""),
                "[AI分析了图片] " + visionResult
        );
    }

    // ======== 语音消息处理 ========

    private void handleVoice(String chatId, Voice voice) {
        try {
            sendTyping(chatId);

            // 下载语音文件（OGG格式）
            byte[] audioData = downloadFile(voice.getFileId());
            log.info("Downloaded voice: {} bytes, duration: {}s", audioData.length, voice.getDuration());

            // 1. 语音转文字
            sendText(chatId, "🎤 正在识别语音...");
            String transcript = speechService.speechToText(audioData, "voice.ogg");

            if (transcript == null || transcript.isBlank()) {
                sendText(chatId, "抱歉，无法识别这段语音。");
                return;
            }

            // 2. 注入记忆并让 DeepSeek 基于上下文回复
            orchestrator.injectMemory(
                    "[用户发送了语音消息] 识别结果: " + transcript,
                    null  // AI还没回复，回复由下面的 callOrchestratorWithTimeout 注入
            );

            String reply = callOrchestratorWithTimeout(transcript);

            // 3. 文字转语音回复
            sendTyping(chatId);
            byte[] ttsAudio = speechService.textToSpeech(reply);

            if (ttsAudio != null) {
                // 发送语音消息
                InputFile voiceFile = new InputFile()
                        .setMedia(new java.io.ByteArrayInputStream(ttsAudio), "reply.ogg");
                client.execute(SendVoice.builder()
                        .chatId(chatId)
                        .voice(voiceFile)
                        .caption("📝 你说: " + transcript)
                        .build());
                log.info("Sent voice reply to {}", chatId);
            } else {
                // TTS 失败，发文字
                sendText(chatId, "📝 你说: " + transcript + "\n\n" + reply);
            }

        } catch (Exception e) {
            log.error("Error handling voice from {}", chatId, e);
            sendText(chatId, "抱歉，处理语音时出错了：" + e.getMessage());
        }
    }

    // ======== 工具方法 ========

    private String callOrchestratorWithTimeout(String text) throws Exception {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                var result = orchestrator.process(text);
                return result.reply();
            } catch (Exception e) {
                log.error("Orchestrator error", e);
                throw new RuntimeException(e);
            }
        });

        try {
            return future.get(90, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            return "处理超时，DeepSeek API 响应较慢，请稍后再试。";
        }
    }

    /** 从 Telegram 下载文件 */
    private byte[] downloadFile(String fileId) throws Exception {
        // 获取文件路径
        String fileInfoUrl = botFileUrl + "/getFile?file_id=" + fileId;
        var fileInfoReq = HttpRequest.newBuilder()
                .uri(URI.create(fileInfoUrl))
                .GET()
                .build();
        var fileInfoResp = httpClient.send(fileInfoReq, HttpResponse.BodyHandlers.ofString());

        // 解析 file_path
        var root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(fileInfoResp.body());
        String filePath = root.path("result").path("file_path").asText();

        // 下载文件
        String downloadUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;
        var downloadReq = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .GET()
                .build();
        var downloadResp = httpClient.send(downloadReq, HttpResponse.BodyHandlers.ofByteArray());
        return downloadResp.body();
    }

    private void sendText(String chatId, String text) {
        try {
            client.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .parseMode("Markdown")
                    .build());
        } catch (Exception e) {
            // 如果 Markdown 解析失败，用纯文本重试
            try {
                client.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text(text)
                        .build());
            } catch (Exception ex) {
                log.error("Failed to send message", ex);
            }
        }
    }

    private void sendTyping(String chatId) {
        try {
            client.execute(SendChatAction.builder()
                    .chatId(chatId)
                    .action("typing")
                    .build());
        } catch (Exception e) {
            // 忽略
        }
    }

    @AfterBotRegistration
    public void onRegistrationSuccess() {
        log.info("Telegram bot registered and ready to receive messages (multimodal mode)");
    }
}

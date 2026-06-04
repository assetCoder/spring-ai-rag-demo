package com.ragdemo.telegrambot;

import com.ragdemo.service.OrchestratorService;
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
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class TelegramBotService implements LongPollingSingleThreadUpdateConsumer, SpringLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);

    private final OrchestratorService orchestrator;
    private final String botToken;
    private TelegramClient client;

    public TelegramBotService(OrchestratorService orchestrator,
                              @Value("${telegram.bot.token}") String botToken) {
        this.orchestrator = orchestrator;
        this.botToken = botToken;
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
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        var message = update.getMessage();
        var chatId = message.getChatId().toString();
        var text = message.getText();

        log.info("Received message from {}: {}", chatId, text);

        // 异步处理，避免阻塞更新轮询
        new Thread(() -> handleMessage(chatId, text)).start();
    }

    private void handleMessage(String chatId, String text) {
        try {
            // 先通知用户正在处理
            sendTyping(chatId);

            // 异步处理，带超时
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return orchestrator.process(text);
                } catch (Exception e) {
                    log.error("Orchestrator process error for message: {}", text, e);
                    throw new RuntimeException(e);
                }
            });

            String reply;
            try {
                reply = future.get(60, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                log.error("Processing timed out for message: {}", text);
                reply = "处理超时，请稍后再试。DeepSeek API 响应较慢，已加长等待时间。";
            }

            SendMessage response = SendMessage.builder()
                    .chatId(chatId)
                    .text(reply)
                    .build();

            client.execute(response);
            log.info("Sent reply to {}: {}...", chatId, reply.substring(0, Math.min(50, reply.length())));

        } catch (Exception e) {
            log.error("Error handling message from {}: {}", chatId, text, e);
            try {
                SendMessage errorMsg = SendMessage.builder()
                        .chatId(chatId)
                        .text("抱歉，处理消息时出错了。错误：" + e.getMessage())
                        .build();
                client.execute(errorMsg);
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
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
            // 忽略发送typing状态的错误
        }
    }

    @AfterBotRegistration
    public void onRegistrationSuccess() {
        log.info("Telegram bot registered and ready to receive messages");
    }
}

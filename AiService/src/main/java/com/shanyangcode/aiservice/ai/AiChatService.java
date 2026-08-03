package com.shanyangcode.aiservice.ai;

import com.shanyangcode.aiservice.tool.EmailTool;
import com.shanyangcode.aiservice.tool.RagTool;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class AiChatService {
    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private ContentRetriever contentRetriever;

    @Resource
    private McpToolProvider mcpToolProvider;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private RagTool ragTool;

    @Resource
    private EmailTool emailTool;

    private final String baseSystemPrompt;

    public AiChatService() {
        try (var in = getClass().getClassLoader().getResourceAsStream("system-prompt/chat-bot.txt")) {
            this.baseSystemPrompt = in != null ? new String(in.readAllBytes(), StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            throw new RuntimeException("无法加载系统提示词", e);
        }
    }

    @Bean
    public AiChat aichat() {
        return AiServices.builder(AiChat.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .systemMessageProvider(memoryId -> {
                    String now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
                            .format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE HH:mm"));
                    return baseSystemPrompt + "\n\n【系统信息】当前北京时间为: " + now + "，请以此时间为准回答用户关于日期时间的问题。";
                })
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory
                        .builder()
                        .id(memoryId)
                        .chatMemoryStore(redisChatMemoryStore)
                        .maxMessages(20)
                        .build())
                .contentRetriever(contentRetriever)
                .toolProvider(mcpToolProvider)
                .tools(ragTool, emailTool)
                .build();
    }
}

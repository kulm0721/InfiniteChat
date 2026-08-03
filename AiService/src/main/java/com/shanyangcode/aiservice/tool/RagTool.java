package com.shanyangcode.aiservice.tool;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Component
@Slf4j
public class RagTool {
    @Resource
    private EmbeddingStoreIngestor embeddingStoreIngestor;

    @Value("${rag.docs-path}")
    private String docsPath;

    @Tool("当用户想要保存问答对、知识点或者向知识库添加新信息时调用此工具。将问题、答案和目标文件名作为参数。")
    public String addKnowledgeToRag(String question, String answer, String fileName) {
        log.info("Tool 调用: 正在保存知识 - Q: {}, file: {}", question, fileName);
        String formattedContent = String.format("### Q: %s\n\nA: %s", question, answer);

        //处理文件名
        if (fileName == null || fileName.isBlank()) {
            fileName = "InfiniteChat.md";
        }
        if (!fileName.endsWith(".md")) {
            fileName = fileName + ".md";
        }

        //写入物理文件
        boolean writeSuccess = appendToFile(formattedContent, fileName);
        if (!writeSuccess) {
            return "保存失败，无法写入本地文件系统，请检查日志";
        }

        //存入向量数据库
        try {
            Metadata metadata = Metadata.from("file_name", fileName);

            Document document = Document.from(formattedContent, metadata);
            embeddingStoreIngestor.ingest(document);

            log.info("Tool 执行成功: 知识已同步至 RAG");
            return "成功！已将该知识点保存到文档 [" + fileName + "] 并同步至向量数据库。";
        } catch (Exception e) {
            log.error("RAG - 向量化失败", e);
            return "文件写入成功，但向量数据库更新失败：" + e.getMessage();
        }
    }

    private synchronized boolean appendToFile(String content, String fileName) {
        try {
            Path filePath = Paths.get(docsPath, fileName);

            if (!Files.exists(filePath)) {
                if (filePath.getParent() != null) {
                    Files.createDirectories(filePath.getParent());
                }
                Files.createFile(filePath);
                log.info("Tool created new file: {}", filePath.toAbsolutePath());
            }
            String textToAppend = "\n\n" + content;
            Files.writeString(filePath, textToAppend, StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            log.error("RAG Tool - 写入文件失败: {}", e.getMessage(), e);
            return false;
        }
    }
}

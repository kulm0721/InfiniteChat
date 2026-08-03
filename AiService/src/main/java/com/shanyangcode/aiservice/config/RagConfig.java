package com.shanyangcode.aiservice.config;

import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SuppressWarnings({"all"})
public class RagConfig {
    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Bean
    public EmbeddingStoreIngestor embeddingStoreIngestor() {
        DocumentByParagraphSplitter paragraphSplitter = new DocumentByParagraphSplitter(300, 100);
        return EmbeddingStoreIngestor.builder()
                .textSegmentTransformer(textSegment -> {
                    String fileName = textSegment.metadata().getString("file_name");
                    String prefix = fileName != null ? fileName : "Unknown-Source";
                    return TextSegment.from(prefix + "\n" + textSegment.text(), textSegment.metadata());
                })
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }

    @Bean
    public ContentRetriever contentRetriever() {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(5)
                .minScore(0.75)
                .build();
    }


}

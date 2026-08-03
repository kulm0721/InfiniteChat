package com.shanyangcode.aiservice.config;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class McpToolConfig {

    @Value("${bigmodel.api-key}")
    private String apiKey;

    @Bean
    public McpToolProvider mcpToolProvider() {

        McpTransport searchTransport = new HttpMcpTransport.Builder()
                .sseUrl("https://open.bigmodel.cn/api/mcp/web_search/sse?Authorization=" + apiKey.trim()) // 去掉空格并 trim
                .build();

        McpClient searchClient = new DefaultMcpClient.Builder()
                .key("BigModelSearchMcpClient")
                .transport(searchTransport)
                .build();

        return McpToolProvider.builder()
                .mcpClients(Arrays.asList(searchClient))
                .build();
    }
}

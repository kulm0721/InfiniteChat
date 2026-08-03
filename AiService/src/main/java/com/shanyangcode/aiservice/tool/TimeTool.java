package com.shanyangcode.aiservice.tool;

import dev.langchain4j.agent.tool.Tool;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeTool {
    @Tool("获取当前北京/上海时间，返回格式为 yyyy-MM-dd HH:mm:ss")
    public String getCurrentTime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        return "北京时间: " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEEE"));
    }
}

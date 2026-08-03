package com.shanyangcode.aiservice.tool;

import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailTool {

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Tool("向特定用户发送电子邮件。")
    public String sendEmail(String targetEmail, String subject, String content) {
        try {
            log.info("Tool 调用: 正在发送邮件 -> To: {}, Subject: {}", targetEmail, subject);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(targetEmail);
            message.setSubject(subject);
            message.setText(content);
            message.setFrom(fromEmail);

            mailSender.send(message);

            log.info("邮件发送成功");
            return "邮件已成功发送给 " + targetEmail;
        } catch (Exception e) {
            log.error("邮件发送失败", e);
            return "邮件发送失败" + e.getMessage();
        }
    }
}

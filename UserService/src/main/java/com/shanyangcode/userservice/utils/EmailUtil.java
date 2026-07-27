package com.shanyangcode.userservice.utils;

import com.shanyangcode.userservice.constant.UserConstant;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class EmailUtil {

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendEmail(String targetEmail, String randomCode) {
        try {
            log.info("正在发送邮件 -> To: {}, Subject: {}", targetEmail, UserConstant.EMAIL_SUBJECT);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(targetEmail);
            message.setSubject(UserConstant.EMAIL_SUBJECT);
            message.setText("您的验证码为:" + randomCode + "(五分钟内有效)");
            mailSender.send(message);
            log.info("邮件发送成功");
        } catch (Exception e) {
            log.error("邮件发送失败", e);
        }
    }
}
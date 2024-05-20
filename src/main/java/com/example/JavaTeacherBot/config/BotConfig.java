package com.example.JavaTeacherBot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
@Configuration
@Data
@PropertySource("application.properties")
public class BotConfig {

    @Value("${bot.token}")
    String botToken;
    @Value("${bot.name}")
    String botName;

}

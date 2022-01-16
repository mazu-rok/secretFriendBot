package com.mazurok.secretFriend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
public class SecretFriendBot extends TelegramLongPollingBot {
    private final String botUsername;
    private final String token;

    @Autowired
    public SecretFriendBot(@Value("${secret-friend.bot.name}") String botUsername,
                           @Value("${secret-friend.bot.token}") String token) {
        this.botUsername = botUsername;
        this.token = token;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // TODO: bot logic
    }
}

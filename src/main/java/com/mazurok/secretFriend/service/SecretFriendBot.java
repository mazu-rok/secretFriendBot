package com.mazurok.secretFriend.service;

import com.mazurok.secretFriend.repository.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class SecretFriendBot extends TelegramLongPollingBot {
    private final String botUsername;
    private final String token;

    private final UserService userService;

    @Autowired
    public SecretFriendBot(@Value("${secret-friend.bot.name}") String botUsername,
                           @Value("${secret-friend.bot.token}") String token,
                           UserService userService) {
        this.botUsername = botUsername;
        this.token = token;

        this.userService = userService;
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
        UserEntity user = userService.getUserByUpdate(update);

        Object message = switch (user.getStage()) {
            case CHANGING_AGE, CHANGING_CITY, CHANGING_GENDER -> userService.userConfig(user, update);
            default -> null;
        };
        sendMessage(message);
    }

    private void sendMessage(Object message) {
        try {
            if (message instanceof SendMessage msg) {
                execute(msg);
            } else if (message instanceof AnswerCallbackQuery answer) {
                execute(answer);
            }
        } catch (TelegramApiException e) {
            log.error("Exception: {}", e.toString());
        }
    }
}

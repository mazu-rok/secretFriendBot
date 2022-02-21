package com.mazurok.secretFriend.service;

import com.mazurok.secretFriend.exceptions.IllegalInputException;
import com.mazurok.secretFriend.repository.UserRepository;
import com.mazurok.secretFriend.repository.entity.Language;
import com.mazurok.secretFriend.repository.entity.StagePart;
import com.mazurok.secretFriend.repository.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Optional;

import static com.mazurok.secretFriend.repository.entity.Stage.*;

@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity getUserByUpdate(Update update) throws IllegalInputException {
        if (!update.hasCallbackQuery() && !update.hasMessage() && !update.hasMyChatMember()) {
            throw new IllegalInputException("Message is not correct %s".formatted(update));
        }
        User user;
        Long chatId;
        if (update.hasMyChatMember()) {
            if (update.getMyChatMember().getNewChatMember().getStatus().equals("kicked")) {
                userRepository.deleteByChatId(update.getMyChatMember().getChat().getId());
                return null;
            } else if (update.getMyChatMember().getNewChatMember().getStatus().equals("member")) {
                user = update.getMyChatMember().getFrom();
                chatId = update.getMyChatMember().getChat().getId();
            } else {
                throw new IllegalInputException("MyChatMember is not correct %s".formatted(update.getMyChatMember()));
            }
        } else {
            user = update.hasCallbackQuery() ? update.getCallbackQuery().getFrom() : update.getMessage().getFrom();
            chatId = update.hasCallbackQuery() ? update.getCallbackQuery().getMessage().getChatId() : update.getMessage().getChatId();
        }
        // just for testing
        if (chatId == -569680531) {
            user.setId(1234L);
        }
        Optional<UserEntity> userEntityOptional = userRepository.findById(user.getId());

        return userEntityOptional.orElseGet(() -> createUserEntity(user, chatId));
    }

    public UserEntity save(UserEntity user) {
        return userRepository.save(user);
    }

    private UserEntity createUserEntity(User user, Long chatId) {
        UserEntity userEntity = UserEntity.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userName(user.getUserName())
                .chatId(chatId)
                .language(Language.valueOf(user.getLanguageCode()))
                .build();

        userEntity.getStages().add(Pair.of(NO_STAGE, StagePart.NO_ACTION));
        userEntity.getStages().add(Pair.of(CONFIGURE_FULL_PROFILE, StagePart.ASK_AGE));
        userRepository.save(userEntity);
        return userEntity;
    }
}

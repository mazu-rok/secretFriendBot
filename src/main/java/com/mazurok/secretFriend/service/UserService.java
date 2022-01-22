package com.mazurok.secretFriend.service;

import com.mazurok.secretFriend.repository.UserRepository;
import com.mazurok.secretFriend.repository.entity.Gender;
import com.mazurok.secretFriend.repository.entity.Stage;
import com.mazurok.secretFriend.repository.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

import static com.mazurok.secretFriend.repository.entity.Stage.*;

@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;
    private final MessageService messageService;

    @Autowired
    public UserService(UserRepository userRepository,
                       MessageService messageService) {
        this.userRepository = userRepository;
        this.messageService = messageService;
    }

    public UserEntity getUserByUpdate(Update update) {
        Long userId = update.hasCallbackQuery() ? update.getCallbackQuery().getFrom().getId() : update.getMessage().getFrom().getId();
        Optional<UserEntity> user = userRepository.findById(userId);

        return user.orElseGet(() -> createUserEntity(update));
    }

    private UserEntity createUserEntity(Update update) {
        UserEntity user = UserEntity.builder()
                .id(update.getMessage().getFrom().getId())
                .firstName(update.getMessage().getFrom().getFirstName())
                .lastName(update.getMessage().getFrom().getLastName())
                .userName(update.getMessage().getFrom().getUserName())
                .chatId(update.getMessage().getChatId())
                .stageState(false)
                .stage(Stage.CHANGING_AGE)
                .build();
        userRepository.save(user);
        return user;
    }

    public Object userConfig(UserEntity user, Update update) {
        Object resultMessage = null;

        if (user.getStage() == CHANGING_AGE) {
            if (user.getStageState()) {
                try {
                    Integer age = Integer.valueOf(update.getMessage().getText());
                    user.setAge(age);
                    user.setStage(CHANGING_CITY);
                    user.setStageState(false);
                } catch (NumberFormatException e) {
                    log.error("Failed to set user's age", e);
                    resultMessage = messageService.createIncorrectUserAgeMessage(update);
                }
            } else {
                user.setStageState(true);
                resultMessage = messageService.createAskUserAgeMessage(update);
            }
        }

        if (user.getStage() == CHANGING_CITY) {
            if (user.getStageState()) {
                try {
                    String city = update.getMessage().getText();
                    user.setCity(city);
                    user.setStage(CHANGING_GENDER);
                    user.setStageState(false);
                } catch (NumberFormatException e) {
                    log.error("Failed to set user's age", e);
                }
            } else {
                user.setStageState(true);
                resultMessage = messageService.createAskUserCityMessage(update);
            }
        }

        if (user.getStage() == CHANGING_GENDER) {
            if (user.getStageState()) {
                try {
                    Gender gender = Gender.valueOf(update.getCallbackQuery().getData());
                    user.setGender(gender);
                    user.setStage(PAUSE);
                    user.setStageState(false);
                } catch (NumberFormatException e) {
                    log.error("Failed to set user's age", e);
                }
            } else {
                user.setStageState(true);
                resultMessage = messageService.createAskUserGenderMessage(update);
            }
        }
        userRepository.save(user);

        if (user.getStage() == PAUSE) {
            resultMessage = messageService.createConfigFinishedMessage(user, update);
        }
        return resultMessage;
    }

}

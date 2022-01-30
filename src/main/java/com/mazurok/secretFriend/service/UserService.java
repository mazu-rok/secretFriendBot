package com.mazurok.secretFriend.service;

import com.mazurok.secretFriend.exceptions.IllegalInputException;
import com.mazurok.secretFriend.repository.UserRepository;
import com.mazurok.secretFriend.repository.entity.Gender;
import com.mazurok.secretFriend.repository.entity.StagePart;
import com.mazurok.secretFriend.repository.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

import static com.mazurok.secretFriend.repository.entity.Stage.*;
import static com.mazurok.secretFriend.repository.entity.StagePart.*;

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
        Long chatId = update.hasCallbackQuery() ? update.getCallbackQuery().getMessage().getChatId() : update.getMessage().getChatId();
        Optional<UserEntity> user = userRepository.findById(userId);

        UserEntity userEntity = user.orElseGet(() -> createUserEntity(update));
        if (!userEntity.getChatId().equals(chatId)) {
            userEntity.setChatId(chatId);
            userRepository.save(userEntity);
        }

        return userEntity;
    }

    private UserEntity createUserEntity(Update update) {
        UserEntity user = UserEntity.builder()
                .id(update.getMessage().getFrom().getId())
                .firstName(update.getMessage().getFrom().getFirstName())
                .lastName(update.getMessage().getFrom().getLastName())
                .userName(update.getMessage().getFrom().getUserName())
                .chatId(update.getMessage().getChatId())
                .stage(CONFIGURE_FULL_PROFILE)
                .stagePart(ASK_AGE)
                .build();
        userRepository.save(user);
        return user;
    }

    public Object setUserAge(UserEntity user, Update update) {
        try {
            Integer age = Integer.valueOf(update.getMessage().getText());
            user.setAge(age);
            user.setStagePart(NO_ACTION);
            userRepository.save(user);
            return messageService.createSavedMessage(user.getChatId());
        } catch (NumberFormatException e) {
            log.error("Failed to set user's age", e);
            return messageService.createIncorrectUserAgeMessage(user.getChatId());
        }
    }

    public Object askUserAge(UserEntity user) {
        user.setStagePart(SET_AGE);
        userRepository.save(user);
        return messageService.createAskUserAgeMessage(user.getChatId());
    }

    public Object setUserCity(UserEntity user, Update update) {
        String city = update.getMessage().getText();
        user.setCity(city);
        user.setStagePart(NO_ACTION);
        userRepository.save(user);
        return messageService.createSavedMessage(user.getChatId());
    }

    public Object askUserCity(UserEntity user) {
        user.setStagePart(SET_CITY);
        userRepository.save(user);
        return messageService.createAskUserCityMessage(user.getChatId());
    }

    public Object setUserGender(UserEntity user, Update update) {
        Gender gender = Gender.valueOf(update.getCallbackQuery().getData());
        user.setGender(gender);
        user.setStagePart(NO_ACTION);
        userRepository.save(user);
        return messageService.createSavedMessage(user.getChatId());
    }

    public Object askUserGender(UserEntity user) {
        user.setStagePart(SET_GENDER);
        userRepository.save(user);
        return messageService.createAskUserGenderMessage(user.getChatId());
    }

    public Object userConfig(UserEntity user, Update update) {
        Object resultMessage = null;

        if (user.getStagePart().equals(ASK_AGE)) {
            resultMessage = askUserAge(user);
        } else if (user.getStagePart().equals(SET_AGE)) {
            resultMessage = setUserAge(user, update);
            user.setStagePart(ASK_CITY);
        }

        if (user.getStagePart().equals(ASK_CITY)) {
            resultMessage = askUserCity(user);
        } else if (user.getStagePart().equals(SET_CITY)) {
            resultMessage = setUserCity(user, update);
            user.setStagePart(ASK_GENDER);
        }

        if (user.getStagePart().equals(ASK_GENDER)) {
            resultMessage = askUserGender(user);
        } else if (user.getStagePart().equals(SET_GENDER)) {
            resultMessage = setUserGender(user, update);
        }

        // test output
        if (user.getStagePart().equals(NO_ACTION)) {
            user.setStage(CONFIGURE_FULL_SECRET_FRIEND_PROFILE);
            user.setStagePart(StagePart.ASK_SECRET_FRIEND_AGE);
            userRepository.save(user);
            resultMessage = messageService.createConfigFinishedMessage(user, update.getCallbackQuery().getId());
        }
        return resultMessage;
    }

    public Object setSecretFriendAge(UserEntity user, Update update) {
        try {
            String[] minMaxAge = update.getMessage().getText().split("-");
            int minAge;
            int maxAge;
            if (minMaxAge.length == 1) {
                minAge = Integer.parseInt(minMaxAge[0].trim());
                maxAge = Integer.parseInt(minMaxAge[0]);
            } else if (minMaxAge.length == 2) {
                minAge = Integer.parseInt(minMaxAge[0]);
                maxAge = Integer.parseInt(minMaxAge[1]);
            } else {
                throw new IllegalInputException("Failed to parse age");
            }
            user.getSecretFriendConfig().setMinAge(minAge);
            user.getSecretFriendConfig().setMaxAge(maxAge);
            user.setStagePart(NO_ACTION);
            userRepository.save(user);
            return messageService.createSavedMessage(user.getChatId());
        } catch (NumberFormatException | IllegalInputException e) {
            log.error("Failed to set user's age", e);
            return messageService.createIncorrectSecretFriendAgeMessage(user.getChatId());
        }
    }

    public Object askSecretFriendAge(UserEntity user) {
        user.setStagePart(SET_SECRET_FRIEND_AGE);
        userRepository.save(user);
        return messageService.createAskSecretFriendAgeMessage(user.getChatId());
    }

    public Object setSecretFriendCity(UserEntity user, Update update) {
        String city = update.getMessage().getText();
        user.getSecretFriendConfig().setCity(city);
        user.setStagePart(NO_ACTION);
        userRepository.save(user);
        return messageService.createSavedMessage(user.getChatId());
    }

    public Object askSecretFriendCity(UserEntity user) {
        user.setStagePart(SET_SECRET_FRIEND_CITY);
        userRepository.save(user);
        return messageService.createAskSecretFriendCityMessage(user.getChatId());
    }

    public Object setSecretFriendGender(UserEntity user, Update update) {
        Gender gender = Gender.valueOf(update.getCallbackQuery().getData());
        user.getSecretFriendConfig().setGender(gender);
        user.setStagePart(NO_ACTION);
        userRepository.save(user);
        return messageService.createSavedMessage(user.getChatId());
    }

    public Object askSecretFriendGender(UserEntity user) {
        user.setStagePart(SET_SECRET_FRIEND_GENDER);
        userRepository.save(user);
        return messageService.createAskSecretFriendGenderMessage(user.getChatId());
    }

    public Object userSecretFriendConfig(UserEntity user, Update update) {
        Object resultMessage = null;

        if (user.getStagePart().equals(ASK_SECRET_FRIEND_AGE)) {
            resultMessage = askSecretFriendAge(user);
        } else if (user.getStagePart().equals(SET_SECRET_FRIEND_AGE)) {
            resultMessage = setSecretFriendAge(user, update);
            user.setStagePart(ASK_SECRET_FRIEND_CITY);
        }

        if (user.getStagePart().equals(ASK_SECRET_FRIEND_CITY)) {
            resultMessage = askSecretFriendCity(user);
        } else if (user.getStagePart().equals(SET_SECRET_FRIEND_CITY)) {
            resultMessage = setSecretFriendCity(user, update);
            user.setStagePart(ASK_SECRET_FRIEND_GENDER);
        }

        if (user.getStagePart().equals(ASK_SECRET_FRIEND_GENDER)) {
            resultMessage = askSecretFriendGender(user);
        } else if (user.getStagePart().equals(SET_SECRET_FRIEND_GENDER)) {
            resultMessage = setSecretFriendGender(user, update);
        }

        // test output
        if (user.getStagePart().equals(NO_ACTION)) {
            user.setStage(NO_STAGE);
            userRepository.save(user);
            resultMessage = messageService.createSecretFriendConfigFinishedMessage(user, update.getCallbackQuery().getId());
        }
        return resultMessage;
    }
}

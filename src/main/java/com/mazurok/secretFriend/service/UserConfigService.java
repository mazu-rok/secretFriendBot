package com.mazurok.secretFriend.service;

import com.mazurok.secretFriend.exceptions.IllegalInputException;
import com.mazurok.secretFriend.repository.UserRepository;
import com.mazurok.secretFriend.repository.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.mazurok.secretFriend.repository.entity.Stage.*;
import static com.mazurok.secretFriend.repository.entity.StagePart.*;
import static com.mazurok.secretFriend.repository.entity.StagePart.NO_ACTION;

@Slf4j
@Service
public class UserConfigService {
    private final UserRepository userRepository;
    private final MessageSource messageSource;

    @Autowired
    public UserConfigService(UserRepository userRepository,
                             MessageSource messageSource) {
        this.userRepository = userRepository;
        this.messageSource = messageSource;
    }

    public List<Object> handleConfigCommand(Commands command, UserEntity user) {
        List<Object> messages = new ArrayList<>();

        switch (command) {
            case CONFIGURE_PROFILE -> {
                messages.add(createAskConfigProfileMessage(user.getChatId(), user.getLanguage()));
                user.setStage(CONFIGURE_PROFILE);
                userRepository.save(user);
            }
            case CONFIGURE_SECRET_FRIEND_PROFILE -> {
                messages.add(createAskConfigProfileMessage(user.getChatId(), user.getLanguage()));
                user.setStage(CONFIGURE_SECRET_FRIEND_PROFILE);
                userRepository.save(user);
            }
            case SHOW_PROFILE -> messages.add(createShowProfileMessage(user));
        }
        return messages;
    }

    public List<Object> handleConfigStage(UserEntity user, Update update) {
        List<Object> messages = new ArrayList<>();
//        if (user.getStage().equals(CONFIGURE_FULL_PROFILE)) {
//            messages.addAll(userConfig(user, update));
//        }
//        if (user.getStage().equals(CONFIGURE_FULL_SECRET_FRIEND_PROFILE)) {
//            messages.addAll(userSecretFriendConfig(user, update));
//        }

        if (update.hasCallbackQuery() && update.getCallbackQuery().getData().contains("ASK")) {
            user.setStagePart(StagePart.valueOf(update.getCallbackQuery().getData()));
            messages.add(AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId()).build());
        }
        if (user.getStage().equals(CONFIGURE_PROFILE)) {
            messages.addAll(switch (user.getStagePart()) {
                case ASK_AGE -> askUserAge(user);
                case SET_AGE -> setUserAge(user, update);
                case ASK_CITY -> askUserCity(user);
                case SET_CITY -> setUserCity(user, update);
                case ASK_GENDER -> askUserGender(user);
                case SET_GENDER -> setUserGender(user, update);
                default -> Collections.emptyList();
            });
        } else if (user.getStage().equals(CONFIGURE_SECRET_FRIEND_PROFILE)) {
            messages.addAll(switch (user.getStagePart()) {
                case ASK_AGE -> askSecretFriendAge(user);
                case SET_AGE -> setSecretFriendAge(user, update);
                case ASK_CITY -> askSecretFriendCity(user);
                case SET_CITY -> setSecretFriendCity(user, update);
                case ASK_GENDER -> askSecretFriendGender(user);
                case SET_GENDER -> setSecretFriendGender(user, update);
                default -> Collections.emptyList();
            });
        }
        return messages;
    }


    private List<Object> setUserAge(UserEntity user, Update update) {
        try {
            Integer age = Integer.valueOf(update.getMessage().getText());
            user.setAge(age);
            user.setStage(NO_STAGE);
            user.setStagePart(NO_ACTION);
            userRepository.save(user);
            return List.of(createSavedMessage(user.getChatId(), user.getLanguage()));
        } catch (NumberFormatException e) {
            log.error("Failed to set user's age", e);
            return List.of(createIncorrectUserAgeMessage(user.getChatId(), user.getLanguage()));
        }
    }

    private List<Object> askUserAge(UserEntity user) {
        user.setStagePart(SET_AGE);
        userRepository.save(user);
        return List.of(createAskUserAgeMessage(user.getChatId(), user.getLanguage()));
    }

    private List<Object> setUserCity(UserEntity user, Update update) {
        String city = update.getMessage().getText();
        user.setCity(city);
        user.setStage(NO_STAGE);
        user.setStagePart(NO_ACTION);
        userRepository.save(user);
        return List.of(createSavedMessage(user.getChatId(), user.getLanguage()));
    }

    private List<Object> askUserCity(UserEntity user) {
        user.setStagePart(SET_CITY);
        userRepository.save(user);
        return List.of(createAskUserCityMessage(user.getChatId(), user.getLanguage()));
    }

    private List<Object> setUserGender(UserEntity user, Update update) {
        Gender gender = Gender.valueOf(update.getCallbackQuery().getData());
        user.setGender(gender);
        user.setStage(NO_STAGE);
        user.setStagePart(NO_ACTION);
        userRepository.save(user);
        return List.of(createSavedMessage(user.getChatId(), user.getLanguage()),
                AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId()).build());
    }

    private List<Object> askUserGender(UserEntity user) {
        user.setStagePart(SET_GENDER);
        userRepository.save(user);
        return List.of(createAskUserGenderMessage(user.getChatId(), user.getLanguage()));
    }

//    private List<Object> userConfig(UserEntity user, Update update) {
//        List<Object> resultMessage = null;
//
//        if (user.getStagePart().equals(ASK_AGE)) {
//            resultMessage = askUserAge(user);
//        } else if (user.getStagePart().equals(SET_AGE)) {
//            resultMessage = setUserAge(user, update);
//            user.setStagePart(ASK_CITY);
//        }
//
//        if (user.getStagePart().equals(ASK_CITY)) {
//            resultMessage = askUserCity(user);
//        } else if (user.getStagePart().equals(SET_CITY)) {
//            resultMessage = setUserCity(user, update);
//            user.setStagePart(ASK_GENDER);
//        }
//
//        if (user.getStagePart().equals(ASK_GENDER)) {
//            resultMessage = askUserGender(user);
//        } else if (user.getStagePart().equals(SET_GENDER)) {
//            resultMessage = setUserGender(user, update);
//        }

//        // test output
//        if (user.getStagePart().equals(NO_ACTION)) {
//            user.setStage(CONFIGURE_FULL_SECRET_FRIEND_PROFILE);
//            user.setStagePart(ASK_AGE);
//            userRepository.save(user);
//            resultMessage = List.of(createConfigFinishedMessage(user, update.getCallbackQuery().getId()));
//        }
//        return resultMessage;
//    }

    private List<Object> setSecretFriendAge(UserEntity user, Update update) {
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
            user.setStage(NO_STAGE);
            user.setStagePart(NO_ACTION);
            userRepository.save(user);
            return List.of(createSavedMessage(user.getChatId(), user.getLanguage()));
        } catch (NumberFormatException | IllegalInputException e) {
            log.error("Failed to set user's age", e);
            return List.of(createIncorrectSecretFriendAgeMessage(user.getChatId(), user.getLanguage()));
        }
    }

    private List<Object> askSecretFriendAge(UserEntity user) {
        user.setStagePart(SET_AGE);
        userRepository.save(user);
        return List.of(createAskSecretFriendAgeMessage(user.getChatId(), user.getLanguage()));
    }

    private List<Object> setSecretFriendCity(UserEntity user, Update update) {
        String city = update.getMessage().getText();
        user.getSecretFriendConfig().setCity(city);
        user.setStage(NO_STAGE);
        user.setStagePart(NO_ACTION);
        userRepository.save(user);
        return List.of(createSavedMessage(user.getChatId(), user.getLanguage()));
    }

    private List<Object> askSecretFriendCity(UserEntity user) {
        user.setStagePart(SET_CITY);
        userRepository.save(user);
        return List.of(createAskSecretFriendCityMessage(user.getChatId(), user.getLanguage()));
    }

    private List<Object> setSecretFriendGender(UserEntity user, Update update) {
        Gender gender = Gender.valueOf(update.getCallbackQuery().getData());
        user.getSecretFriendConfig().setGender(gender);
        user.setStage(NO_STAGE);
        user.setStagePart(NO_ACTION);
        userRepository.save(user);
        return List.of(createSavedMessage(user.getChatId(), user.getLanguage()),
                AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId()).build());
    }

    private List<Object> askSecretFriendGender(UserEntity user) {
        user.setStagePart(SET_GENDER);
        userRepository.save(user);
        return List.of(createAskSecretFriendGenderMessage(user.getChatId(), user.getLanguage()));
    }

//    public List<Object> userSecretFriendConfig(UserEntity user, Update update) {
//        List<Object> resultMessage = null;
//
//        if (user.getStagePart().equals(ASK_AGE)) {
//            resultMessage = askSecretFriendAge(user);
//        } else if (user.getStagePart().equals(SET_AGE)) {
//            resultMessage = setSecretFriendAge(user, update);
//            user.setStagePart(ASK_CITY);
//        }
//
//        if (user.getStagePart().equals(ASK_CITY)) {
//            resultMessage = askSecretFriendCity(user);
//        } else if (user.getStagePart().equals(SET_CITY)) {
//            resultMessage = setSecretFriendCity(user, update);
//            user.setStagePart(ASK_GENDER);
//        }
//
//        if (user.getStagePart().equals(ASK_GENDER)) {
//            resultMessage = askSecretFriendGender(user);
//        } else if (user.getStagePart().equals(SET_GENDER)) {
//            resultMessage = setSecretFriendGender(user, update);
//        }
//
//        // test output
//        if (user.getStagePart().equals(NO_ACTION)) {
//            user.setStage(NO_STAGE);
//            userRepository.save(user);
//            resultMessage = List.of(createSecretFriendConfigFinishedMessage(user, update.getCallbackQuery().getId()));
//        }
//        return resultMessage;
//    }

    /**
     * Creating messages
     **/

    private SendMessage createSavedMessage(Long chatId, Language lang) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("saved_msg",null, new Locale(lang.name())))
                .build();
    }

    private SendMessage createAskUserAgeMessage(Long chatId, Language lang) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("ask_age_msg",null, new Locale(lang.name())))
                .build();
    }

    private SendMessage createIncorrectUserAgeMessage(Long chatId, Language lang) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("incorrect_age_msg",null, new Locale(lang.name())))
                .build();
    }

    private SendMessage createAskUserCityMessage(Long chatId, Language lang) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("ask_city_msg",null, new Locale(lang.name())))
                .build();
    }

    private SendMessage createAskUserGenderMessage(Long chatId, Language lang) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> buttons1 = new ArrayList<>();
        buttons1.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("man_gender_answer_msg",null, new Locale(lang.name())))
                .callbackData(Gender.MALE.name())
                .build());
        buttons1.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("woman_gender_answer_msg",null, new Locale(lang.name())))
                .callbackData(Gender.FEMALE.name())
                .build());
        buttons.add(buttons1);

        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(buttons);

        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("ask_gender_msg",null, new Locale(lang.name())))
                .replyMarkup(markupKeyboard)
                .build();
    }

    private SendMessage createAskConfigProfileMessage(Long chatId, Language lang) {
        List<List<InlineKeyboardButton>> actions = new ArrayList<>();
        List<InlineKeyboardButton> actionsLine1 = new ArrayList<>();
        List<InlineKeyboardButton> actionsLine2 = new ArrayList<>();
        List<InlineKeyboardButton> actionsLine3 = new ArrayList<>();
        actionsLine1.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("change_age_btn_msg",null, new Locale(lang.name())))
                .callbackData(ASK_AGE.name()).build());
        actionsLine2.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("change_city_btn_msg",null, new Locale(lang.name())))
                .callbackData(ASK_CITY.name()).build());
        actionsLine3.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("change_gender_btn_msg",null, new Locale(lang.name())))
                .callbackData(ASK_GENDER.name()).build());
        actions.add(actionsLine1);
        actions.add(actionsLine2);
        actions.add(actionsLine3);

        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("ask_what_change_msg",null, new Locale(lang.name())))
                .replyMarkup(new InlineKeyboardMarkup(actions))
                .build();
    }

    private SendMessage createShowProfileMessage(UserEntity user) {
        return SendMessage.builder()
                .chatId(String.valueOf(user.getChatId()))
                .text(messageSource.getMessage("show_profile_msg", List.of(user.getFirstName(), user.getLastName(),
                        user.getAge(), user.getCity(), user.getGender()).toArray(), new Locale(user.getLanguage().name())))
                .build();
    }


    /**
     * Messages for secret friend config
     **/
    private SendMessage createAskSecretFriendAgeMessage(Long chatId, Language lang) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("ask_secret_friend_age_msg",null, new Locale(lang.name())))
                .build();
    }

    public SendMessage createIncorrectSecretFriendAgeMessage(Long chatId, Language lang) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("incorrect_secret_friend_age_msg",null, new Locale(lang.name())))
                .build();
    }

    public SendMessage createAskSecretFriendCityMessage(Long chatId, Language lang) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("ask_secret_friend_city_msg",null, new Locale(lang.name())))
                .build();
    }

    public SendMessage createAskSecretFriendGenderMessage(Long chatId, Language lang) {
        List<List<InlineKeyboardButton>> genderButtons = new ArrayList<>();
        List<InlineKeyboardButton> genderButtonsLine1 = new ArrayList<>();
        genderButtonsLine1.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("man_gender_answer_msg",null, new Locale(lang.name())))
                .callbackData(Gender.MALE.name()).build());
        genderButtonsLine1.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("woman_gender_answer_msg",null, new Locale(lang.name())))
                .callbackData(Gender.FEMALE.name()).build());
        genderButtonsLine1.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("any_gender_answer_msg",null, new Locale(lang.name())))
                .callbackData(Gender.ANY.name()).build());
        genderButtons.add(genderButtonsLine1);

        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(genderButtons);

        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("ask_secret_Friend_gender_msg",null, new Locale(lang.name())))
                .replyMarkup(markupKeyboard)
                .build();
    }

//    public AnswerCallbackQuery createSecretFriendConfigFinishedMessage(UserEntity user, String callbackId) {
//        return AnswerCallbackQuery.builder()
//                .callbackQueryId(callbackId)
//                .text(messageSource.getMessage("any_gender_answer_msg",null, new Locale(lang.name()))
//
//                        String.format(secretFriendConfigFinished, user.getSecretFriendConfig().getGender(),
//                        user.getSecretFriendConfig().getMinAge(), user.getSecretFriendConfig().getMaxAge(),
//                        user.getSecretFriendConfig().getCity()))
//                .showAlert(true)
//                .build();
//    }

}

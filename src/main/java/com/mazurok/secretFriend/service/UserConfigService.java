package com.mazurok.secretFriend.service;

import com.mazurok.secretFriend.exceptions.IllegalInputException;
import com.mazurok.secretFriend.repository.UserRepository;
import com.mazurok.secretFriend.repository.entity.Commands;
import com.mazurok.secretFriend.repository.entity.Gender;
import com.mazurok.secretFriend.repository.entity.StagePart;
import com.mazurok.secretFriend.repository.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mazurok.secretFriend.repository.entity.Stage.*;
import static com.mazurok.secretFriend.repository.entity.StagePart.*;
import static com.mazurok.secretFriend.repository.entity.StagePart.NO_ACTION;

@Slf4j
@Service
public class UserConfigService {
    private final UserRepository userRepository;

    @Autowired
    public UserConfigService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<Object> handleConfigCommand(Commands command, UserEntity user, Update update) {
        List<Object> messages = new ArrayList<>();

        switch (command) {
            case CONFIGURE_PROFILE -> {
                messages.add(createAskConfigProfileMessage(user.getChatId()));
                user.setStage(CONFIGURE_PROFILE);
                userRepository.save(user);
            }
            case CONFIGURE_SECRET_FRIEND_PROFILE -> {
                messages.add(createAskConfigProfileMessage(user.getChatId()));
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
            return List.of(createSavedMessage(user.getChatId()));
        } catch (NumberFormatException e) {
            log.error("Failed to set user's age", e);
            return List.of(createIncorrectUserAgeMessage(user.getChatId()));
        }
    }

    private List<Object> askUserAge(UserEntity user) {
        user.setStagePart(SET_AGE);
        userRepository.save(user);
        return List.of(createAskUserAgeMessage(user.getChatId()));
    }

    private List<Object> setUserCity(UserEntity user, Update update) {
        String city = update.getMessage().getText();
        user.setCity(city);
        user.setStage(NO_STAGE);
        user.setStagePart(NO_ACTION);
        userRepository.save(user);
        return List.of(createSavedMessage(user.getChatId()));
    }

    private List<Object> askUserCity(UserEntity user) {
        user.setStagePart(SET_CITY);
        userRepository.save(user);
        return List.of(createAskUserCityMessage(user.getChatId()));
    }

    private List<Object> setUserGender(UserEntity user, Update update) {
        Gender gender = Gender.valueOf(update.getCallbackQuery().getData());
        user.setGender(gender);
        user.setStage(NO_STAGE);
        user.setStagePart(NO_ACTION);
        userRepository.save(user);
        return List.of(createSavedMessage(user.getChatId()),
                AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId()).build());
    }

    private List<Object> askUserGender(UserEntity user) {
        user.setStagePart(SET_GENDER);
        userRepository.save(user);
        return List.of(createAskUserGenderMessage(user.getChatId()));
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
            return List.of(createSavedMessage(user.getChatId()));
        } catch (NumberFormatException | IllegalInputException e) {
            log.error("Failed to set user's age", e);
            return List.of(createIncorrectSecretFriendAgeMessage(user.getChatId()));
        }
    }

    private List<Object> askSecretFriendAge(UserEntity user) {
        user.setStagePart(SET_AGE);
        userRepository.save(user);
        return List.of(createAskSecretFriendAgeMessage(user.getChatId()));
    }

    private List<Object> setSecretFriendCity(UserEntity user, Update update) {
        String city = update.getMessage().getText();
        user.getSecretFriendConfig().setCity(city);
        user.setStage(NO_STAGE);
        user.setStagePart(NO_ACTION);
        userRepository.save(user);
        return List.of(createSavedMessage(user.getChatId()));
    }

    private List<Object> askSecretFriendCity(UserEntity user) {
        user.setStagePart(SET_CITY);
        userRepository.save(user);
        return List.of(createAskSecretFriendCityMessage(user.getChatId()));
    }

    private List<Object> setSecretFriendGender(UserEntity user, Update update) {
        Gender gender = Gender.valueOf(update.getCallbackQuery().getData());
        user.getSecretFriendConfig().setGender(gender);
        user.setStage(NO_STAGE);
        user.setStagePart(NO_ACTION);
        userRepository.save(user);
        return List.of(createSavedMessage(user.getChatId()),
                AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId()).build());
    }

    private List<Object> askSecretFriendGender(UserEntity user) {
        user.setStagePart(SET_GENDER);
        userRepository.save(user);
        return List.of(createAskSecretFriendGenderMessage(user.getChatId()));
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

    // TODO: add localization
    private static final String changeAge = "Change age";
    private static final String changeCity = "Change city";
    private static final String changeGender = "Change gender";
    private final static String savedMessage = "Saved";
    private final static String askAgeMessage = "Please set your age";
    private final static String incorrectAgeMessage = "Use only numbers, for example: 21";

    private final static String askCityMessage = """
            Please set your city.
            Use official english name, use wikipedia for example: Kyiv (NOT Kiev!)
            It is important for automatic search""";

    private final static String askGenderMessage = "Please set gender";
    private final static String manGenderAnswerMessage = "Male";
    private final static String womanGenderAnswerMessage = "Female";
    private final static String anyGenderAnswerMessage = "Any";
    private final static String askWhatChangeMessage = "What do you want to change?";

//    private final static String userConfigFinished = """
//            Your profile configured:
//            First Name: %s
//            Last Name: %s
//            Gender: %s
//            Age: %s
//            City: %s
//            """;

    /**
     * Text for secret friend config
     **/
    private final static String askSecretFriendAgeMessage = "Please set age for your secret friend in format: minAge-maxAge. Example: 20-25";
    private final static String incorrectSecretFriendAgeMessage = "Use only numbers, for example: 20-25";

    private final static String askSecretFriendCityMessage = """
            Please set your secret friend city.
            Use official english name, you can use wikipedia to check correct name. For example: Kyiv (NOT Kiev!)
            It is important for automatic search""";

    private final static String askSecretFriendGenderMessage = "Please set gender of your friend";

    private final static String secretFriendConfigFinished = """
            Your Secret Friend profile configured:
            Gender: %s
            Age: %s-%s
            City: %s
            """;

    private SendMessage createSavedMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(savedMessage)
                .build();
    }

    private SendMessage createAskUserAgeMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(askAgeMessage)
                .build();
    }

    private SendMessage createIncorrectUserAgeMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(incorrectAgeMessage)
                .build();
    }

    private SendMessage createAskUserCityMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(askCityMessage)
                .build();
    }

    private SendMessage createAskUserGenderMessage(Long chatId) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> buttons1 = new ArrayList<>();
        buttons1.add(InlineKeyboardButton.builder().text(manGenderAnswerMessage).callbackData(Gender.MALE.name()).build());
        buttons1.add(InlineKeyboardButton.builder().text(womanGenderAnswerMessage).callbackData(Gender.FEMALE.name()).build());
        buttons.add(buttons1);

        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(buttons);

        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(askGenderMessage)
                .replyMarkup(markupKeyboard)
                .build();
    }

    private SendMessage createAskConfigProfileMessage(Long chatId) {
        List<List<InlineKeyboardButton>> actions = new ArrayList<>();
        List<InlineKeyboardButton> actionsLine1 = new ArrayList<>();
        List<InlineKeyboardButton> actionsLine2 = new ArrayList<>();
        List<InlineKeyboardButton> actionsLine3 = new ArrayList<>();
        actionsLine1.add(InlineKeyboardButton.builder().text(changeAge).callbackData(ASK_AGE.name()).build());
        actionsLine2.add(InlineKeyboardButton.builder().text(changeCity).callbackData(ASK_CITY.name()).build());
        actionsLine3.add(InlineKeyboardButton.builder().text(changeGender).callbackData(ASK_GENDER.name()).build());
        actions.add(actionsLine1);
        actions.add(actionsLine2);
        actions.add(actionsLine3);

        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(askWhatChangeMessage)
                .replyMarkup(new InlineKeyboardMarkup(actions))
                .build();
    }

    private SendMessage createShowProfileMessage(UserEntity user) {
        return SendMessage.builder()
                .chatId(String.valueOf(user.getChatId()))
                .text("""
                        This is your profile:
                        First name: %s
                        Last name: %s
                        Age: %s
                        City: %s
                        Gender: %s
                        """.formatted(user.getFirstName(), user.getLastName(), user.getAge(),
                        user.getCity(), user.getGender()))
                .build();
    }


    /**
     * Messages for secret friend config
     **/
    private SendMessage createAskSecretFriendAgeMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(askSecretFriendAgeMessage)
                .build();
    }

    public SendMessage createIncorrectSecretFriendAgeMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(incorrectSecretFriendAgeMessage)
                .build();
    }

    public SendMessage createAskSecretFriendCityMessage(Long chatId) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(askSecretFriendCityMessage)
                .build();
    }

    public SendMessage createAskSecretFriendGenderMessage(Long chatId) {
        List<List<InlineKeyboardButton>> genderButtons = new ArrayList<>();
        List<InlineKeyboardButton> genderButtonsLine1 = new ArrayList<>();
        genderButtonsLine1.add(InlineKeyboardButton.builder().text(manGenderAnswerMessage).callbackData(Gender.MALE.name()).build());
        genderButtonsLine1.add(InlineKeyboardButton.builder().text(womanGenderAnswerMessage).callbackData(Gender.FEMALE.name()).build());
        genderButtonsLine1.add(InlineKeyboardButton.builder().text(anyGenderAnswerMessage).callbackData(Gender.ANY.name()).build());
        genderButtons.add(genderButtonsLine1);

        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(genderButtons);

        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(askSecretFriendGenderMessage)
                .replyMarkup(markupKeyboard)
                .build();
    }

    public AnswerCallbackQuery createSecretFriendConfigFinishedMessage(UserEntity user, String callbackId) {
        return AnswerCallbackQuery.builder()
                .callbackQueryId(callbackId)
                .text(String.format(secretFriendConfigFinished, user.getSecretFriendConfig().getGender(),
                        user.getSecretFriendConfig().getMinAge(), user.getSecretFriendConfig().getMaxAge(),
                        user.getSecretFriendConfig().getCity()))
                .showAlert(true)
                .build();
    }

}

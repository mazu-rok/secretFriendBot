package com.mazurok.secretFriend.service;

import com.mazurok.secretFriend.exceptions.IllegalInputException;
import com.mazurok.secretFriend.repository.UserRepository;
import com.mazurok.secretFriend.repository.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.util.Pair;
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
    private final ButtonsService buttonsService;
    private final MessageSource messageSource;

    @Autowired
    public UserConfigService(UserRepository userRepository,
                             ButtonsService buttonsService,
                             MessageSource messageSource) {
        this.userRepository = userRepository;
        this.buttonsService = buttonsService;
        this.messageSource = messageSource;
    }

    public List<Object> handleConfigCommand(Commands command, UserEntity user) {
        List<Object> messages = new ArrayList<>();

        switch (command) {
            case CONFIGURE_PROFILE -> {
                messages.add(SendMessage.builder()
                        .text(messageSource.getMessage("show_profile_msg", List.of(user.getFirstName(), user.getLastName(),
                                user.getAge(), user.getCity(), user.getGender()).toArray(), new Locale(user.getLanguage().name())))
                        .chatId(String.valueOf(user.getChatId()))
                        .replyMarkup(buttonsService.createCancelButton(user.getLanguage()))
                        .build());
                messages.add(createAskConfigProfileMessage(user.getChatId(), user.getLanguage()));
                user.getStages().add(Pair.of(CONFIGURE_PROFILE, NO_ACTION));
                userRepository.save(user);
            }
            case CONFIGURE_SECRET_FRIEND_PROFILE -> {
                messages.add(SendMessage.builder()
                        .text(messageSource.getMessage("update_sec_friend_profile_msg",
                                List.of(user.getSecretFriendConfig().getGender(), user.getSecretFriendConfig().getMinAge(),
                                user.getSecretFriendConfig().getMaxAge(),user.getSecretFriendConfig().getCity()).toArray(),
                                new Locale(user.getLanguage().name())))
                        .chatId(String.valueOf(user.getChatId()))
                        .replyMarkup(buttonsService.createCancelButton(user.getLanguage()))
                        .build());
                messages.add(createAskConfigProfileMessage(user.getChatId(), user.getLanguage()));
                user.getStages().add(Pair.of(CONFIGURE_SECRET_FRIEND_PROFILE, NO_ACTION));
                userRepository.save(user);
            }
            case SHOW_PROFILE -> messages.add(createShowProfileMessage(user));
        }
        return messages;
    }

    public List<Object> handleConfigStage(UserEntity user, Update update) {
        List<Object> messages = new ArrayList<>();
        Pair<Stage, StagePart> userStage = user.getStages().peek();
        if (userStage.getFirst().equals(CONFIGURE_FULL_PROFILE)) {
            messages.addAll(userFullConfig(user, update));
            return messages;
        }
//        if (user.getStage().equals(CONFIGURE_FULL_SECRET_FRIEND_PROFILE)) {
//            messages.addAll(userSecretFriendConfig(user, update));
//        }

        if (update.hasCallbackQuery() && update.getCallbackQuery().getData().contains("ASK")) {
            user.getStages().pop();
            user.getStages().add(Pair.of(userStage.getFirst(), StagePart.valueOf(update.getCallbackQuery().getData())));
            userStage = user.getStages().peek();

            messages.add(AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId()).build());
        }
        if (userStage.getFirst().equals(CONFIGURE_PROFILE)) {
            messages.addAll(switch (userStage.getSecond()) {
                case ASK_AGE -> askUserAge(user);
                case SET_AGE -> setUserAge(user, update);
                case ASK_CITY -> askUserCity(user);
                case SET_CITY -> setUserCity(user, update);
                case ASK_GENDER -> askUserGender(user);
                case SET_GENDER -> setUserGender(user, update);
                default -> Collections.emptyList();
            });
        } else if (userStage.getFirst().equals(CONFIGURE_SECRET_FRIEND_PROFILE)) {
            messages.addAll(switch (userStage.getSecond()) {
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
            user.getStages().pop();

            userRepository.save(user);
            return List.of(createSavedMessage(user.getChatId(), user.getLanguage()));
        } catch (NumberFormatException e) {
            log.error("Failed to set user's age", e);
            return List.of(createIncorrectUserAgeMessage(user.getChatId(), user.getLanguage()));
        }
    }

    private List<Object> askUserAge(UserEntity user) {
        user.replaceLastStagePart(SET_AGE);
        userRepository.save(user);
        return List.of(createAskUserAgeMessage(user.getChatId(), user.getLanguage()));
    }

    private List<Object> setUserCity(UserEntity user, Update update) {
        String city = update.getMessage().getText();
        user.setCity(city);
        user.getStages().pop();
        userRepository.save(user);
        return List.of(createSavedMessage(user.getChatId(), user.getLanguage()));
    }

    private List<Object> askUserCity(UserEntity user) {
        user.replaceLastStagePart(SET_CITY);
        userRepository.save(user);
        return List.of(createAskUserCityMessage(user.getChatId(), user.getLanguage()));
    }

    private List<Object> setUserGender(UserEntity user, Update update) {
        Gender gender = Gender.valueOf(update.getCallbackQuery().getData());
        user.setGender(gender);
        user.getStages().pop();
        userRepository.save(user);
        return List.of(createSavedMessage(user.getChatId(), user.getLanguage()),
                AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId()).build());
    }

    private List<Object> askUserGender(UserEntity user) {
        user.replaceLastStagePart(SET_GENDER);
        userRepository.save(user);
        return List.of(createAskUserGenderMessage(user.getChatId(), user.getLanguage()));
    }

    private List<Object> userFullConfig(UserEntity user, Update update) {
        List<Object> resultMessages = null;

        if (user.getStages().peek().getSecond().equals(ASK_AGE)) {
            resultMessages = askUserAge(user);
        } else if (user.getStages().peek().getSecond().equals(SET_AGE)) {
            resultMessages = setUserAge(user, update);
            if (user.getStages().peek().getSecond().equals(SET_AGE)) {
                return resultMessages;
            }
            user.getStages().add(Pair.of(CONFIGURE_FULL_PROFILE, ASK_CITY));
        }

        if (user.getStages().peek().getSecond().equals(ASK_CITY)) {
            resultMessages = askUserCity(user);
        } else if (user.getStages().peek().getSecond().equals(SET_CITY)) {
            resultMessages = setUserCity(user, update);
            user.getStages().add(Pair.of(CONFIGURE_FULL_PROFILE, ASK_GENDER));
        }

        if (user.getStages().peek().getSecond().equals(ASK_GENDER)) {
            resultMessages = askUserGender(user);
        } else if (user.getStages().peek().getSecond().equals(SET_GENDER)) {
            resultMessages = setUserGender(user, update);
        }

        if (user.getStages().peek().getFirst().equals(NO_STAGE)) {
            resultMessages = List.of(createUserProfileFinishedMsg(user, update.getCallbackQuery().getId()),
                    SendMessage.builder()
                            .chatId(String.valueOf(user.getChatId()))
                            .replyMarkup(buttonsService.createMainButtons(user.getLanguage()))
                            .text(messageSource.getMessage("profile_configured_msg", null, new Locale(user.getLanguage().name())))
                            .build());
        }
        return resultMessages;
    }

    public AnswerCallbackQuery createUserProfileFinishedMsg(UserEntity user, String callbackId) {
        return AnswerCallbackQuery.builder()
                .callbackQueryId(callbackId)
                .text(messageSource.getMessage("show_profile_msg", List.of(user.getFirstName(), user.getLastName(),
                        user.getAge(), user.getCity(), user.getGender()).toArray(), new Locale(user.getLanguage().name())))
                .showAlert(true)
                .build();
    }

    public List<Object> handleCancelCommand(UserEntity user) {
        Pair<Stage, StagePart> stage = user.getStages().pop();
        userRepository.save(user);
//        if (stage.getFirst().equals(CONFIGURE_PROFILE)) {
        return List.of(SendMessage.builder()
                .text(messageSource.getMessage("no_stage_msg",
                        List.of(user.getFirstName()).toArray(), new Locale(user.getLanguage().name())))
                .replyMarkup(buttonsService.createMainButtons(user.getLanguage())).build());

    }

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
            user.getStages().pop();
            userRepository.save(user);
            return List.of(createSavedMessage(user.getChatId(), user.getLanguage()));
        } catch (NumberFormatException | IllegalInputException e) {
            log.error("Failed to set user's age", e);
            return List.of(createIncorrectSecretFriendAgeMessage(user.getChatId(), user.getLanguage()));
        }
    }

    private List<Object> askSecretFriendAge(UserEntity user) {
        user.replaceLastStagePart(SET_AGE);
        userRepository.save(user);
        return List.of(createAskSecretFriendAgeMessage(user.getChatId(), user.getLanguage()));
    }

    private List<Object> setSecretFriendCity(UserEntity user, Update update) {
        String city = update.getMessage().getText();
        user.getSecretFriendConfig().setCity(city);
        user.getStages().pop();
        userRepository.save(user);
        return List.of(createSavedMessage(user.getChatId(), user.getLanguage()));
    }

    private List<Object> askSecretFriendCity(UserEntity user) {
        user.replaceLastStagePart(SET_CITY);
        userRepository.save(user);
        return List.of(createAskSecretFriendCityMessage(user.getChatId(), user.getLanguage()));
    }

    private List<Object> setSecretFriendGender(UserEntity user, Update update) {
        Gender gender = Gender.valueOf(update.getCallbackQuery().getData());
        user.getSecretFriendConfig().setGender(gender);
        user.getStages().pop();
        userRepository.save(user);
        return List.of(createSavedMessage(user.getChatId(), user.getLanguage()),
                AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId()).build());
    }

    private List<Object> askSecretFriendGender(UserEntity user) {
        user.replaceLastStagePart(SET_GENDER);
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
                .text(messageSource.getMessage("saved_msg", null, new Locale(lang.name())))
                .replyMarkup(buttonsService.createMainButtons(lang))
                .build();
    }

    private SendMessage createAskUserAgeMessage(Long chatId, Language lang) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("ask_age_msg", null, new Locale(lang.name())))
                .build();
    }

    private SendMessage createIncorrectUserAgeMessage(Long chatId, Language lang) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("incorrect_age_msg", null, new Locale(lang.name())))
                .build();
    }

    private SendMessage createAskUserCityMessage(Long chatId, Language lang) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("ask_city_msg", null, new Locale(lang.name())))
                .build();
    }

    private SendMessage createAskUserGenderMessage(Long chatId, Language lang) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> buttons1 = new ArrayList<>();
        buttons1.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("man_gender_answer_msg", null, new Locale(lang.name())))
                .callbackData(Gender.MALE.name())
                .build());
        buttons1.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("woman_gender_answer_msg", null, new Locale(lang.name())))
                .callbackData(Gender.FEMALE.name())
                .build());
        buttons.add(buttons1);

        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(buttons);

        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("ask_gender_msg", null, new Locale(lang.name())))
                .replyMarkup(markupKeyboard)
                .build();
    }

    private SendMessage createAskConfigProfileMessage(Long chatId, Language lang) {
        List<List<InlineKeyboardButton>> actions = new ArrayList<>();
        List<InlineKeyboardButton> actionsLine1 = new ArrayList<>();
        List<InlineKeyboardButton> actionsLine2 = new ArrayList<>();
        List<InlineKeyboardButton> actionsLine3 = new ArrayList<>();
        actionsLine1.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("change_age_btn_msg", null, new Locale(lang.name())))
                .callbackData(ASK_AGE.name()).build());
        actionsLine2.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("change_city_btn_msg", null, new Locale(lang.name())))
                .callbackData(ASK_CITY.name()).build());
        actionsLine3.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("change_gender_btn_msg", null, new Locale(lang.name())))
                .callbackData(ASK_GENDER.name()).build());
        actions.add(actionsLine1);
        actions.add(actionsLine2);
        actions.add(actionsLine3);

        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("ask_what_change_msg", null, new Locale(lang.name())))
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
                .text(messageSource.getMessage("ask_secret_friend_age_msg", null, new Locale(lang.name())))
                .build();
    }

    public SendMessage createIncorrectSecretFriendAgeMessage(Long chatId, Language lang) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("incorrect_secret_friend_age_msg", null, new Locale(lang.name())))
                .build();
    }

    public SendMessage createAskSecretFriendCityMessage(Long chatId, Language lang) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("ask_secret_friend_city_msg", null, new Locale(lang.name())))
                .build();
    }

    public SendMessage createAskSecretFriendGenderMessage(Long chatId, Language lang) {
        List<List<InlineKeyboardButton>> genderButtons = new ArrayList<>();
        List<InlineKeyboardButton> genderButtonsLine1 = new ArrayList<>();
        genderButtonsLine1.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("man_gender_answer_msg", null, new Locale(lang.name())))
                .callbackData(Gender.MALE.name()).build());
        genderButtonsLine1.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("woman_gender_answer_msg", null, new Locale(lang.name())))
                .callbackData(Gender.FEMALE.name()).build());
        genderButtonsLine1.add(InlineKeyboardButton.builder()
                .text(messageSource.getMessage("any_gender_answer_msg", null, new Locale(lang.name())))
                .callbackData(Gender.ANY.name()).build());
        genderButtons.add(genderButtonsLine1);

        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(genderButtons);

        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(messageSource.getMessage("ask_secret_Friend_gender_msg", null, new Locale(lang.name())))
                .replyMarkup(markupKeyboard)
                .build();
    }
}

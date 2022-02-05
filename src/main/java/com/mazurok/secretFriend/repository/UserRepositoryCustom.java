package com.mazurok.secretFriend.repository;

import com.mazurok.secretFriend.exceptions.IllegalInputException;
import com.mazurok.secretFriend.repository.entity.UserEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface UserRepositoryCustom {
    UserEntity findRandomUserByIdNot(Long id);
}

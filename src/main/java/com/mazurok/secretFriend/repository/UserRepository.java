package com.mazurok.secretFriend.repository;

import com.mazurok.secretFriend.repository.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<UserEntity, Long> {

}

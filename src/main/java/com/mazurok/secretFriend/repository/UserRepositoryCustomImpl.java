package com.mazurok.secretFriend.repository;

import com.mazurok.secretFriend.exceptions.IllegalInputException;
import com.mazurok.secretFriend.repository.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SampleOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryCustomImpl implements UserRepositoryCustom {
    private final MongoTemplate mongoTemplate;

    @Autowired
    public UserRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    //TODO: find only available user
    @Override
    public UserEntity findRandomUserByIdNot(Long id) {
        SampleOperation matchStage = Aggregation.sample(1);
        MatchOperation removeUser = Aggregation.match(Criteria.where("_id").ne(id));
        Aggregation aggregation = Aggregation.newAggregation(removeUser, matchStage);
        AggregationResults<UserEntity> output = mongoTemplate.aggregate(aggregation, "users", UserEntity.class);
        return output.getUniqueMappedResult();
    }
}

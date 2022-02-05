package com.marketingshop.web.repository;

import com.marketingshop.web.entity.Subscription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SubscriptionRepositoryTest {
    @Autowired
    SubscriptionRepository subscriptionRepository;
    @Autowired
    UserRepository userRepository;

    @Test
    @Transactional
    void findBySubs_idAndUser() {
        Optional<Subscription> test = subscriptionRepository.findBySubsidAndUser(7426601l, userRepository.getById(5l));
        System.out.println(test.get());
    }
}
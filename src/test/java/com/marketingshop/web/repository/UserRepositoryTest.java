package com.marketingshop.web.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserRepositoryTest {
    @Autowired
    UserRepository userRepository;

    @Test
    void findLastId() {
        Long lastid = userRepository.findLastId();
    }

    @Test
    void test() {
        String str = "abcdabcda";
        int min = Math.min(str.length(),8);
    }
}
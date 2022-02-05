package com.marketingshop.web.repository;

import com.marketingshop.web.entity.ServiceList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class ServiceListRepositoryTest {

    @Autowired
    ServiceListRepository serviceListRepository;

    @Test
    void findDistinctCategory() {
        List<String> testval = serviceListRepository.findDistinctCategory();
        System.out.println(testval);
    }

    @Test
    void findByCategory() {
        List<ServiceList> testlist = serviceListRepository.findByCategory("Facebook Post Likes");
        System.out.println(testlist);
    }

    @Test
    void test() {
        String test = "acbCDefg";
        String target = "cd";
        int idx = test.toLowerCase().indexOf(target);
        System.out.println(idx);
        System.out.println(test.substring(idx));
    }

}
package com.marketingshop.web.repository;

import com.marketingshop.web.entity.OrderStatus;
import com.marketingshop.web.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SpringBootTest
class OrderStatusRepositoryTest {
    @Autowired
    OrderStatusRepository orderStatusRepository;
    @Autowired
    UserRepository userRepository;

    @Test
    void findByUserId() {
        List<OrderStatus> orderstatuslist = orderStatusRepository.findByUserId(5l);
        System.out.println(orderstatuslist);
    }

    @Test
    void test(){
        /*Pageable
        orderStatusRepository.findAll(pa)*/

    }

    @Test
    void findByUser() {
        Pageable pageable = PageRequest.of(2-1,2, Sort.by("orderid").descending());
        User user = userRepository.getById(5L);
        Page<OrderStatus> test = orderStatusRepository.findByUser(user, pageable);
        for (OrderStatus t: test) {
            System.out.println(t);
        }

        System.out.println(test.get().collect(Collectors.toList()));
    }

    @Test
    void findByIdAndUser() {
        /*orderStatusRepository.findById();*/

        Optional<OrderStatus> test = orderStatusRepository.findByOrderidAndUser(7479250l, userRepository.getById(5l));
        System.out.println(test);
        System.out.println(test.get());

    }
}
package com.marketingshop.web.service;

import com.marketingshop.web.entity.OrderStatus;
import com.marketingshop.web.repository.ServiceListRepository;
import com.marketingshop.web.repository.UserRepository;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class OrderStatusServiceTest {
    @Autowired
    OrderStatusService orderStatusService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ServiceListRepository serviceListRepository;

    @Test
    void getMultiOrderStatusList() {
        /*try {
            *//*orderStatusService.getOrderStatusList(5l);*//*
            orderStatusService.getMultiOrderStatusList(5l);
        } catch (ParseException e) {
            e.printStackTrace();
        }*/
    }

    @Test
    void justTest(){
        String str = "he,ll,o,";
        if (str.contains(",")){
            System.out.println("진입");
            str = str.replace(",","");};
        System.out.println(str);

    }

    @Test
    void getOrderStatus() {
        try {
            List<OrderStatus> orderStatus = orderStatusService.getOrderStatusBySubs(7426601l,userRepository.getById(5l),serviceListRepository.findByService("3329").get());
            System.out.println("최종 결과물 : "+orderStatus);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testGetOrderStatus() throws ParseException {
        orderStatusService.getOrderStatus(7479250l);
    }
}
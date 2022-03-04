package com.marketingshop.web.service;

import com.marketingshop.web.config.ExternalProperties;
import com.marketingshop.web.entity.PaymentData;
import com.marketingshop.web.entity.User;
import com.marketingshop.web.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

@Service
@Slf4j
public class DepositService {
    @Autowired
    private ServiceListRepository serviceListRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private OrderStatusRepository orderStatusRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private PaymentDataRepository paymentDataRepository;

    @Autowired
    private WebClient webClient;
    @Autowired
    private ExternalProperties externalProperties;


    @Transactional(rollbackFor = Exception.class)
    public Page<PaymentData> getMultiPayment(String privateid, Pageable pageable) {
        User user = userRepository.findByPrivateid(privateid).get();
        Page<PaymentData> paymentList = paymentDataRepository.findByUser(user, pageable);

        for (PaymentData payment: paymentList) {
            if (payment.getStatus().equals("대기") && payment.getDate().plusHours(12l).isBefore(LocalDateTime.now()))
                payment.setStatus("취소");
        }

        return paymentDataRepository.findByUser(user, pageable);
    }

}
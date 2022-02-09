package com.marketingshop.web.service;

import com.marketingshop.web.entity.ServiceList;
import com.marketingshop.web.entity.Subscription;
import com.marketingshop.web.entity.User;
import com.marketingshop.web.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SubscriptionService {
    @Autowired
    private ServiceListRepository serviceListRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private OrderStatusRepository orderStatusRepository;
    @Autowired
    private OrderStatusService orderStatusService;
    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private WebClient webClient;
    private String apiKey = "9ad7be959340d16c54fb19ca200722ac";


    @Transactional(rollbackFor = Exception.class)
    public Subscription getSubscription(Long orderid, User user, ServiceList serviceList) throws ParseException {
        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("key", apiKey);
        params.add("action", "status");
        params.add("order", String.valueOf(orderid));

        System.out.println("상태확인 직전");

        String subsTemp = webClient.post().uri("/api/v2")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(params)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        System.out.println(subsTemp);

        JSONParser jsonParser = new JSONParser();
        JSONObject subsTempJson = (JSONObject)jsonParser.parse(subsTemp);

        Subscription subscription;
        if (subscriptionRepository.findById(orderid).isPresent())
            subscription = subscriptionRepository.findById(orderid).get();
        else subscription = new Subscription();
        System.out.println("service끝나기직전");

        return subscription.update(subsTempJson, user, serviceList, orderid);
    }

    @Transactional(rollbackFor = Exception.class)
    public Page<Subscription> getMultiSubscriptionListBySearch(String privateid, String search, Pageable pageable){ //구해야하는 orderid묶고 보내고 받고, id마다 구데이터에 신규데이터 입힘
        User user = userRepository.findByPrivateid(privateid).get();
        Page<Subscription> subscriptionList = subscriptionRepository.findByUserAndUsernameContaining(user, search, pageable);
        List<String> subsids = new ArrayList<>();
        for (Subscription subscription: subscriptionList) {
            if (subscription.getStatus().equals("Completed") || subscription.getStatus().equals("Canceled")) continue;
            String subsid = String.valueOf(subscription.getSubsid());
            subsids.add(subsid);
        }

        String subsidsComma = String.join(",",subsids);
        if (subsidsComma.isEmpty()) return subscriptionList;

        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("key", apiKey);
        params.add("action", "status");
        params.add("orders", subsidsComma);


        String orderStatuses = webClient.post().uri("/api/v2")
                .bodyValue(params)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONParser jsonParser = new JSONParser();
        try {
            JSONObject orderStatusJsons = (JSONObject)jsonParser.parse(orderStatuses);
            for (String subsid : subsids) {
                Long id = Long.valueOf(subsid);
                Subscription subscription = subscriptionRepository.findById(id).get();
                JSONObject orderStatusJson = (JSONObject) orderStatusJsons.get(subsid);
                Subscription updated = subscription.update(orderStatusJson, user, subscription.getServiceList(), id);

                String newStatus = (String) orderStatusJson.get("status");
                if (subscription.equals("Active") || newStatus.equals("Canceled")){
                    //남은갯수 환불처리
                    Subscription subs = subscriptionRepository.getById(id);
                    int remain = Integer.parseInt(subs.getPosts()) - Integer.parseInt(subs.getUse_posts());
                    int refund = Integer.parseInt(subs.getCharge().replace(",", "")) * remain / Integer.parseInt(subs.getPosts());
                    user.setBalance(user.getBalance() + refund);
                    log.info("{}님이 subs {}를 취소하여 {}만큼 환불받았습니다.",user.getPrivateid(),subsid,refund);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return subscriptionRepository.findByUserAndUsernameContaining(user, search, pageable);
    }

    @Transactional(rollbackFor = Exception.class)
    public Page<Subscription> getMultiSubscriptionListByStatusAndSearch(String privateid, String status, String search, Pageable pageable){ //구해야하는 orderid묶고 보내고 받고, id마다 구데이터에 신규데이터 입힘
        User user = userRepository.findByPrivateid(privateid).get();
        Page<Subscription> subscriptionList = subscriptionRepository.findByUserAndStatusAndUsernameContaining(user, status, search, pageable);
        List<String> subsids = new ArrayList<>();
        for (Subscription subscription: subscriptionList) {
            if (subscription.getStatus().equals("Completed") || subscription.getStatus().equals("Canceled")) continue;
            String subsid = String.valueOf(subscription.getSubsid());
            subsids.add(subsid);
        }

        String subsidsComma = String.join(",",subsids);
        if (subsidsComma.isEmpty()) return subscriptionList;

        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("key", apiKey);
        params.add("action", "status");
        params.add("orders", subsidsComma);


        String orderStatuses = webClient.post().uri("/api/v2")
                .bodyValue(params)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONParser jsonParser = new JSONParser();
        try {
            JSONObject orderStatusJsons = (JSONObject)jsonParser.parse(orderStatuses);
            for (String subsid : subsids) {
                Long id = Long.valueOf(subsid);
                Subscription subscription = subscriptionRepository.findById(id).get();
                JSONObject orderStatusJson = (JSONObject) orderStatusJsons.get(subsid);
                Subscription updated = subscription.update(orderStatusJson, user, subscription.getServiceList(), id);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return subscriptionRepository.findByUserAndStatusAndUsernameContaining(user, status, search, pageable);
    }
}


/*@Transactional(rollbackFor = Exception.class)
    public List<OrderStatus> getOrderStatusList(Long userid) {
        List<OrderStatus> orderStatusList = orderStatusRepository.findByUserId(userid);
        List<String> orderids = new ArrayList<>();
        for (OrderStatus orderStatus: orderStatusList){
            if (orderStatus.getStatus()=="Completed") continue;
            String orderid = String.valueOf(orderStatus.getOrderid());
            orderids.add(orderid);
        }
        String orderidsComma = String.join(",",orderids);
        System.out.println(orderidsComma);

        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("key", apiKey);
        params.add("action", "status");
        params.add("orders", orderidsComma);

        OrderStatusTemp orderStatusTemp = webClient.post().uri("/api/v2")
                .bodyValue(params)
                .retrieve()
                .bodyToFlux(OrderStatusTemp.class)
                .collectMap()
                .block();


        return orderStatusList;
    }*/
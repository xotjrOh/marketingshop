package com.marketingshop.web.service;

import com.marketingshop.web.entity.OrderStatus;
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
public class OrderStatusService {
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
    private WebClient webClient;
    private String apiKey = "9ad7be959340d16c54fb19ca200722ac";

    @Transactional(rollbackFor = Exception.class)
    public OrderStatus getOrderStatus(Long orderid) throws ParseException {
        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("key", apiKey);
        params.add("action", "status");
        params.add("order", String.valueOf(orderid));

        String orderStatusTemp = webClient.post().uri("/api/v2")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(params)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        System.out.println(orderStatusTemp);
        /*ObjectMapper objectMapper = new ObjectMapper();
        OrderStatusTemp test = objectMapper.readValue(orderStatusTemp, OrderStatusTemp.class);
        log.info("{} 잘 생성됨",test.toString());*/

        JSONParser jsonParser = new JSONParser();
        JSONObject orderStatusTempJson = (JSONObject)jsonParser.parse(orderStatusTemp);

        OrderStatus orderStatus;
        if (orderStatusRepository.findById(orderid).isPresent())
            orderStatus = orderStatusRepository.findById(orderid).get();
        else orderStatus = new OrderStatus(); //이게 작동할 일이있나, 작동하면 user랑 service객체 없을듯

        return orderStatus.update(orderStatusTempJson,orderid);
    }

    @Transactional(rollbackFor = Exception.class) //1페이지에 모든걸 호출할거라서.
    public List<OrderStatus> getOrderStatusBySubs(Long subsid, User user, ServiceList serviceList) throws ParseException {
        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("key", apiKey);
        params.add("action", "status");
        params.add("order", String.valueOf(subsid));

        String orderStatusTemp = webClient.post().uri("/api/v2") //{"status":"Canceled","expiry":null,"posts":"3","orders":["7432220","7436923","7448363"]}
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(params)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        JSONParser jsonParser = new JSONParser();
        JSONObject orderStatusTempJson = (JSONObject)jsonParser.parse(orderStatusTemp);

        List<String> orderidList = (List)orderStatusTempJson.get("orders");
        MultiValueMap<String,String> paramss = new LinkedMultiValueMap<>();
        paramss.add("key", apiKey);
        paramss.add("action", "status");
        paramss.add("orders", String.join(",", orderidList));


        String orderStatuses = webClient.post().uri("/api/v2")
                .bodyValue(paramss)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        Subscription subscription = subscriptionRepository.findById(subsid).get();
        try {
            JSONObject orderStatusJsons = (JSONObject)jsonParser.parse(orderStatuses);

            for (String orderid : orderidList) {
                Long id = Long.valueOf(orderid);
                JSONObject orderStatusJson = (JSONObject) orderStatusJsons.get(orderid);

                OrderStatus orderStatus;
                if (orderStatusRepository.findById(id).isPresent()) //댓글 초기화 때문에 나눠야함
                    orderStatus = orderStatusRepository.getById(id);
                else orderStatus = new OrderStatus(user, serviceList, String.valueOf(orderStatusJson.get("charge")),subscription);

                OrderStatus updated = orderStatus.update(orderStatusJson,id);
                orderStatusRepository.save(updated);
            }
            return orderStatusRepository.findBySubscriptionOrderByOrderidDesc(subscription);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    /*@Transactional(rollbackFor = Exception.class)
    public Page<OrderStatus> getMultiOrderStatusList(String privateid, Pageable pageable){ //구해야하는 orderid묶고 보내고 받고, id마다 구데이터에 신규데이터 입힘
        User user = userRepository.findByPrivateid(privateid).get();
        Page<OrderStatus> orderStatusList = orderStatusRepository.findByUser(user, pageable);
        List<String> orderids = new ArrayList<>();
        for (OrderStatus orderStatus: orderStatusList) {
            if (orderStatus.getStatus().equals("Completed") || orderStatus.getStatus().equals("Partial") || orderStatus.getStatus().equals("Canceled")) continue;
                String orderid = String.valueOf(orderStatus.getOrderid());
                orderids.add(orderid);
            }

        String orderidsComma = String.join(",",orderids);
        if (orderidsComma.isEmpty()) return orderStatusList;

        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("key", apiKey);
        params.add("action", "status");
        params.add("orders", orderidsComma);


        String orderStatuses = webClient.post().uri("/api/v2")
                .bodyValue(params)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONParser jsonParser = new JSONParser();
        try {
            JSONObject orderStatusJsons = (JSONObject)jsonParser.parse(orderStatuses);
            for (String orderid : orderids) {
                Long id = Long.valueOf(orderid);
                OrderStatus orderstatus = orderStatusRepository.findById(id).get();
                JSONObject orderStatusJson = (JSONObject) orderStatusJsons.get(orderid);
                OrderStatus updated = orderstatus.update(orderStatusJson,id);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return orderStatusRepository.findByUser(user, pageable);
    }

    @Transactional(rollbackFor = Exception.class)
    public Page<OrderStatus> getMultiOrderStatusListByStatus(String privateid, String status, Pageable pageable){ //구해야하는 orderid묶고 보내고 받고, id마다 구데이터에 신규데이터 입힘
        User user = userRepository.findByPrivateid(privateid).get();
        Page<OrderStatus> orderStatusList = orderStatusRepository.findByUserAndStatus(user, status, pageable);
        List<String> orderids = new ArrayList<>();
        for (OrderStatus orderStatus: orderStatusList) {
            if (orderStatus.getStatus().equals("Completed") || orderStatus.getStatus().equals("Partial") || orderStatus.getStatus().equals("Canceled")) continue;
            String orderid = String.valueOf(orderStatus.getOrderid());
            orderids.add(orderid);
        }

        String orderidsComma = String.join(",",orderids);
        if (orderidsComma.isEmpty()) return orderStatusList;

        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("key", apiKey);
        params.add("action", "status");
        params.add("orders", orderidsComma);


        String orderStatuses = webClient.post().uri("/api/v2")
                .bodyValue(params)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONParser jsonParser = new JSONParser();
        try {
            JSONObject orderStatusJsons = (JSONObject)jsonParser.parse(orderStatuses);
            for (String orderid : orderids) {
                Long id = Long.valueOf(orderid);
                OrderStatus orderstatus = orderStatusRepository.findById(id).get();
                JSONObject orderStatusJson = (JSONObject) orderStatusJsons.get(orderid);
                OrderStatus updated = orderstatus.update(orderStatusJson,id);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return orderStatusRepository.findByUserAndStatus(user, status, pageable);
    }*/

    @Transactional(rollbackFor = Exception.class)
    public Page<OrderStatus> getMultiOrderStatusListBySearch(String privateid, String search, Pageable pageable) {
        User user = userRepository.findByPrivateid(privateid).get();
        Page<OrderStatus> orderStatusList = orderStatusRepository.findByUserAndLinkContaining(user, search, pageable);
        List<String> orderids = new ArrayList<>();
        for (OrderStatus orderStatus: orderStatusList) {
            if (orderStatus.getStatus().equals("Completed") || orderStatus.getStatus().equals("Partial") || orderStatus.getStatus().equals("Canceled")) continue;
            String orderid = String.valueOf(orderStatus.getOrderid());
            orderids.add(orderid);
        }

        String orderidsComma = String.join(",",orderids);
        if (orderidsComma.isEmpty()) return orderStatusList;

        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("key", apiKey);
        params.add("action", "status");
        params.add("orders", orderidsComma);


        String orderStatuses = webClient.post().uri("/api/v2")
                .bodyValue(params)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONParser jsonParser = new JSONParser();
        try {
            JSONObject orderStatusJsons = (JSONObject)jsonParser.parse(orderStatuses);
            for (String orderid : orderids) {
                Long id = Long.valueOf(orderid);
                OrderStatus orderstatus = orderStatusRepository.findById(id).get();
                JSONObject orderStatusJson = (JSONObject) orderStatusJsons.get(orderid);
                OrderStatus updated = orderstatus.update(orderStatusJson,id);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return orderStatusRepository.findByUserAndLinkContaining(user, search, pageable);
    }

    @Transactional(rollbackFor = Exception.class)
    public Page<OrderStatus> getMultiOrderStatusListByStatusAndSearch(String privateid, String status, String search, Pageable pageable){ //구해야하는 orderid묶고 보내고 받고, id마다 구데이터에 신규데이터 입힘
        User user = userRepository.findByPrivateid(privateid).get();
        Page<OrderStatus> orderStatusList = orderStatusRepository.findByUserAndStatusAndLinkContaining(user, status, search, pageable);
        List<String> orderids = new ArrayList<>();
        for (OrderStatus orderStatus: orderStatusList) {
            if (orderStatus.getStatus().equals("Completed") || orderStatus.getStatus().equals("Partial") || orderStatus.getStatus().equals("Canceled")) continue;
            String orderid = String.valueOf(orderStatus.getOrderid());
            orderids.add(orderid);
        }

        String orderidsComma = String.join(",",orderids);
        if (orderidsComma.isEmpty()) return orderStatusList;

        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("key", apiKey);
        params.add("action", "status");
        params.add("orders", orderidsComma);


        String orderStatuses = webClient.post().uri("/api/v2")
                .bodyValue(params)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONParser jsonParser = new JSONParser();
        try {
            JSONObject orderStatusJsons = (JSONObject)jsonParser.parse(orderStatuses);
            for (String orderid : orderids) {
                Long id = Long.valueOf(orderid);
                OrderStatus orderstatus = orderStatusRepository.findById(id).get();
                JSONObject orderStatusJson = (JSONObject) orderStatusJsons.get(orderid);
                OrderStatus updated = orderstatus.update(orderStatusJson,id);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return orderStatusRepository.findByUserAndStatusAndLinkContaining(user, status, search, pageable);
    }

    /*@Transactional(rollbackFor = Exception.class)
    public List<OrderStatus> getMultiOrderStatusListBySubs(String privateid, Long subsid) throws ParseException { //구해야하는 orderid묶고 보내고 받고, id마다 구데이터에 신규데이터 입힘
        User user = userRepository.findByPrivateid(privateid).get();
        Subscription subscription = subscriptionRepository.getById(subsid);
        List<OrderStatus> orderStatusList = orderStatusRepository.findByUserAndSubscription(user, subscription);
        List<String> orderids = new ArrayList<>();
        for (OrderStatus orderStatus: orderStatusList) {
            if (orderStatus.getStatus().equals("Completed") || orderStatus.getStatus().equals("Partial") || orderStatus.getStatus().equals("Canceled")) continue;
            String orderid = String.valueOf(orderStatus.getOrderid());
            orderids.add(orderid);
        }

        String orderidsComma = String.join(",",orderids);
        if (orderidsComma.isEmpty()) return orderStatusList;

        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("key", apiKey);
        params.add("action", "status");
        params.add("orders", orderidsComma);


        String orderStatuses = webClient.post().uri("/api/v2")
                .bodyValue(params)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONParser jsonParser = new JSONParser();
        try {
            JSONObject orderStatusJsons = (JSONObject)jsonParser.parse(orderStatuses);
            for (String orderid : orderids) {
                Long id = Long.valueOf(orderid);
                OrderStatus orderstatus = orderStatusRepository.findById(id).get();
                JSONObject orderStatusJson = (JSONObject) orderStatusJsons.get(orderid);
                OrderStatus updated = orderstatus.update(orderStatusJson,id);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return orderStatusRepository.findByUserAndSubscription(user, subscription);
    }*/

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
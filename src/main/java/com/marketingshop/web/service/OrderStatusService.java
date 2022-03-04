package com.marketingshop.web.service;

import com.marketingshop.web.config.ExternalProperties;
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

import java.text.DecimalFormat;
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
    @Autowired
    private ExternalProperties externalProperties;

    @Transactional(rollbackFor = Exception.class)
    public OrderStatus getOrderStatus(Long orderid) throws ParseException { //addorder에서 상단 flash때매 1번 사용됨
        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("key", externalProperties.getApiKey());
        params.add("action", "status");
        params.add("order", String.valueOf(orderid));

        String orderStatusTemp = webClient.post().uri("/api/v2")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(params)
                .retrieve()
                .bodyToMono(String.class)
                .block();
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
        params.add("key", externalProperties.getApiKey());
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
        paramss.add("key", externalProperties.getApiKey());
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
                String newStatus = (String) orderStatusJson.get("status");

                OrderStatus orderStatus;
                boolean chk = false; //상태변화 있었거나 새삥이면 true
                if (orderStatusRepository.findById(id).isPresent()) { //댓글 초기화 때문에 나눠야함
                    orderStatus = orderStatusRepository.getById(id);
                    if (!orderStatus.getStatus().equals(newStatus))
                        chk=true;
                } else {
                    orderStatus = new OrderStatus(user, serviceList, String.valueOf(orderStatusJson.get("charge")),subscription);
                    chk =true;
                }

                OrderStatus updated = orderStatus.update(orderStatusJson,id);
                OrderStatus saved = orderStatusRepository.save(updated);
                saved.setCreatedate("0000년 00월 00일 00시 00분 00초");

                if (chk) { //환불여부 체크
                    if (newStatus.equals("Partial") || newStatus.equals("Canceled")) {
                        //새로운 가격 저장
                        int remainQuantity = Integer.parseInt(updated.getRemains());
                        int refund = remainQuantity * serviceList.getPrice() / 1000;

                        user.setBalance(user.getBalance()+refund);
                        saved.setCharge(String.valueOf( Integer.parseInt(saved.getCharge().replace(",","")) - refund ));
                        log.info("{}가 subsDetail 조회중 {} 상품으로 {} 만큼 환불받았습니다",user.getPrivateid(),id,refund);
                    }
                }

            }
            return orderStatusRepository.findBySubscriptionOrderByOrderidDesc(subscription);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

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
        params.add("key", externalProperties.getApiKey());
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
                int oldprice = Integer.parseInt(orderstatus.getCharge().replace(",",""));

                JSONObject orderStatusJson = (JSONObject) orderStatusJsons.get(orderid);
                String NewStatus = (String) orderStatusJson.get("status");
                if (NewStatus.equals("Partial")) {
                    //새로운 가격 저장
                    DecimalFormat decFormat = new DecimalFormat("###,###");
                    String chargeSTR = String.valueOf(orderStatusJson.get("charge"));
                    int kor_charge = (int) ((Float.parseFloat(chargeSTR))*2200);
                    String charge = decFormat.format(kor_charge);
                    orderstatus.setCharge(charge);

                    int refund = oldprice-kor_charge;
                    user.setBalance(user.getBalance()+refund);
                    log.info("{}가 orders 조회중 partial로 {} 상품으로 {} 만큼 환불받았습니다",user.getPrivateid(),id,refund);
                } else if (NewStatus.equals("Canceled")) {
                    orderstatus.setCharge("0");

                    user.setBalance(user.getBalance()+oldprice);
                    log.info("{}가 orders 조회중 canceled로 {} 상품으로 {} 만큼 환불받았습니다",user.getPrivateid(),id,oldprice);
                }

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
        List<OrderStatus> orderStatusList = orderStatusRepository.findByUserAndLinkContaining(user,search);//orderStatusRepository.findByUserAndStatusAndLinkContaining(user, status, search, pageable);
        List<String> orderids = new ArrayList<>();
        for (OrderStatus orderStatus: orderStatusList) {
            if (orderStatus.getStatus().equals("Completed") || orderStatus.getStatus().equals("Partial") || orderStatus.getStatus().equals("Canceled")) continue;
            String orderid = String.valueOf(orderStatus.getOrderid());
            orderids.add(orderid);
        }

        String orderidsComma = String.join(",",orderids);
        if (orderidsComma.isEmpty()) return orderStatusRepository.findByUserAndStatusAndLinkContaining(user, status, search, pageable);

        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("key", externalProperties.getApiKey());
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
                int oldprice = Integer.parseInt(orderstatus.getCharge().replace(",",""));

                JSONObject orderStatusJson = (JSONObject) orderStatusJsons.get(orderid);
                String NewStatus = (String) orderStatusJson.get("status");
                if (NewStatus.equals("Partial")) {
                    //새로운 가격 저장
                    DecimalFormat decFormat = new DecimalFormat("###,###");
                    String chargeSTR = String.valueOf(orderStatusJson.get("charge"));
                    int kor_charge = (int) ((Float.parseFloat(chargeSTR))*2200);
                    String charge = decFormat.format(kor_charge);
                    orderstatus.setCharge(charge);

                    int refund = oldprice-kor_charge;
                    user.setBalance(user.getBalance()+refund);
                    log.info("{}가 orders status 조회중 partial로 {} 상품으로 {} 만큼 환불받았습니다",user.getPrivateid(),id,refund);
                } else if (NewStatus.equals("Canceled")) {
                    orderstatus.setCharge("0");

                    user.setBalance(user.getBalance()+oldprice);
                    log.info("{}가 orders status 조회중 canceled로 {} 상품으로 {} 만큼 환불받았습니다",user.getPrivateid(),id,oldprice);
                }

                OrderStatus updated = orderstatus.update(orderStatusJson,id);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return orderStatusRepository.findByUserAndStatusAndLinkContaining(user, status, search, pageable);
    }

}
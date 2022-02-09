package com.marketingshop.web.entity;

import com.marketingshop.web.dto.OrderForm;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import javax.persistence.*;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    @Id
    private Long subsid;

    @ManyToOne
    @JoinColumn(name = "service_id") //FK 명칭
    private ServiceList serviceList;

    @OneToMany(mappedBy = "subscription")
    private List<OrderStatus> orderStatuses = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String status;
    private String expiry;
    private String use_posts; //smm에서 받은 사용값

    private String username;
    private String min;
    private String max;
    private String posts; //입력한 전체 게시물수
    private String charge;

    private String use_posts_tag; //구현 기능 필요
    private String functag;

    private String createdate;
    private String updatedate;

    @PrePersist
    public void cretedAt(){
        LocalDateTime now = LocalDateTime.now();
        String formatedNow = now.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초"));
        this.createdate = formatedNow;
    }

    @PreUpdate
    public void updatedAt(){
        LocalDateTime now = LocalDateTime.now();
        String formatedNow = now.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초"));
        this.updatedate = formatedNow;
    }

    public Subscription update(JSONObject json, User userData, ServiceList serviceListData, Long subsid) throws ParseException {
        serviceList = serviceListData;
        user = userData;

        System.out.println(json);
        this.subsid = subsid;

        status = (String) json.get("status");
        expiry = (String) json.get("expiry");
        use_posts = (String) json.get("posts"); //처음이라 무조건 0

        if (status.equals("Active"))
            functag = "<a href='/subscriptions/stop/"+subsid+"' class='btn btn-danger btn-sm'>취소</a>";
        else if (status.equals("Completed"))
            functag = "<a href='/subscriptions/reorder/"+subsid+"' class='btn btn-success btn-sm'>재주문</a>";
        else if (status.equals("Canceled"))
            functag = "<a href='/subscriptions/reorder/"+subsid+"' class='btn btn-success btn-sm'>재주문</a>";
        else
            functag = "<div> html 글자 오타 찾아라</div>";

        if (use_posts.equals("0"))
            use_posts_tag = "0";
        else
            use_posts_tag = "<a href='/customer/orders/subs/"+subsid+"'>"+use_posts+"</a>";

/*        System.out.println(this);
        for (Object OBJorderid : (JSONArray)json.get("orders")) {
            System.out.println(OBJorderid);
            Long orderid = Long.valueOf(String.valueOf(OBJorderid));
            System.out.println("orderid : " + orderid);

            OrderStatus orderStatus = orderStatusService.getOrderStatusBySubs(orderid, user, serviceList);

            orderStatus.setSubscription(this);
            getOrderStatuses().add(orderStatus); //이로써 양방향 관계의 성립이다

            System.out.println("추가가 문제인가");

            orderStatusRepository.save(orderStatus);
            System.out.println("저장이 문제인가");

        }*/

        return this; //죄다 저장하고 orderStatus도 값 두개 쌔삥으로 생성
    }

    public Subscription inputValueUpdate(ServiceList serviceListData, OrderForm orderForm) { //나중에 뷰에서 완료상태면 다르게 표시되게
        username = orderForm.getUsername();
        min = orderForm.getMin();
        max = orderForm.getMax();
        posts = orderForm.getPosts();

        DecimalFormat decFormat = new DecimalFormat("###,###");

        charge = decFormat.format(serviceListData.getPrice() * Integer.parseInt(orderForm.getQuantity()) / 1000);

        return this;
    }
}

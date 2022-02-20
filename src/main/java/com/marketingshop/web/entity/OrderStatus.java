package com.marketingshop.web.entity;

import com.marketingshop.web.dto.OrderForm;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.json.simple.JSONObject;

import javax.persistence.*;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatus{
    @Id
    private Long orderid;

    @ManyToOne
    @JoinColumn(name = "service_id") //FK 명칭
    private ServiceList serviceList;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne
    @JoinColumn(name = "comment_id")
    private Comment comment; //별점줬는지

    @ManyToOne
    @JoinColumn(name = "subs_id")
    private Subscription subscription;

    private String link;
    private String quantity;
    private String comments;//처음에는 이 5개만 저장

    private String charge;
    private String start_count;
    private String status;
    private String remains;
    private String currency; // 이 5개는 orders호출시 불러와짐

    private String createdate;

    private String status_html;

    @PrePersist
    public void cretedAt(){
        LocalDateTime now = LocalDateTime.now();
        String formatedNow = now.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초"));
        this.createdate = formatedNow;
    }

    public OrderStatus(User user, ServiceList serviceList, String chargeSTR, Subscription subscription) {
        this.user = user;
        this.serviceList = serviceList;
        DecimalFormat decFormat = new DecimalFormat("###,###");

        int kor_charge = (int) ((Float.parseFloat(chargeSTR))*2200);
        /*charge = decFormat.format(kor_charge);*/
        charge = "선차감";

        quantity = decFormat.format(kor_charge*1000/serviceList.getPrice());

        link = "상세내역 필요시 주문번호와 함께 instamarketingshop@gmail.com로 문의";

        this.subscription = subscription;
    }

    public OrderStatus update(JSONObject json, Long orderid) { //update도 따로 만듬
        /*int kor_charge = (int) (Float.parseFloat(String.valueOf(json.get("charge")))*2200);
        charge = String.valueOf(kor_charge);*/
        this.orderid = orderid;

        start_count = (String) json.get("start_count");
        if (start_count==null)
            start_count = "---";

        status = (String) json.get("status");
        remains = (String) json.get("remains");
        remains = remains.replace("-","+");
        currency = (String) json.get("currency");

        if (status.equals("Pending"))
            status_html = "<button class='btn btn-lg btn-dark'><span> 대기중 </span><i class='fas fa-clock ml-1'></i></button>";
        else if (status.equals("In progress"))
            status_html = "<button class='btn btn-lg btn-warning'><span> 진행중 </span><i class='fas fa-hourglass-start ml-1'></i></button>";
        else if (status.equals("Completed"))
            status_html = "<button class='btn btn-lg btn-success'><span> 완료 </span><i class='fas fa-check ml-1'></i></button>";
        else if (status.equals("Partial"))
            status_html = "<button class='btn btn-lg btn-danger'><span> 부분공급 </span><i class='fas fa-percent ml-1'></i></button>";
        else if (status.equals("Processing"))
            status_html = "<button class='btn btn-lg btn-warning'><span> 작업지연 </span><i class='fas fa-cog ml-1'></i></button>";
        else if (status.equals("Canceled"))
            status_html = "<button class='btn btn-lg btn-secondary'><span> 취소 </span><i class='fas fa-ban ml-1'></i></button>";
        else
            status_html = "<div> html 글자 오타 찾아라</div>";

        return this;
    }

    public OrderStatus inputValueUpdate(User userData, ServiceList serviceListData, OrderForm orderForm, int price) { //orderForm.charge 업데이트해야함 ㅇㅇ 콤마 제거해서
        user = userData;
        serviceList = serviceListData;

        DecimalFormat decFormat = new DecimalFormat("###,###");
        if (orderForm.getType().equals("12")){ //{{!12 Default 2개}}
            link = orderForm.getLink();
            quantity = decFormat.format(Integer.parseInt(orderForm.getQuantity()));
        } else if (orderForm.getType().equals("14")){ //{{!14 Custom Comments Package 2개}}
            link = orderForm.getLink();

            quantity = "1";
            comments = orderForm.getComments();
        } else if (orderForm.getType().equals("2")){ //{{!2 Custom Comments 3개}}
            link = orderForm.getLink();

            /*String LINE_SEPERATOR=System.getProperty("line.separator");
            quantity = String.valueOf(orderForm.getComments().split(LINE_SEPERATOR).length);*/ //1000개를 넘기지 않을듯
            quantity = orderForm.getQuantity();
            comments = orderForm.getComments();
        } else if (orderForm.getType().equals("10")){ //{{!10 Package 1개}}
            link = orderForm.getLink();
            quantity = "1";
        }

        charge = decFormat.format(price);

        return this;
    }
}

package com.marketingshop.web.dto;

import lombok.*;

//현재 안쓰는중
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderStatusTemp { //현재 사용되는 곳 없는듯

    private Float charge;
    private Long start_count;
    private String status;
    private Long remains;
    private String currency; // 이 5개는 orders호출시 불러와짐

}

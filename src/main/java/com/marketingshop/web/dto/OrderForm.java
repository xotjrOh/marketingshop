package com.marketingshop.web.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderForm {

    /*private String "OrderForm[user_name]";*/

    private String category;
    private String type;
    private String privateid;
    private String charge;

    private String service;
    private String link;
    private String quantity;
    private String comments;

    private String username;
    private String min;
    private String max;
    private String posts;

    private String comment_username;

    /*private String delay;
    private String expiry;*/

}

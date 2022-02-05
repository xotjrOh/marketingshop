package com.marketingshop.web.dto;

import com.marketingshop.web.entity.Comment;
import com.marketingshop.web.entity.OrderStatus;
import com.marketingshop.web.entity.ServiceList;
import com.marketingshop.web.entity.User;
import com.marketingshop.web.repository.ServiceListRepository;
import com.marketingshop.web.repository.UserRepository;
import lombok.*;

import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class CommentDTO {

    //private Long id;
    private int rate;
    private String content;

    private String servicenum;
    private String privateid;


    public Comment toEntity(ServiceListRepository serviceListRepository, UserRepository userRepository, OrderStatus orderStatus){
        Optional<ServiceList> serviceList = serviceListRepository.findByService(servicenum);
        /*if (!serviceList.isPresent()) //서비스가 없음
            return null;*/
        Optional<User> user = userRepository.findByPrivateid(privateid);
        /*if (!serviceList.isPresent()) //회원정보가 없음
            return null;*/

        int min=Math.min(content.length(),100);

        return new Comment(null,serviceList.get(),user.get(),orderStatus,rate,content.substring(0,min),null,null);
    }
}

package com.marketingshop.web.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.marketingshop.web.dto.CommentDTO;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "service_id") //FK 명칭
    private ServiceList serviceList;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; //nickname을 get하기 위한, google_123456

    @OneToOne(mappedBy = "comment")
    @JsonIgnore
    private OrderStatus orderStatus;

    @NotNull
    private int star;
    @NotNull
    private String content;

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

    public Comment update(CommentDTO commentDTO) {
        star = commentDTO.getRate();

        String contentTemp = commentDTO.getContent();
        int min=Math.min(contentTemp.length(),100);
        content = contentTemp.substring(0,min);

        return this;
    }

}

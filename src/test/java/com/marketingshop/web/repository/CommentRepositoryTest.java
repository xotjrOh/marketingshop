package com.marketingshop.web.repository;

import com.marketingshop.web.entity.Comment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CommentRepositoryTest {

    @Autowired
    CommentRepository commentRepository;
    @Autowired
    ServiceListRepository serviceListRepository;

    @Test
    void findByServiceNum() {
        List<Comment> commentList = commentRepository.findByServiceNum("3329");
        if (commentList.isEmpty())
            System.out.println("비었다");
        else{
            for(Comment comment: commentList)
                System.out.println(comment);
        }
    }

    @Test
    void findAVGByServiceNum() {
        Float avg = commentRepository.findAVGByServiceNum("3329");
        System.out.println(Math.round(avg*10));
        System.out.println(Math.round(avg*10)/10.0);
    }

    @Test
    void test(){
        List<Comment> commentList = commentRepository.findByServiceListOrderByIdDesc(serviceListRepository.getById("3329"));
        for(Comment comment: commentList)
            System.out.println(comment.toString());
    }
}
package com.marketingshop.web.repository;

import com.marketingshop.web.entity.Comment;
import com.marketingshop.web.entity.ServiceList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment,Long> {
    @Override
    List<Comment> findAll();

    @Query(value = "SELECT * FROM comment WHERE service_id = :serviceNum", nativeQuery = true)
    List<Comment> findByServiceNum(String serviceNum);

    @Query(value = "SELECT AVG(c.star) FROM Comment c WHERE service_id = ?1")
    Float findAVGByServiceNum(String serviceNum);

    List<Comment> findByServiceListOrderByIdDesc(ServiceList serviceList);
}

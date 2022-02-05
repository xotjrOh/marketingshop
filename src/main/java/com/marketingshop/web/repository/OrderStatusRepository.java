package com.marketingshop.web.repository;

import com.marketingshop.web.entity.OrderStatus;
import com.marketingshop.web.entity.Subscription;
import com.marketingshop.web.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderStatusRepository extends JpaRepository<OrderStatus,Long> {

    @Query(value = "SELECT * FROM orderstatus WHERE user_id = :userid order by orderid desc", nativeQuery = true)
    List<OrderStatus> findByUserId(Long userid);

    List<OrderStatus> findBySubscriptionOrderByOrderidDesc(Subscription subscription);

    List<OrderStatus> findByUser(User user);

    Page<OrderStatus> findByUser(User user, Pageable pageable);

    Page<OrderStatus> findByUserAndStatus(User user, String status, Pageable pageable);

    Optional<OrderStatus> findByOrderidAndUser(Long orderid, User user);

    /*Optional<OrderStatus> findByServiceListAndUser(ServiceList serviceList, User user);*/
}

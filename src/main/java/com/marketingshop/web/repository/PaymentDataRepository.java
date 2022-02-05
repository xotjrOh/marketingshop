package com.marketingshop.web.repository;

import com.marketingshop.web.entity.PaymentData;
import com.marketingshop.web.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentDataRepository extends JpaRepository<PaymentData,Long> {
    Page<PaymentData> findByUser(User user, Pageable pageable);

    @Query("select sum(p.money) from PaymentData p where p.user = ?1")
    Long amountSum(User user);
}

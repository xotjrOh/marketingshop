package com.marketingshop.web.repository;

import com.marketingshop.web.entity.Subscription;
import com.marketingshop.web.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription,Long> {

    Optional<Subscription> findBySubsidAndUser(Long subsid, User user);

    Page<Subscription> findByUser(User user, Pageable pageable);

    Page<Subscription> findByUserAndStatus(User user, String status, Pageable pageable);
}

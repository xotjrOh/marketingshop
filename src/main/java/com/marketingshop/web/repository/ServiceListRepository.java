package com.marketingshop.web.repository;

import com.marketingshop.web.entity.ServiceList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


public interface ServiceListRepository extends JpaRepository<ServiceList, String> {

    @Query(value = "select distinct category from servicelist",nativeQuery = true)
    List<String> findDistinctCategory();

    @Query(value = "select * from servicelist where category = ?1 order by sequence",nativeQuery = true)
    List<ServiceList> findByCategory(String category);

    @Query(value = "select * from servicelist where category = ?1 order by timetocomplete desc",nativeQuery = true)
    List<ServiceList> findByCategoryOrderByTime(String category);

    Optional<ServiceList> findByService(String service);

}
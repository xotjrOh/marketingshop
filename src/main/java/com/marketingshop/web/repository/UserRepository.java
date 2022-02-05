package com.marketingshop.web.repository;

import com.marketingshop.web.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

//이미 가입한 사용자인지 확인할 목적/ repository는 entity의 영속성을 관장함
//@Repository //해당 어노테이션 없어도 됨 CRUD들고잇음.
public interface UserRepository extends JpaRepository<User, Long> {
	//findBy규칙->Email은 문법
	//select * from user where username =? 로 호출된다.
	//이런걸 Jpa Query Methods 라고 부른다.
	Optional<User> findByPrivateid(String privateid);
	Optional<User> findByNickname(String nickname);

	@Query(value = "select id from user order by id desc limit 1", nativeQuery = true)
	Long findLastId();

	@Query(value = "select id from user where privateid = :privateid", nativeQuery = true)
	Long findIdByPrivateId(String privateid);
}
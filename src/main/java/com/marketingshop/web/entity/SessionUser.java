package com.marketingshop.web.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

//인증된 사용자 정보/ httpsession에 넣을것임
@Getter
@Setter
@NoArgsConstructor
public class SessionUser implements Serializable {
	
	private static final long serialVersionUID = 1L; //노란 경고없애려고 넣음. 문제되면 주석처리할거. 이거 없으면 자동으로 값할당하는데 문제될수도있다함
	
	private String name;//오태석
	private String privateid;//google-123456
	private String email;
	private String picture;
	private String nickname;
	private int balance;

	public SessionUser(User user) {
		this.name = user.getName();
		this.privateid = user.getPrivateid();
		this.email = user.getEmail();
		this.picture = user.getPicture();
		this.nickname = user.getNickname();
		this.balance = user.getBalance();
	}
	
}

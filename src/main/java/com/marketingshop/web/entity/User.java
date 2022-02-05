package com.marketingshop.web.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity //provider google providerId 10974...686
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@NotNull
	private String name; //오태석

	@NotNull
	private String privateid; //google_10974...686이런식이라 겹칠일 없음
	@NotNull
	private String email;
	private String password; //암호화를 거쳐서 안전. 어차피 구글로 로그인 버튼으로 해결이니 노상관
	private String picture;
	private String role = "ROLE_USER";
	private String nickname;

	@Column(insertable=false)
	@ColumnDefault("0")
	private int balance;

	private String createdate;

	@PrePersist
	public void cretedAt(){
		LocalDateTime now = LocalDateTime.now();
		String formatedNow = now.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초"));
		this.createdate = formatedNow;
	}
	
	public User(String name, @NotNull String privateid, @NotNull String email, String picture, String guestNum) {
		this.name = name;
		this.privateid = privateid;
		this.email = email;
		this.picture = picture;
		this.nickname = "게스트"+guestNum; //파라미터에 없는 값으로 저장
	}

	public User update(String name, String picture) {
		this.name = name;
		this.picture = picture;
		
		return this;
	}

}

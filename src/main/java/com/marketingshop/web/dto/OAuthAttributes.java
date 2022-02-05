package com.marketingshop.web.dto;

import com.marketingshop.web.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

//DTO임. ENTITY담기 전에 담는곳 ㅇㅇ//존재목적:view에 쓰이지 않을 데이터도 들어감. 이중장치로 실제 데이터가 외부에 들어나지않음
//entity로서 담는 기능과 함수의 기능을 고루갖춤. 변수들을 쪼개어 담기에 서비스마다 이름이 다른게 해결됨
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OAuthAttributes {
	
	private Map<String, Object> attributes; //모든 개인정보 json값
	private String nameAttributeKey;

	private String name;
	private String userid;
	private String email;
	private String picture;

	
	//(google,kakao,naver)/(sub,id,response)등등 파라미터로 전달
	public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
		if (registrationId.equals("kakao")) {
			System.out.println("카카오입니다.");
			return ofKakao(userNameAttributeName, attributes);
		} else if (registrationId.equals("naver")) {
			System.out.println("네이버입니다.");
			return ofNaver(userNameAttributeName, attributes);
		} else if (registrationId.equals("google")) {
			System.out.println("구글입니다.");
			return ofGoogle(userNameAttributeName, attributes);
		}
		System.out.println("페이스북입니다.");
		return ofFacebook(userNameAttributeName, attributes);
	}
	//kakao_attributes: {id=2035526094, connected_at=2021-12-15T23:45:20Z, properties={nickname=오태석, profile_image=http://k.kakaocdn.net/dn/KQAZE/btrmWrb2gyt/A4GjT1UFZK96qvEXyesD90/img_640x640.jpg, thumbnail_image=http://k.kakaocdn.net/dn/KQAZE/btrmWrb2gyt/A4GjT1UFZK96qvEXyesD90/img_110x110.jpg}, kakao_account={profile_nickname_needs_agreement=false, profile_image_needs_agreement=false, profile={nickname=오태석, thumbnail_image_url=http://k.kakaocdn.net/dn/KQAZE/btrmWrb2gyt/A4GjT1UFZK96qvEXyesD90/img_110x110.jpg, profile_image_url=http://k.kakaocdn.net/dn/KQAZE/btrmWrb2gyt/A4GjT1UFZK96qvEXyesD90/img_640x640.jpg, is_default_image=false}, has_email=true, email_needs_agreement=false, is_email_valid=true, is_email_verified=true, email=xotjr8054@naver.com}
	private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
		//properties={nickname=오태석,profile_image=http://k.kakaocdn.net/dn/KQAZE/btrmWrb2gyt/A4GjT1UFZK96qvEXyesD90/img_640x640.jpg, 	thumbnail_image=http://k.kakaocdn.net/dn/KQAZE/btrmWrb2gyt/A4GjT1UFZK96qvEXyesD90/img_110x110.jpg}, 
		//kakao_account={profile_nickname_needs_agreement=false, profile_image_needs_agreement=false,profile={nickname=오태석, thumbnail_image_url=http://k.kakaocdn.net/dn/KQAZE/btrmWrb2gyt/A4GjT1UFZK96qvEXyesD90/img_110x110.jpg, 	profile_image_url=http://k.kakaocdn.net/dn/KQAZE/btrmWrb2gyt/A4GjT1UFZK96qvEXyesD90/img_640x640.jpg, is_default_image=false}, 
		Map<String, Object> kakao_account = (Map<String, Object>) attributes.get("kakao_account");
		Map<String, String> profile = (Map<String, String>) kakao_account.get("profile");
		String userId = "kakao_"+ String.valueOf(attributes.get("id")); //어째서인지 (String)캐스트가 안됨
		
		return new OAuthAttributes(attributes,
				userNameAttributeName,
				(String) profile.get("nickname"),
									userId,
				(String) kakao_account.get("email"),
				(String) profile.get("profile_image_url"));
	}
	
	//Naver attributes: {resultcode=00, message=success, response={id=0z1FMvR-LTcm02b97ti3k5ScXSnVXR_cu55tadT9sLQ, profile_image=https://phinf.pstatic.net/contact/20210104_76/1609690302899ba4xK_JPEG/%C7%A5%C1%F6_%BF%CF%BC%BA%BA%BB.jpg, email=xotjr8054@naver.com, name=오태석}}
	private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
		//kakao에서 얻은 값들 저장/ profile은 nickname, image_url등을 포함함.
		System.out.println("Naver attributes: "+attributes);
		Map<String, Object> response = (Map<String, Object>) attributes.get("response");
		String userId = "naver_" + (String) response.get("id");
		
		return new OAuthAttributes(attributes,
				userNameAttributeName,
				(String) response.get("name"),
									userId,
				(String) response.get("email"),
				(String) response.get("profile_image"));
	}
	//google attributes: {sub=116463162791834863252, name=오태석, given_name=태석, family_name=오, picture=https://lh3.googleusercontent.com/a-/AOh14Gh8sE2G70R2W7DUxMFQM4Rk_lSy-9wLBXUG-GVW1g=s96-c, email=xotjr8052@gmail.com, email_verified=true, locale=ko}
	private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
		String userId = "google_"+ (String)attributes.get("sub");
		
		return new OAuthAttributes(attributes,
				userNameAttributeName,
				(String) attributes.get("name"),
									userId,
				(String) attributes.get("email"),
				(String) attributes.get("picture"));
	}
	
	private static OAuthAttributes ofFacebook(String userNameAttributeName, Map<String, Object> attributes) {
		String userId = "facebook_"+(String) attributes.get("id");
		
		return new OAuthAttributes(attributes,
				userNameAttributeName,
				(String) attributes.get("name"),
									userId,
				(String) attributes.get("email"),
				(String) attributes.get("picture")); //picture확인필요
	}


	public User toEntity(String guestNum) {
		/* String password = bCryptPasswordEncoder.encode("random"); */
		
		return new User(name, userid, email, picture, guestNum);
	}
}

package com.marketingshop.web.service;

import com.marketingshop.web.dto.OAuthAttributes;
import com.marketingshop.web.entity.SessionUser;
import com.marketingshop.web.entity.User;
import com.marketingshop.web.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Collections;

//google, naver, kakao 에 구애받지 않고 값을 저장하는 함수들이 구현. 자원을 저장키위한 클래스
//추정 : login요청이 오면 OAuth2UserService타입으로 ioc되어있는(지금 클래스 말하는거) loadUser함수가 실행되는듯함
@Service
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User>{

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private HttpSession httpSession; //원래 시큐리티 세션은 더 작은범위. authentication객체만 가능함.해당세션이 차면 로그인된거
	//일반로그인->userdetails타입이 oauth로그인하면 oauth2user타입이 저장된다.
	//타입 두 개 경우의 수 생각하는 문제를 oauthAttributes가 해결해줌.아마?
	
	//만능열쇠에 필요한 정보는 request에, 리턴값에 있는 사용자 데이터는 oAuth2Userdp 저장되어있으니. 꺼내쓰면 된다.
	@Override //얻어진 데이터 oAuth2UserRequest를 후처리 하는 함수 /org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest@1454baf2
	public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException{
		OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService = new DefaultOAuth2UserService();
		OAuth2User oAuth2User = oAuth2UserService.loadUser(oAuth2UserRequest); //그냥도 request.get으로 클라이언트 정보 뺄수있는데 그냥 순차적으로
		
		//google, naver, kakao중 어디인지 값 받기
		String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
		
		//oauth2 로그인시 키 값 저장. google:sub /naver:response /kakao:id
		String userNameAttributeName = oAuth2UserRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
		
		//user 식별자 획득하는거인듯 ; 변수형태: kakao, id, 모든 개인정보 json값
		OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName,oAuth2User.getAttributes());
		
		//dto를 entity에 담고 저장. session에 넘겨서 들고다님 //user: com.marketingshop.web.auth.entity.User@3c4b1ca1
		User user = saveOrUpdate(attributes);
		httpSession.setAttribute("user", new SessionUser(user));
		
		
		return new DefaultOAuth2User(Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
				, attributes.getAttributes()
				, attributes.getNameAttributeKey());
	}

		//email이 이미 있으면 업데이트, 없으면 새로운 행 추가(회원가입이지 ㅇㅇ)
		private User saveOrUpdate(OAuthAttributes attributes) {
			String guestNum = Long.toString(userRepository.findLastId()+1);

			User user = userRepository.findByPrivateid(attributes.getUserid())
					.map(entity -> entity.update(attributes.getName(), attributes.getPicture()))
					.orElse(attributes.toEntity(guestNum));
			log.info("로그인 정보 nickname={}, privateID={}",user.getNickname(),user.getPrivateid());
			
			return userRepository.save(user);
		
	}
}

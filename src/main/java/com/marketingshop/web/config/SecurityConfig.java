package com.marketingshop.web.config;

import com.marketingshop.web.service.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

//@EnableGlobalMethodSecurity(securedEnabled = true,prePostEnable = true)
// Secured("ROLE_ADMIN")어노테이션 활성화 / PreAuthorize("hasRole('ROLE_MANAGER') or hasRole('ROLE_ADMIN')")
//어노테이션 활성화, 어노테이션 달린 함수가 실행되기전에 실행됨, 여러개의 역할 제한을 걸고싶을때 사용됨,PostreAuthorize사용가능하나 안씀
@Configuration
@EnableWebSecurity //스프링 시큐리티 필터가 스프링 필터체인에 등록이 된다
public class SecurityConfig extends WebSecurityConfigurerAdapter{

	@Autowired
	private CustomOAuth2UserService customOAuth2UserService;
	
	//리턴되는 오브젝트를 ioc에 등록해준다.
	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	//httpsecurity보다 websecurity가 훨씬 빠름
	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().mvcMatchers("/members/**", "/image/**"); // 해당 파일들은 security 적용 무시
		web.ignoring().requestMatchers(PathRequest.toStaticResources().atCommonLocations()); // 정적인 리소스들에 대해 시큐리티 적용 무시
	}
	
	//antMatchers("/admin/**").access("hasRole('ROLE_ADMIN')") 으로 하면 접근에 권한 부여. 이게 더맞는듯
	@Override //antMatchers("/**").permitAll().hasRole("ROLE_ADMIN")어드민 권한 부여
	protected void configure(HttpSecurity http) throws Exception{
		http.csrf().disable(); //위조된 이용자 방지=csrf / 그러나 oauth 카카오같은데에서 알아서 걸러주니까 안써도되느듯?
		http.authorizeRequests() //요청에 의한 보안검사 시작
					.antMatchers("/customer/**") //해당 요청에 대하여
					.authenticated() //인증요청을 하겠다. permitAll()은 통과시키는거 ROLE_USER
					.antMatchers("/admin/**")
					.access("hasRole('ROLE_ADMIN')") //권한없으면 403에러
					.anyRequest()
					.permitAll()
				.and()
					.logout()
					.logoutUrl("/doLogout") //컨트롤러 노쓸모일듯
					.logoutSuccessUrl("/loginform") //로그아웃하면 "/login"로 이동
				.and()
					.oauth2Login()
					.loginPage("/loginform") //여기에 안적으면 ㅈ댐
					.defaultSuccessUrl("/customer/neworder")
					.userInfoEndpoint()
					.userService(customOAuth2UserService); //구글로그인이 완료된 뒤의 후처리. 엑세스토큰+프로필정보 동시에 줌
		//oauth2 로그인에 성공하면 유저데이터를 가지고
		// 우리가 생성한 customOAuth2UserService에서 처리를 하겠다. loadUser함수에서 후처리가 진행됨
	}
}

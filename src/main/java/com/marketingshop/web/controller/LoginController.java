package com.marketingshop.web.controller;

import com.marketingshop.web.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller //view를 리턴하겠다는 말
public class LoginController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@GetMapping("/")
	public String home() {
		
		return "loginform";
	}
	
	@GetMapping("/loginform")
	public String loginform() {
	
	return "loginform"; 
	}

	
}

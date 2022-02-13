package com.marketingshop.web.controller;

import com.marketingshop.web.annotation.LoginUser;
import com.marketingshop.web.entity.PaymentData;
import com.marketingshop.web.entity.SessionUser;
import com.marketingshop.web.entity.User;
import com.marketingshop.web.repository.*;
import com.marketingshop.web.service.OrderStatusService;
import com.marketingshop.web.service.SubscriptionService;
import com.marketingshop.web.service.WebClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/")
@Slf4j
public class PayController {

	@Autowired
	private CommentRepository commentRepository;
	@Autowired
	private WebClientService webClientService;
	@Autowired
	private ServiceListRepository serviceListRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private OrderStatusService orderStatusService;
	@Autowired
	private OrderStatusRepository orderStatusRepository;
	@Autowired
	private PaymentDataRepository paymentDataRepository;
	@Autowired
	private SubscriptionRepository subscriptionRepository;
	@Autowired
	private SubscriptionService subscriptionService;


	@GetMapping("addCharge")
	@Transactional(rollbackFor = Exception.class)
	@ResponseBody
	public String addCharge(@LoginUser SessionUser user, String payname, String money){

		if (payname.isEmpty() || money.isEmpty())
			return "<script>alert('빈칸을 채워주세요');location.href='/customer/deposit/1';</script>";

		int int_money=Integer.parseInt(money);
		if (int_money < 5000){
			return "<script>alert('최소 5,000원부터 충전이 가능합니다');location.href='/customer/deposit/1';</script>";
		}

		log.info("{}님이 {}라는 입금자명으로 {}원을 충전 요청하였습니다", user.getPrivateid(), payname, money);
		User realUser = userRepository.findByPrivateid(user.getPrivateid()).get();
		//realUser.setBalance(realUser.getBalance()+amount);

		PaymentData paymentData = new PaymentData(null,realUser,payname,int_money,"대기","무통장입금",null,null,null);
		paymentDataRepository.save(paymentData);

		return "<script>alert('\"동일한\" 입금자명으로 12시간 이내에 이체해주세요');location.href='/customer/deposit/1';</script>";
	}

}
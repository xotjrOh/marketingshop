package com.marketingshop.web.controller;

import com.marketingshop.web.annotation.LoginUser;
import com.marketingshop.web.entity.SessionUser;
import com.marketingshop.web.repository.*;
import com.marketingshop.web.service.OrderStatusService;
import com.marketingshop.web.service.SubscriptionService;
import com.marketingshop.web.service.WebClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
@Slf4j
public class SubsController {

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


	@GetMapping("subscriptions/stop/{subsid}")
	public String subscriptionsStop(Model model, @PathVariable Long subsid, @LoginUser SessionUser user){
		/*model.addAttribute("subscriptions",true);
		model.addAttribute("user",user);

		Pageable pageable = PageRequest.of(page-1,10, Sort.by("subsid").descending()); //Page request [number: 11, size 1, sort: orderid: DESC]
		Page<Subscription> subscriptionList = subscriptionService.getMultiSubscriptionList(user.getPrivateid(),pageable);//Page 12 of 12 containing com.marketingshop.web.entity.OrderStatus instances

		model.addAttribute("subscriptionList",subscriptionList);*/

		return "subscriptions";
	}

	@GetMapping("subscriptions/reorder/{subsid}")
	public String subscriptionsReorder(Model model, @PathVariable Long subsid, @LoginUser SessionUser user){
		/*model.addAttribute("subscriptions",true);
		model.addAttribute("user",user);

		Pageable pageable = PageRequest.of(page-1,10, Sort.by("subsid").descending()); //Page request [number: 11, size 1, sort: orderid: DESC]
		Page<Subscription> subscriptionList = subscriptionService.getMultiSubscriptionListByStatus(user.getPrivateid(),status,pageable);//Page 12 of 12 containing com.marketingshop.web.entity.OrderStatus instances

		model.addAttribute("subscriptionList",subscriptionList);*/

		return "neworder";
	}
}
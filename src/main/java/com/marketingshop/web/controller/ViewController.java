package com.marketingshop.web.controller;

import com.marketingshop.web.annotation.LoginUser;
import com.marketingshop.web.dto.OrderForm;
import com.marketingshop.web.entity.*;
import com.marketingshop.web.repository.*;
import com.marketingshop.web.service.DepositService;
import com.marketingshop.web.service.OrderStatusService;
import com.marketingshop.web.service.SubscriptionService;
import com.marketingshop.web.service.WebClientService;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/customer/")
@Slf4j
public class ViewController {

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
	@Autowired
	private DepositService depositService;
	@Autowired
	private DescriptionRepository descriptionRepository;


	@GetMapping("neworder")
	public String neworder(Model model, @LoginUser SessionUser user) {
		model.addAttribute("neworder",true);
		model.addAttribute("description",descriptionRepository.getById(1l).getContent());
		model.addAttribute("user",user);

		List<String> categories = webClientService.getCategories();
		model.addAttribute("categories",categories);

		return "neworder";
	}

	@PostMapping("addOrder")
	public String addOrder(@LoginUser SessionUser user, OrderForm orderForm, RedirectAttributes rttr) throws ParseException {
		/*if (orderForm.getCharge().contains(","))*/
		String result = webClientService.addOrder(user.getPrivateid(), orderForm); //api 두 번 호출, 주문 및 db 저장
		log.info("{}가 {}를 구매하였습니다. 결과값은 \'{}\'입니다.",user.getPrivateid(),orderForm.getService(),result);

		if (result.contains(".")){ //오류 문구 처리
			rttr.addFlashAttribute("addorder",true);
			rttr.addFlashAttribute("category",orderForm.getCategory());
			rttr.addFlashAttribute("service",orderForm.getService());
			rttr.addFlashAttribute("msg",result);
			return "redirect:/customer/neworder";
		}

		if (!orderForm.getType().equals("100"))
			return "redirect:/customer/neworder/order/"+result;
		else return "redirect:/customer/neworder/subscription/"+result;

	}

	@GetMapping("neworder/order/{orderid}")
	public String afterAddOrder(Model model, @LoginUser SessionUser user, @PathVariable Long orderid) throws ParseException {
		model.addAttribute("user",user);
		if (!orderStatusRepository.findByOrderidAndUser(orderid, userRepository.findByPrivateid(user.getPrivateid()).get()).isPresent())
			return "404"; //본인 주문 아닌거 조회 못하게.

		model.addAttribute("neworder",true);
		model.addAttribute("description",descriptionRepository.getById(1l).getContent());
		model.addAttribute("addorder",true);

		List<String> categories = webClientService.getCategories();
		model.addAttribute("categories",categories);

		OrderStatus orderStatus = orderStatusRepository.getById(orderid);
		model.addAttribute("orderStatus",orderStatus);

		ServiceList serviceList = orderStatus.getServiceList(); //여기 4줄은 subs인지에 따라 경우가 다름
		model.addAttribute("category",serviceList.getCategory());
		model.addAttribute("service",serviceList.getService());

		return "neworder";
	}

	@GetMapping("neworder/subscription/{subsid}")
	public String afterAddSubs(Model model, @LoginUser SessionUser user, @PathVariable Long subsid){
		model.addAttribute("user",user);
		User realuser =  userRepository.findByPrivateid(user.getPrivateid()).get();
		if (!subscriptionRepository.findBySubsidAndUser(subsid, realuser).isPresent())
			return "404"; //본인 주문 아닌거 조회 못하게.

		model.addAttribute("neworder",true);
		model.addAttribute("description",descriptionRepository.getById(1l).getContent());
		model.addAttribute("addorder",true);

		List<String> categories = webClientService.getCategories();
		model.addAttribute("categories",categories);

		Subscription subscription = subscriptionRepository.getById(subsid);
		model.addAttribute("subscription", subscription);

		ServiceList serviceList = subscription.getServiceList();
		model.addAttribute("category",serviceList.getCategory());
		model.addAttribute("service",serviceList.getService());

		return "neworder";
	}
	
	 @GetMapping("orders") 
	 public String orders() {
		return "redirect:/customer/orders/all/1";
	 }

	@GetMapping("orders/all/{page}")
	public String ordersPage(Model model, @PathVariable int page, @LoginUser SessionUser user, String search) throws ParseException {
		model.addAttribute("orders",true);
		model.addAttribute("user",user);
		Pageable pageable = PageRequest.of(page - 1, 10, Sort.by("orderid").descending()); //Page request [number: 11, size 1, sort: orderid: DESC]

		if (search == null) search="";
		Page<OrderStatus> orderlist = orderStatusService.getMultiOrderStatusListBySearch(user.getPrivateid(), search, pageable);//Page 12 of 12 containing com.marketingshop.web.entity.OrderStatus instances

		int startPage = Math.max(1,page-3);
		int endPage = Math.min(orderlist.getTotalPages(),page+3);

		List<Map<String,Integer>> pagelist = new ArrayList<>(); //[{curpage=1, page=1}, {page=2}, {page=3}, {page=4}]
		for (int j = startPage; j < endPage+1; j++) {
			Map<String,Integer> elem = new HashMap<>();
			elem.put("page",j);

			if (orderlist.getNumber()+1 == j)
				elem.put("curpage",1);

			pagelist.add(elem);
		}
		model.addAttribute("pagelist",pagelist);
		model.addAttribute("orderList",orderlist);

		return "orders";
	}

	@GetMapping("orders/subs/{subsid}") //subs에서 클릭시 이동되는곳
	public String subsDetail(Model model, @PathVariable Long subsid, @LoginUser SessionUser user) throws ParseException {
		User realUser = userRepository.findByPrivateid(user.getPrivateid()).get();
		if (!subscriptionRepository.findBySubsidAndUser(subsid, realUser).isPresent())
			return "404"; //본인 주문 아닌거 조회 못하게.
		model.addAttribute("subsDetail",true);
		model.addAttribute("subscriptions",true); //view단 페이지때매 orders안씀
		model.addAttribute("user",user);

		List<OrderStatus> orderlist = orderStatusService.getOrderStatusBySubs(subsid, realUser, subscriptionRepository.findById(subsid).get().getServiceList());

		model.addAttribute("orderList", orderlist);

		return "orders";
	}

	@GetMapping("orders/{status}/{page}")
	public String ordersStatusPage(Model model, @PathVariable String status, @PathVariable int page, @LoginUser SessionUser user, String search) throws ParseException {
		if (status.equals("inprogress")) status="in progress";

		model.addAttribute("orders",true);
		model.addAttribute("user",user);

		Pageable pageable = PageRequest.of(page-1,10, Sort.by("orderid").descending()); //Page request [number: 11, size 1, sort: orderid: DESC]
		if (search == null) search="";
		Page<OrderStatus> orderlist = orderStatusService.getMultiOrderStatusListByStatusAndSearch(user.getPrivateid(), status, search, pageable);//Page 12 of 12 containing com.marketingshop.web.entity.OrderStatus instances

		int startPage = Math.max(1,page-3);
		int endPage = Math.min(orderlist.getTotalPages(),page+3);

		List<Map<String,Integer>> pagelist = new ArrayList<>(); //[{curpage=1, page=1}, {page=2}, {page=3}, {page=4}]
		for (int j = startPage; j < endPage+1; j++) {
			Map<String,Integer> elem = new HashMap<>();
			elem.put("page",j);

			if (orderlist.getNumber()+1 == j)
				elem.put("curpage",1);

			pagelist.add(elem);
		}
		model.addAttribute("pagelist",pagelist);
		model.addAttribute("orderList",orderlist);

		return "orders";
	}

	 @GetMapping("deposit/{page}")
	 public String deposit(Model model, @PathVariable int page, @LoginUser SessionUser user) {
		 model.addAttribute("deposit",true);
		 model.addAttribute("user",user);

		 Pageable pageable = PageRequest.of(page-1,10, Sort.by("id").descending());
		 Page<PaymentData> paymentList = depositService.getMultiPayment(user.getPrivateid(), pageable); //12시간 경과하면 상태 '취소'로 변경

		 int startPage = Math.max(1,page-3);
		 int endPage = Math.min(paymentList.getTotalPages(),page+3);

		 List<Map<String,Integer>> pagelist = new ArrayList<>(); //[{curpage=1, page=1}, {page=2}, {page=3}, {page=4}]
		 for (int j = startPage; j < endPage+1; j++) {
			 Map<String,Integer> elem = new HashMap<>();
			 elem.put("page",j);

			 if (paymentList.getNumber()+1 == j)
				 elem.put("curpage",1);

			 pagelist.add(elem);
		 }
		 model.addAttribute("pagelist",pagelist);
		 model.addAttribute("paymentData",paymentList);

		 return "deposit";
	 }

	@GetMapping("subscriptions")
	public String subscriptions() {
		return "redirect:/customer/subscriptions/all/1";
	}


	@GetMapping("subscriptions/all/{page}")
	public String subscriptionsPage(Model model, @PathVariable int page, @LoginUser SessionUser user, String search){
		model.addAttribute("subscriptions",true);
		model.addAttribute("user",user);

		Pageable pageable = PageRequest.of(page-1,10, Sort.by("subsid").descending()); //Page request [number: 11, size 1, sort: orderid: DESC]
		if (search == null) search="";
		Page<Subscription> subscriptionList = subscriptionService.getMultiSubscriptionListBySearch(user.getPrivateid(), search, pageable);//Page 12 of 12 containing com.marketingshop.web.entity.OrderStatus instances

		int startPage = Math.max(1,page-3);
		int endPage = Math.min(subscriptionList.getTotalPages(),page+3);

		List<Map<String,Integer>> pagelist = new ArrayList<>(); //[{curpage=1, page=1}, {page=2}, {page=3}, {page=4}]
		for (int j = startPage; j < endPage+1; j++) {
			Map<String,Integer> elem = new HashMap<>();
			elem.put("page",j);

			if (subscriptionList.getNumber()+1 == j)
				elem.put("curpage",1);

			pagelist.add(elem);
		}
		model.addAttribute("pagelist",pagelist);
		model.addAttribute("subscriptionList",subscriptionList);

		return "subscriptions";
	}

	@GetMapping("subscriptions/{status}/{page}") //이유는 모르나 위와 중복체크되지 않음
	public String subscriptionsStatusPage(Model model, @PathVariable String status, @PathVariable int page, @LoginUser SessionUser user, String search){
		model.addAttribute("subscriptions",true);
		model.addAttribute("user",user);

		Pageable pageable = PageRequest.of(page-1,10, Sort.by("subsid").descending()); //Page request [number: 11, size 1, sort: orderid: DESC]
		if (search == null) search="";
		Page<Subscription> subscriptionList = subscriptionService.getMultiSubscriptionListByStatusAndSearch(user.getPrivateid(), status, search, pageable);//Page 12 of 12 containing com.marketingshop.web.entity.OrderStatus instances

		int startPage = Math.max(1,page-3);
		int endPage = Math.min(subscriptionList.getTotalPages(),page+3);

		List<Map<String,Integer>> pagelist = new ArrayList<>(); //[{curpage=1, page=1}, {page=2}, {page=3}, {page=4}]
		for (int j = startPage; j < endPage+1; j++) {
			Map<String,Integer> elem = new HashMap<>();
			elem.put("page",j);

			if (subscriptionList.getNumber()+1 == j)
				elem.put("curpage",1);

			pagelist.add(elem);
		}
		model.addAttribute("pagelist",pagelist);
		model.addAttribute("subscriptionList",subscriptionList);

		return "subscriptions";
	}



	//sk update내역 받아올때
	@GetMapping("test")
	public String test(Model model, @LoginUser SessionUser user) {
		/*model.addAttribute("orders",true);
		model.addAttribute("user",user);*/

		/*webClientService.getSmmServices();*/
		webClientService.changekor();

		return "test3";
	}
	
}
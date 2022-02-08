package com.marketingshop.web.controller;

import com.marketingshop.web.annotation.LoginUser;
import com.marketingshop.web.entity.SessionUser;
import com.marketingshop.web.repository.*;
import com.marketingshop.web.service.OrderStatusService;
import com.marketingshop.web.service.SubscriptionService;
import com.marketingshop.web.service.WebClientService;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.concurrent.TimeUnit;

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
	@Transactional(rollbackFor = Exception.class)
	public String subscriptionsStop(@PathVariable String subsid, @LoginUser SessionUser user) throws InterruptedException {
		String WEB_DRIVER_ID = "webdriver.chrome.driver";
		String WEB_DRIVER_PATH = "C:\\Users\\xotjr\\.spyder-py3\\chromedriver.exe";
		System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);

		WebDriver driver = new ChromeDriver();
		String url = "https://smmkings.com/";
		driver.get(url);
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		//로그인
		driver.findElement(new By.ByLinkText("GET STARTED")).click();
		Thread.sleep(1000); //driver.wait(3000);
		driver.findElement(By.name("LoginForm[username]")).sendKeys("xotjr8054");
		Thread.sleep(100);
		driver.findElement(By.name("LoginForm[password]")).sendKeys("**ots8054");
		Thread.sleep(100);
		driver.findElement(By.xpath("//*[@id=\"container\"]/div[2]/form/button[1]")).submit();
		Thread.sleep(1000);
		driver.get("https://smmkings.com/subscriptions/stop/"+subsid);
		Thread.sleep(1000);
		driver.quit();

		return "redirect:/customer/subscriptions/all/1";
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
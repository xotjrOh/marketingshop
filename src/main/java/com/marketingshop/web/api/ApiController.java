package com.marketingshop.web.api;

import com.marketingshop.web.entity.Comment;
import com.marketingshop.web.entity.ServiceList;
import com.marketingshop.web.entity.Subscription;
import com.marketingshop.web.entity.User;
import com.marketingshop.web.repository.*;
import com.marketingshop.web.service.WebClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController //json반환 컨트롤러
@RequestMapping("/api/v2")
@Slf4j
public class ApiController { //get, post, patch, delete

    private String apiKey = "9ad7be959340d16c54fb19ca200722ac";
    @Autowired
    private WebClientService webClientService;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PaymentDataRepository paymentDataRepository;
    @Autowired
    private ServiceListRepository serviceListRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;


    /*------------------------------------------ServiceList 관련 서비스-------------------------------------*/
    @GetMapping("/allServices")
    public List<ServiceList> Services(){
        //log.info(webClientService.getServices());
        List<ServiceList> services = webClientService.getAllServices();

        return services;
    }

    @GetMapping("/servicesByCategory")
    public List<ServiceList> ServiceLists(String category){

        return webClientService.getServicesByCategory(category);
    }

    @GetMapping("/sortTime")
    public List<ServiceList> sortTime(String servicenum){
        String category = serviceListRepository.getById(servicenum).getCategory();
        return serviceListRepository.findByCategoryOrderByTime(category);
    }

    @GetMapping("/getService")
    public Optional<ServiceList> getService(String servicenum){

        return webClientService.getServiceByServicenum(servicenum);
    }

    @GetMapping("/getSubscription")
    public Subscription getSubscription(Long subsid){

        return subscriptionRepository.getById(subsid);
    }


    /*-----------------------------------------------User 관련 서비스------------------------------------------*/
    @GetMapping("/changeNickname")
    @Transactional(rollbackFor = Exception.class)
    public boolean changeNicknameAlarm(String nickname, String privateId){
        //닉네임 글자 제한
        int min = Math.min(nickname.length(),8);
        nickname = nickname.substring(0,min);

        Optional<User> nicknameOwner = webClientService.validNickname(nickname);
        User myProfile = webClientService.getUserByPrivateId(privateId).get();
        String beforeNickname = myProfile.getNickname();
        if (!nicknameOwner.isPresent()){
            myProfile.setNickname(nickname);
            log.info("{}님이 아이디 {}에서 {}로 변경하였습니다",privateId,beforeNickname,nickname);
            return true;
        }   else{
            log.info("{}님이 아이디 {}에서 {}로 변경에 실패하였습니다",privateId,beforeNickname,nickname);
            return false;
        }
    }

    @GetMapping("/userByPrivateId")
    public Optional<User> UserByPrivateId(String privateId){
        return webClientService.getUserByPrivateId(privateId);
    }

    /*@GetMapping("/addCharge") //아임포트용. 사용하려면 paymentData생성자 바꿔야함
    @Transactional(rollbackFor = Exception.class)
    public void addCharge(int amount, String pg, String merchant_uid, @LoginUser SessionUser user){ //오로지 db값 증가시키려는 목적
        log.info("{} : {}님이 {}원을 충전하였습니다", merchant_uid, user.getPrivateid(), amount);
        User realUser = userRepository.findByPrivateid(user.getPrivateid()).get();
        realUser.setBalance(realUser.getBalance()+amount);

        PaymentData paymentData = new PaymentData(null,realUser,amount,pg,merchant_uid,null);
        paymentDataRepository.save(paymentData);
    }*/


    /*---------------------------------------------Comment 관련 서비스----------------------------------------*/
    /*@PatchMapping("/comments/{commentid}")
    @Transactional(rollbackFor = Exception.class)
    public boolean update(@PathVariable Long commentid, @RequestBody CommentDTO commentDTO){ //못쓰겠다 일단 포기(구현하려하면 clickChangeStar가 뜬금멈춤)
        Optional<Comment> comment = commentRepository.findById(commentid);
        Comment updated = comment.get().update(commentDTO);

        return true*//*ResponseEntity.status(HttpStatus.OK).body(updated)*//*;
    }*/

    @GetMapping("/comments/{serviceNum}")
    @Transactional(rollbackFor = Exception.class)
    public List<Comment> getCommentList(@PathVariable String serviceNum){
        return commentRepository.findByServiceListOrderByIdDesc(serviceListRepository.getById(serviceNum));
    }







    /*-----------------------------------------------아직 보류중------------------------------------------*/
    //key:apikey action:services
    /*@PostMapping("/addOrder")
    public String AddOrder(){
        //log.info(webClientService.getServices());
        //webClientService.addOrder();

        return null;
    }*/

    //key:apikey action:services
    /*@GetMapping("/add/{serviceID}/{link}/{quantity}")
    public ResponseEntity<String> addOrder(){
        log.info("로그찍습니다");
        String ordernum;
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }*/
}

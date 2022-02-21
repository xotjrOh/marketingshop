package com.marketingshop.web.service;

import com.marketingshop.web.dto.OrderForm;
import com.marketingshop.web.entity.OrderStatus;
import com.marketingshop.web.entity.ServiceList;
import com.marketingshop.web.entity.Subscription;
import com.marketingshop.web.entity.User;
import com.marketingshop.web.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class WebClientService {
    @Autowired
    private WebClient webClient;
    private String apiKey = "9ad7be959340d16c54fb19ca200722ac";

    @Autowired
    private ServiceListRepository serviceListRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private OrderStatusRepository orderStatusRepository;
    @Autowired
    private OrderStatusService orderStatusService;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private SubscriptionService subscriptionService;

    /*------------------------------------------SMM 관련 서비스-------------------------------------*/
    @Transactional(rollbackFor = Exception.class)
    public List<ServiceList> getSmmServices() {
        MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
        params.add("key", apiKey);
        params.add("action", "services");

        List<ServiceList> serviceList = webClient.post().uri("/api/v2")
                                        .bodyValue(params)
                                        .retrieve()
                                        .bodyToFlux(ServiceList.class)
                                        .collectList()
                                        .block();
        //변경사항 db에 업데이트, 반환값 받아도 위에꺼랑 같을듯
        saveOrUpdateServices(serviceList);

        return serviceList;
    }

    @Transactional(rollbackFor = Exception.class)
    public String addOrder(String privateid,OrderForm orderForm) throws ParseException {
        MultiValueMap<String,Object> params = new LinkedMultiValueMap<>();
        params.add("key", apiKey);
        params.add("action", "add");
        params.add("service", orderForm.getService());

        ServiceList serviceList = serviceListRepository.findByService(orderForm.getService()).get();
        int min = Integer.parseInt(serviceList.getMin());
        int max = Integer.parseInt(serviceList.getMax());
        int price;

        if (orderForm.getType().equals("12")){ //{{!12 Default 2개}}
            if (orderForm.getLink().isEmpty() || orderForm.getQuantity().isEmpty()) return "빈칸을 기입해주세요.";
            if ( Integer.parseInt(orderForm.getQuantity()) < min ||
                 Integer.parseInt(orderForm.getQuantity()) > max)
                return "최소, 최대 범위를 확인해주세요.";
            params.add("link", orderForm.getLink());
            params.add("quantity", orderForm.getQuantity());

            price = serviceList.getPrice() * Integer.parseInt(orderForm.getQuantity()) / 1000;
        } else if (orderForm.getType().equals("100")){ //{{!100 Subscriptions 3개}}
            if (orderForm.getUsername().isEmpty() || orderForm.getMin().isEmpty() || orderForm.getMax().isEmpty() || orderForm.getPosts().isEmpty()) return "빈칸을 기입해주세요.";
            if ( Integer.parseInt(orderForm.getMin()) < min ||
                 Integer.parseInt(orderForm.getMax()) > max ||
                 Integer.parseInt(orderForm.getMin()) > Integer.parseInt(orderForm.getMax()))
                return "최소, 최대 범위를 확인해주세요.";
            params.add("username", orderForm.getUsername());
            params.add("min", orderForm.getMin());
            params.add("max", orderForm.getMax());
            params.add("posts", orderForm.getPosts());
            params.add("delay", "0"); //나중에 설정값 넣을수도

            price = serviceList.getPrice() * Integer.parseInt(orderForm.getPosts()) * (Integer.parseInt(orderForm.getMin()) + Integer.parseInt(orderForm.getMax())) / 2000;
        } else if (orderForm.getType().equals("14")){ //{{!14 Custom Comments Package 2개}}
            if (orderForm.getLink().isEmpty() || orderForm.getComments().isEmpty()) return "빈칸을 기입해주세요.";

            params.add("link", orderForm.getLink());
            params.add("comments", orderForm.getComments());

            price = serviceList.getPrice();
        }  else if (orderForm.getType().equals("2")){ //{{!2 Custom Comments 3개}}
            if (orderForm.getLink().isEmpty() || orderForm.getQuantity().isEmpty() || orderForm.getComments().isEmpty()) return "빈칸을 기입해주세요.";
            if ( Integer.parseInt(orderForm.getQuantity()) < min ||
                    Integer.parseInt(orderForm.getQuantity()) > max)
                return "최소, 최대 범위를 확인해주세요.";

            params.add("link", orderForm.getLink());
            params.add("comments", orderForm.getComments());

            String LINE_SEPERATOR=System.getProperty("line.separator");
            int quantity = orderForm.getComments().split(LINE_SEPERATOR).length;
            price = serviceList.getPrice() * quantity / 1000;
        } else { //{{!10 Package 1개}}
            if (orderForm.getLink().isEmpty()) return "빈칸을 기입해주세요.";
            params.add("link", orderForm.getLink());

            price = serviceList.getPrice();
        } /*else if (orderForm.getType().equals("15"))//{{!15 Comment Likes 3개}} 임의 추정
            params.add("link", orderForm.getLink());
            params.add("quantity", orderForm.getQuantity());
            params.add("comment_username", orderForm.getComment_username);
        }*/

        User user = userRepository.findByPrivateid(privateid).get();

        if (orderForm.getCharge().isEmpty()){
            log.info("{}님이 주문도중 가격이 업데이트 되지않았습니다. orderForm : {}",privateid,orderForm);
            return "가격이 업데이트 되지 않았습니다.";
        }

        if (user.getBalance() - price < 0)
            return "잔액이 부족합니다.";
        user.setBalance(user.getBalance() - price);

        Long order;
        try {
            String orderid = webClient.post().uri("/api/v2") //주문 오류입니다. 삭제된 서비스일 수 있습니다.
                    .bodyValue(params)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JSONParser jsonParser = new JSONParser();
            JSONObject orderidJson = (JSONObject) jsonParser.parse(orderid);
            order = (Long) orderidJson.get("order"); //없어진 서비스 주문하면 어찌되는지 확인하기
        } catch(Exception e){
            log.info("{}님이 알 수 없는 주문발생, orderform : {}, params : {}, exception : {}",privateid,orderForm,params,e);
            return "잘못된 주문입니다. 관리자에게 문의하세요";
        }
        /*Long order = 7527413l;*/

        serviceList.salesPlus();
        try {
            if (!orderForm.getType().equals("100")) {
                OrderStatus orderStatus = orderStatusService.getOrderStatus(order); //smm api 값들 업데이트 시킴
                orderStatus.inputValueUpdate(user, serviceList, orderForm, price);
                orderStatusRepository.save(orderStatus);
            } else {
                Subscription subscription = subscriptionService.getSubscription(order, user, serviceList); //type에 따라 아예 저장하는 테이블이 달라짐
                subscription.inputValueUpdate(serviceList, orderForm, price);
                subscriptionRepository.save(subscription);
            }
        }catch(Exception e){
            log.info("{}님이 주문내역 업데이트중 오류, orderform : {}, params : {}, exception : {}, orderid : {}",privateid,orderForm,params,e,order);
            return "주문은 정상적으로 처리되었으나 주문내역 업데이트 중 오류가 발생했습니다.";
        }
        return  String.valueOf(order);
    }


    /*------------------------------------------ServiceList 관련 서비스-------------------------------------*/
    @Transactional(rollbackFor = Exception.class)
    public List<ServiceList> getAllServices() {
        return serviceListRepository.findAll();
    }

    @Transactional(rollbackFor = Exception.class)
    public List<ServiceList> getServicesByCategory(String category){
        return serviceListRepository.findByCategory(category);
    }

    @Transactional(rollbackFor = Exception.class)
    public Optional<ServiceList> getServiceByServicenum(String servicenum){
        return serviceListRepository.findByService(servicenum);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateServices(List<ServiceList> services) {
        serviceListRepository.saveAll(services);
    }

    @Transactional(rollbackFor = Exception.class) //한국어 + 한국 가격으로 이름 변경
    public void changekor(){
        List<ServiceList> serviceLists = serviceListRepository.findAll();

        for (ServiceList serviceList : serviceLists) {
            DecimalFormat decFormat = new DecimalFormat("###,###");

            int price = (int) (Float.parseFloat(serviceList.getRate()) * 2200);
            serviceList.setPrice(price);

            String korname;
            if (serviceList.getType().equals("Package") || serviceList.getType().equals("Custom Comments Package")){
                korname = String.format("%s -- %s원 / 1개", serviceList.getName(), decFormat.format(price));
            } else {
                korname = String.format("%s -- %s원 / 1,000개", serviceList.getName(), decFormat.format(price));
            }
            korname = korname.replace("Instagram","인스타그램")
                    .replace("Dislikes","싫어요")
                    .replace("Email Scraping by User [Followers]","사용자 팔로워를 통한 이메일 수집")
                    .replace("Email Scraping by User [Following]","사용자 팔로잉을 통한 이메일 수집")
                    .replace("Email Scraping by #Hashtags","#해시태그 를 통한 이메일 수집")
                    .replace("RAV-GS™ - Real & Active Views","구글검색 광고")
                    .replace("RAV-MTS™ - High Monetization Views","구글검색 광고(수익 극대화)")
                    .replace("RAV™ - Real & Active Views","직접 광고")
                    .replace("Likes","좋아요")
                    .replace("Impressions","노출")
                    .replace("Reach","도달")
                    .replace("Saudi Arabia","사우디아라비아인")
                    /*.replace("LQ","낮은퀄")
                    .replace("UHQ","높은퀄")
                    .replace("HQ","일반퀄")
                    .replace("Real","높은퀄")
                    .replace("Active","실제유저")
                    .replace("Mixed","혼합퀄")*/
                    .replace("Max","최대")
                    .replace("NO-DROP","이탈X")
                    .replace("WorldWide","전세계")
                    .replace("Worldwide","전세계")
                    .replace("South Korea","한국인")
                    .replace("South-Korea","한국인")
                    .replace("EU","유럽인")
                    .replace("Arabic","아랍어")
                    .replace("Arab","아랍인")
                    .replace("Based","기반")
                    .replace("Female","여성")
                    .replace("Male","남성")
                    .replace("Russian","러시아인")
                    .replace("Russia","러시아인")
                    .replace("USA","미국인")
                    .replace("Turkey","터키인")
                    .replace("Usuarios Latinos","라틴계 사용자")
                    .replace("Posts","게시물")
                    .replace("Post","게시물")
                    .replace("\uD835\uDC02\uD835\uDC28\uD835\uDC26\uD835\uDC1B\uD835\uDC28 \uD835\uDC0F\uD835\uDC1A\uD835\uDC1C\uD835\uDC24","패키지") //콤보팩
                    .replace("\uD835\uDC11\uD835\uDC1E\uD835\uDC1A\uD835\uDC1C\uD835\uDC21","도달")
                    .replace("\uD835\uDC0F\uD835\uDC2B\uD835\uDC28\uD835\uDC1F\uD835\uDC22\uD835\uDC25\uD835\uDC1E \uD835\uDC15\uD835\uDC22\uD835\uDC2C\uD835\uDC22\uD835\uDC2D\uD835\uDC2C","프로필 방문")
                    .replace("Profile Visits","프로필 방문")
                    .replace("Photo","사진")
                    .replace("Video","영상")
                    .replace("Highlights Views","하이라이트 조회수")
                    .replace("Saves","저장")
                    .replace("INSTANT","즉시")
                    .replace("Verified","검증된")
                    .replace("Followers","팔로워")
                    .replace("Follower","팔로워")
                    .replace("Artist","아티스트")
                    .replace(" Days Refill","일 리필")
                    .replace("Aged Profiles","고령의 프로필")
                    .replace("Turkish","터키어")
                    .replace(" Days Auto-Refill","일 자동리필")
                    .replace("NON DROP","이탈X")
                    .replace("Custom","지정")
                    .replace("Random","임의의")
                    .replace("Commercial","상업성")
                    .replace("Comments","댓글")
                    .replace("Comment","댓글")
                    .replace("Asian Mix","아시아인")
                    .replace("Asian","아시아인")
                    .replace("Japan","일본인")
                    .replace("Chinese","중국인")
                    .replace("Views","조회수")
                    .replace("Reel","릴")
                    .replace("Accepted","모두 허용")
                    .replace("\uD835\uDC02\uD835\uDC28\uD835\uDC26\uD835\uDC1B\uD835\uDC28","패키지")
                    .replace("Emoji","이모지")
                    .replace("Positive","긍정적인")
                    .replace("Germany","독일인")
                    .replace("France","프랑스인")
                    .replace("Accounts","계정")
                    .replace("Account","계정")
                    .replace("Auto","자동")
                    .replace("AUTO","자동")
                    .replace("AU","호주")
                    .replace("UK","영국")
                    .replace("Telegram","텔레그램")
                    .replace("\uD835\uDC08\uD835\uDC26\uD835\uDC29\uD835\uDC2B\uD835\uDC1E\uD835\uDC2C\uD835\uDC2C\uD835\uDC22\uD835\uDC28\uD835\uDC27\uD835\uDC2C","노출")
                    .replace("Story","스토리")
                    .replace("ALL POSTS","모든 스토리")
                    .replace("Sticker Links Clicks","스티커 링크 클릭")
                    .replace("Live","라이브")
                    .replace("YouTube","유튜브")
                    .replace("Unique","실제")
                    .replace("Engagements","참여")
                    .replace("\uD835\uDC04\uD835\uDC27\uD835\uDC20\uD835\uDC25\uD835\uDC22\uD835\uDC2C\uD835\uDC21 \uD835\uDC12\uD835\uDC29\uD835\uDC1E\uD835\uDC1A\uD835\uDC24\uD835\uDC1E\uD835\uDC2B\uD835\uDC2C","영어 사용 시청자")
                    .replace("\uD835\uDC05\uD835\uDC2B\uD835\uDC1E\uD835\uDC27\uD835\uDC1C\uD835\uDC21 \uD835\uDC12\uD835\uDC29\uD835\uDC1E\uD835\uDC1A\uD835\uDC24\uD835\uDC1E\uD835\uDC2B\uD835\uDC2C","프랑스어 사용 시청자")
                    .replace("\uD835\uDC12\uD835\uDC29\uD835\uDC1A\uD835\uDC27\uD835\uDC22\uD835\uDC2C\uD835\uDC21 \uD835\uDC12\uD835\uDC29\uD835\uDC1E\uD835\uDC1A\uD835\uDC24\uD835\uDC1E\uD835\uDC2B\uD835\uDC2C","스페인어 사용 시청자")
                    .replace("\uD835\uDC06\uD835\uDC1E\uD835\uDC2B\uD835\uDC26\uD835\uDC1A\uD835\uDC27 \uD835\uDC12\uD835\uDC29\uD835\uDC1E\uD835\uDC1A\uD835\uDC24\uD835\uDC1E\uD835\uDC2B\uD835\uDC2C","독일어 사용 시청자")
                    .replace("\uD835\uDC00\uD835\uDC2B\uD835\uDC1A\uD835\uDC1B\uD835\uDC22\uD835\uDC1C \uD835\uDC12\uD835\uDC29\uD835\uDC1E\uD835\uDC1A\uD835\uDC24\uD835\uDC1E\uD835\uDC2B\uD835\uDC2C","아랍어 사용 시청자")
                    .replace("\uD835\uDC0F\uD835\uDC28\uD835\uDC2B\uD835\uDC2D\uD835\uDC2E\uD835\uDC20\uD835\uDC2E\uD835\uDC1E\uD835\uDC2C\uD835\uDC1E \uD835\uDC12\uD835\uDC29\uD835\uDC1E\uD835\uDC1A\uD835\uDC24\uD835\uDC1E\uD835\uDC2B\uD835\uDC2C","포르투갈어 사용 시청자")
                    .replace("United Arab (UAE)","아랍에미리트인")
                    .replace("Argentina","아르헨티나인")
                    .replace("Bangladesh","방글라데시인")
                    .replace("Brazil","브라질인")
                    .replace("Egypt","이집트인")
                    .replace("Indian","인도인")
                    .replace("India","인도인")
                    .replace("Viet Nam","베트남인")
                    .replace("Romania","루마니아인")
                    .replace("Morocco","모로코인")
                    .replace("Colombia","콜롬비아인")
                    .replace("Philippines","필리핀인")
                    .replace("Ecuador","에콰도르인")
                    .replace("Tunisia","튀니지인")
                    .replace("Croatia","크로아티아인")
                    .replace("Venezuela","베네수엘라인")
                    .replace("Vietnam","베트남")
                    .replace("South Africa","남아프리카 공화국")
                    .replace("Italy","이탈리아인")
                    .replace("Pakistan","파키스탄인")
                    .replace("Nepal","네팔인")
                    .replace("Mexico","멕시코인")
                    .replace("Algeria","알제리인")
                    .replace("Bulgaria","불가리아인")
                    .replace("Netherlands","네덜란드인")
                    .replace("Greece","그리스인")
                    .replace("Spain","스페인인")
                    .replace("Thailand","태국인")
                    .replace("North Macedonia","북마케도니아인")
                    .replace("Sweden","스웨덴인")
                    .replace("Afghanistan","아프가니스탄인")
                    .replace("Albania","알바니아인")
                    .replace("Angola","앙골라인")
                    .replace("Azerbaijan","아제르바이잔인")
                    .replace("Bolivia","볼리비아인")
                    .replace("Bosnia and Herzegovina","보스니아 헤르체고비나인")
                    .replace("Cambodia","캄보디아인")
                    .replace("Chile","칠레인")
                    .replace("Costa Rica","코스타리카인")
                    .replace("Dominican Republic","도미니카 공화국인")
                    .replace("El Salvador","엘살바도르인")
                    .replace("Georgia","조지아인")
                    .replace("Ghana","가나인")
                    .replace("Guatemala","과테말라인")
                    .replace("Honduras","온두라스인")
                    .replace("Hong Kong","홍콩인")
                    .replace("Hungary","헝가리인")
                    .replace("Iraq","이라크인")
                    .replace("Israel","이스라엘인")
                    .replace("Jamaica","자메이카인")
                    .replace("Jordan","요르단인")
                    .replace("Kenya","케냐인")
                    .replace("Kuwait","쿠웨이트인")
                    .replace("Laos","라오스인")
                    .replace("Libya","리비아인")
                    .replace("Malaysia","말레이시아인")
                    .replace("Mongolia","몽골인")
                    .replace("Montenegro","몬테네그로인")
                    .replace("Myanmar","미얀마인")
                    .replace("Nicaragua","니카라과인")
                    .replace("Oman","오만인")
                    .replace("Panama","파나마인")
                    .replace("Paraguay","파라과이인")
                    .replace("Peru","페루인")
                    .replace("Portugal","포르투갈인")
                    .replace("Puerto Rico","푸에르토리코인")
                    .replace("Qatar","카타르인")
                    .replace("Singapore","싱가폴인")
                    .replace("Senegal","세네갈인")
                    .replace("Slovenia","슬로베니아인")
                    .replace("Sri Lanka","스리랑카인")
                    .replace("Sudan","수단인")
                    .replace("Syria","시리아인")
                    .replace("Taiwan","대만인")
                    .replace("Tanzania","탄자니아인")
                    .replace("Trinidad and Tobago","트리니다드 토바고인")
                    .replace("Uruguay","우루과이인")
                    .replace("Serbia","세르비아인")
                    .replace("Indonesia","인도네시아인")
                    .replace("Australia","호주인")
                    .replace("Poland","폴란드인")
                    .replace("Great Britain","그레이트브리튼인")
                    .replace("Lebanon","레바논인")
                    .replace("Canada","캐나다인")
                    .replace("China","중국인")
                    .replace("Ukraine","우크라이나")
                    .replace("Czech Republic","체코")
                    .replace("Moldova","몰도바")
                    .replace("Switzerland","스위스")
                    .replace("Subscribers","구독자")
                    .replace("No Refill","리필 없음")
                    .replace("Community","커뮤니티")
                    .replace("UPVOTES","상승")
                    .replace("Social Shares","소셜 공유")
                    .replace("Facebook","페이스북")
                    .replace("Twitter","트위터")
                    .replace("Reddit","레딧")
                    .replace("Pinterest","핀터레스트")
                    .replace("Linkedin","링크드인")
                    .replace("Tumblr","텀블러")
                    .replace("Blogger","블로거")
                    .replace("\uD835\uDC0F\uD835\uDC1E\uD835\uDC2B \uD835\uDC03\uD835\uDC1A\uD835\uDC32","/ 1일")
                    .replace("Ultra-Fast Speed","매우 빠름")
                    .replace("ULTRA-FAST","매우 빠름")
                    .replace("TikTok","틱톡")
                    .replace("Shares","공유")
                    .replace("Emoticons","이모티콘")
                    .replace("WOW","멋져요")
                    .replace("LOVE","최고에요")
                    .replace("ANGRY","화나요")
                    .replace("HAHA","웃겨요")
                    .replace("SAD","슬퍼요")
                    .replace("Fan Page","팬 페이지")
                    .replace("Low Retention","빠른 이탈")
                    .replace("High Retention","느린 이탈")
                    .replace("Stream","스트림")
                    .replace("30 Minutes Retention","30분 유지")
                    .replace("CUSTOM","지정")
                    .replace("RANDOM","임의의")
                    .replace("FEMALE","여성")
                    .replace("MALE","남성")
                    .replace("Twitch.TV","트위치")
                    .replace("Twitch","트위치")
                    .replace("NON-DROP","이탈X")
                    .replace("Channel","채널")
                    .replace("Profile Click","프로필 클릭")
                    .replace("Spotify","스포티파이")
                    .replace("Track","트랙")
                    .replace("Plays","재생")
                    .replace("Album","앨범")
                    .replace("Monthly Listeners","월별 리스너")
                    .replace("Soundcloud","사운드클라우드")
                    .replace("Reposts","재게시")
                    .replace("RePins","리핀")
                    .replace("Board","보드")
                    .replace("High-Speed","빠름")
                    .replace("Members","회원")
                    .replace("Last","최근")
                    .replace("Answer","답변")
                    .replace("Natural Speed","자연스러운 속도")
                    .replace("Fast Speed","빠름")
                    .replace("Traffic","트래픽")
                    .replace("Google.com","구글")
                    .replace("Google","구글")
                    .replace("Social Networks","소셜 네트워크")
                    .replace("Social","소셜")
                    .replace("Organic","유기적")
                    .replace("Keywords","키워드")
                    .replace("Direct Visits","직접 방문")
                    .replace("Pop-Under Ads","팝언더 광고")
                    .replace("Wikipedia","위키피디아")
                    .replace("Amazon.com","아마존")
                    .replace("Premium","프리미엄")
                    .replace("Naver Search","네이버 검색")
                    .replace("Daum.net Search","다음 검색")
                    .replace("Mobile Devices","핸드폰")
                    .replace("iPhone","아이폰")
                    .replace("Mobile","핸드폰")
                    .replace("Any Country","원하는 국가")
                    .replace("Signals","신호")
                    .replace("Marketplace","마켓플레이스")
                    .replace("Min","최소")
                    .replace("Visits","방문")
                    .replace("Search","검색")
                    .replace("V","버전")
                    .replace("S","서버")
                    .replace("구글.","Google.")
                    .replace("서버ogou","Sogou")
                    .replace("구글API","GoogleAPI")
                    .replace("AW서버","AWS")
                    .replace("서버teemit","Steemit")
                    .replace("버전K","VK")
                    .replace("T버전","TV")
                    .replace("서버uper","Super")
                    .replace("버전kontakte","Vkontakte")
                    .replace("서버ina","Sina")
                    .replace("서버tumbleUpon","StumbleUpon")
                    .replace("Buy서버ell","BuySell")
                    .replace("서버tumbleupon","Stumbleupon")
                    .replace("서버tumbleupon","Stumbleupon")
                    .replace("버전imeo","Vimeo");
            serviceList.setKorname(korname);

            if (serviceList.getCategory().toLowerCase().contains("instagram")) {
                String category = serviceList.getCategory();
                int idx = category.toLowerCase().indexOf("instagram");
                serviceList.setCategory(category.substring(idx));
            } else if (serviceList.getCategory().toLowerCase().contains("youtube")) {
                String category = serviceList.getCategory();
                int idx = category.toLowerCase().indexOf("youtube");
                serviceList.setCategory(category.substring(idx));
            } else if (serviceList.getCategory().toLowerCase().contains("facebook")) {
                String category = serviceList.getCategory();
                int idx = category.toLowerCase().indexOf("facebook");
                serviceList.setCategory(category.substring(idx));
            } else if (serviceList.getCategory().toLowerCase().contains("twitter")) {
                String category = serviceList.getCategory();
                int idx = category.toLowerCase().indexOf("twitter");
                serviceList.setCategory(category.substring(idx));
            } else if (serviceList.getCategory().toLowerCase().contains("tiktok")) {
                String category = serviceList.getCategory();
                int idx = category.toLowerCase().indexOf("tiktok");
                serviceList.setCategory(category.substring(idx));
            } else if (serviceList.getCategory().toLowerCase().contains("telegram")) {
                String category = serviceList.getCategory();
                int idx = category.toLowerCase().indexOf("telegram");
                serviceList.setCategory(category.substring(idx));
            } else if (serviceList.getCategory().toLowerCase().contains("spotify")) {
                String category = serviceList.getCategory();
                int idx = category.toLowerCase().indexOf("spotify");
                serviceList.setCategory(category.substring(idx));
            } else if (serviceList.getCategory().toLowerCase().contains("pinterest")) {
                String category = serviceList.getCategory();
                int idx = category.toLowerCase().indexOf("pinterest");
                serviceList.setCategory(category.substring(idx));
            } else if (serviceList.getCategory().toLowerCase().contains("quora")) {
                String category = serviceList.getCategory();
                int idx = category.toLowerCase().indexOf("quora");
                serviceList.setCategory(category.substring(idx));
            } else if (serviceList.getCategory().toLowerCase().contains("soundcloud")) {
                String category = serviceList.getCategory();
                int idx = category.toLowerCase().indexOf("soundcloud");
                serviceList.setCategory(category.substring(idx));
            } else if (serviceList.getCategory().toLowerCase().contains("vimeo")) {
                String category = serviceList.getCategory();
                int idx = category.toLowerCase().indexOf("vimeo");
                serviceList.setCategory(category.substring(idx));
            }

            String category = serviceList.getCategory()
                    .replace("RAV™ - Real & Active Views","직접 광고")
                    .replace("APV™ Automated Passive Views","자동화된 시청자")
                    .replace("Instagram","인스타그램")
                    .replace("Post","게시물")
                    .replace("Likes","좋아요")
                    /*.replace("LQ","낮은퀄")
                    .replace("HQ","일반퀄")
                    .replace("Mixed","혼합")
                    .replace("Real","높은퀄")
                    .replace("Active","실제유저")*/
                    .replace("GEO Targeted","국가 기반")
                    .replace("Impressions","노출")
                    .replace("Reach","도달")
                    .replace("Profile Visits","프로필 방문")
                    .replace("Saves","저장")
                    .replace("Verified","검증된")
                    .replace("Followers","팔로워")
                    .replace("No Refill","리필X")
                    .replace("Refill Guarantee","리필 보장")
                    .replace("Aged","고령의 프로필")
                    .replace("Specials","서비스 모음")
                    .replace("\uD835\uDC12\uD835\uDC28\uD835\uDC2E\uD835\uDC2D\uD835\uDC21 \uD835\uDC0A\uD835\uDC28\uD835\uDC2B\uD835\uDC1E\uD835\uDC1A","한국인")
                    .replace("\uD835\uDC09\uD835\uDC1A\uD835\uDC29\uD835\uDC1A\uD835\uDC27","일본인")
                    .replace("\uD835\uDC02\uD835\uDC21\uD835\uDC22\uD835\uDC27\uD835\uDC1A","중국인")
                    .replace("Views","조회수")
                    .replace("Comments","댓글")
                    .replace("Profiles","프로필")
                    .replace("\uD835\uDC00\uD835\uDC2E\uD835\uDC2D\uD835\uDC28","자동")
                    .replace("Story","스토리")
                    .replace("Reel","릴")
                    .replace("Live","라이브")
                    .replace("Video","영상")
                    .replace("Email","이메일")
                    .replace("Extractor","추출")
                    .replace("Shorts","쇼츠")
                    .replace("\uD835\uDC11\uD835\uDC00\uD835\uDC15-\uD835\uDC06\uD835\uDC12™","구글검색 광고")
                    .replace("Language Targeted","언어 기반")
                    .replace("GEO Targeted","국가 선택")
                    .replace("\uD835\uDC11\uD835\uDC00\uD835\uDC15™","직접 광고")
                    .replace("\uD835\uDC11\uD835\uDC00\uD835\uDC15-\uD835\uDC0C\uD835\uDC13\uD835\uDC12™","구글검색 광고(수익)")
                    .replace("Subscribers","구독자")
                    .replace("YouTube","유튜브")
                    .replace("South Korea","한국")
                    .replace("South-Korea","한국")
                    .replace("Shares","공유")
                    .replace("Choose Referrer","플랫폼 선택")
                    .replace("Choose Speed","속도 선택")
                    .replace("WorldWide","전세계")
                    .replace("Worldwide","전세계")
                    .replace("Choose GEO","국가 선택")
                    .replace("Japan","일본")
                    .replace("USA","미국")
                    .replace("Stream","스트림")
                    .replace("Premiere","프리미어")
                    .replace("TikTok","틱톡")
                    .replace("Facebook","페이스북")
                    .replace("FanPage","팬 페이지")
                    .replace("Others","기타")
                    .replace("Twitch","트위치")
                    .replace("Twitter","트위터")
                    .replace("Spotify","스포티파이")
                    .replace("Track Plays","트랙 재생")
                    .replace("Monthly Listeners","월별 리스너")
                    .replace("Soundcloud","사운드클라우드")
                    .replace("Pinterest","핀터레스트")
                    .replace("Telegram","텔레그램")
                    .replace("Website Traffic","웹사이트 트래픽")
                    .replace("Website Premium Traffic Packages","웹사이트 넘치는 트래픽")
                    .replace("Custom GEO","국가 선택")
                    .replace("GEO","국가 선택")
                    .replace("China","중국")
                    .replace("Hong Kong","홍콩")
                    .replace("Mobile","핸드폰")
                    .replace("Website Social Signals","웹사이트 소셜 신호")
                    .replace("Marketplace","마켓플레이스");

            serviceList.setCategory(category);
        }
    }

    //@Transactional(rollbackFor = Exception.class) //카테고리중 안쓰는거 제외시키고 리턴
    public List<String> getCategories(){
        /*List<String> categories = serviceListRepository.findDistinctCategory();*/

        List<String> categories = Arrays.asList(
                "인스타그램 게시물 좋아요 [ LQ/HQ Mixed ]",
                "인스타그램 게시물 좋아요 [ HQ/Real ]",
                "인스타그램 게시물 좋아요 [ Real & Active ] \uD83D\uDD25",
                "인스타그램 게시물 좋아요 [ 국가 선택 ]",
                "인스타그램 노출 / 도달 / 프로필 방문 / 저장",
                "인스타그램 검증된 팔로워 \uD83D\uDD25",
                "인스타그램 팔로워 [리필X]",
                "인스타그램 팔로워 [+ 리필 보장]",
                "인스타그램 팔로워 [ 국가 선택/고령의 프로필 ]",
                "인스타그램 [ 한국인 ] 서비스 모음", //한국
                "인스타그램 [ 일본인 ] 서비스 모음", //일본
                "인스타그램 [ 중국인 ] 서비스 모음", //중국
                "인스타그램 조회수",
                "인스타그램 게시물 댓글",
                "인스타그램 게시물 댓글 [ 검증된 프로필 ] \uD83D\uDD25",
                /*"인스타그램 Comment 좋아요",*/
                "인스타그램 자동 게시물 좋아요",
                "인스타그램 자동 노출 / 프로필 방문 / 도달",
                "인스타그램 자동 조회수",
                "인스타그램 자동 댓글",
                "인스타그램 스토리",
                "인스타그램 TV [IGTV]",
                "인스타그램 릴",
                "인스타그램 라이브 영상",
                "인스타그램 이메일 추출",

                "유튜브 영상/쇼츠 조회수",
                "유튜브 영상/쇼츠 조회수 [ 구글검색 광고 - 언어 기반 ]", //𝐑𝐀𝐕-𝐆𝐒
                "유튜브 영상/쇼츠 조회수 [ 구글검색 광고 - 국가 선택 ]", //𝐑𝐀𝐕-𝐆𝐒
                "유튜브 영상/쇼츠 조회수 [ 직접 광고 - 언어 기반 ]", //𝐑𝐀𝐕
                "유튜브 영상/쇼츠 조회수 [ 직접 광고 - 국가 선택 ]", //𝐑𝐀𝐕
                "유튜브 영상/쇼츠 조회수 [ 구글검색 광고(수익) - 언어 기반 ]", //𝐑𝐀𝐕-𝐌𝐓𝐒
                "유튜브 영상/쇼츠 조회수 [ 구글검색 광고(수익) - 국가 선택 ]", //𝐑𝐀𝐕-𝐌𝐓𝐒
                "유튜브 구독자",
                "유튜브 좋아요",
                "유튜브 댓글",
                "유튜브 한국 공유 [ + 플랫폼 선택 ]",
                "유튜브 공유  [ + 속도 선택 ]",
                "유튜브 전세계 공유 [ + 플랫폼 선택 ]",
                "유튜브 공유 [ + 국가 선택 ]",
                "유튜브 일본 공유 [ + 플랫폼 선택 ]",
                "유튜브 미국 공유 [ + 플랫폼 선택 ]",
                "유튜브 자동 영상/쇼츠 조회수", //𝐀𝐮𝐭𝐨
                "유튜브 자동 영상 공유", //𝐀𝐮𝐭𝐨
                "유튜브 라이브 스트림 / 프리미어 [직접 광고]",
                "유튜브 라이브 스트림 / 프리미어 [자동화된 시청자]",

                "틱톡 [조회수 / 공유 ]",
                "틱톡 팔로워",
                "틱톡 좋아요",
                "틱톡 댓글",
                "틱톡 자동 조회수",

                "페이스북 게시물 좋아요",
                "페이스북 팬 페이지 좋아요",
                "페이스북 영상 조회수",
                "페이스북 라이브 스트림 조회수",
                "페이스북 [기타]",

                "❖ 트위치",

                "트위터 팔로워",
                "트위터 [조회수 / 기타]",

                "스포티파이 트랙 재생",
                "스포티파이 월별 리스너",
                "사운드클라우드",
                "핀터레스트",
                "텔레그램",
                "Quora.com 조회수",
                "Vimeo.com 조회수",
                "❖ Rumble.com 조회수",

                "\uD83D\uDE80\uD83C\uDF10 웹사이트 트래픽 - 전세계 [ + 플랫폼 선택 ]",
                "\uD83D\uDE80\uD83C\uDF10 웹사이트 트래픽 - 전세계 - from Exchange Platforms (PTC)",
                "\uD83D\uDE80\uD83C\uDF10 웹사이트 트래픽 - 국가 선택",
                "\uD83D\uDCE6 웹사이트 넘치는 트래픽 [ 국가 선택 ]",
                "\uD83D\uDE80\uD83C\uDDF0\uD83C\uDDF7 웹사이트 트래픽 from 한국 [ + 플랫폼 선택 ]",
                "\uD83D\uDE80\uD83C\uDDEF\uD83C\uDDF5 웹사이트 트래픽 from 일본 [ + 플랫폼 선택 ]",
                "\uD83D\uDE80\uD83C\uDDE8\uD83C\uDDF3 웹사이트 트래픽 from 중국 [ + 플랫폼 선택 ]",
                "\uD83D\uDE80\uD83C\uDDED\uD83C\uDDF0 웹사이트 트래픽 from 홍콩 [ + 플랫폼 선택 ]",
                "\uD83D\uDE80\uD83C\uDDFA\uD83C\uDDF8 웹사이트 트래픽 from 미국 [ + 플랫폼 선택 ]",
                "\uD83D\uDCF1 웹사이트 트래픽 [ 100% 핸드폰 - 국가 선택 ] \uD83D\uDD25",
                "\uD83D\uDD17 Website SEO & Backlinks",
                "\uD83D\uDCCA 웹사이트 소셜 신호",
                "\uD83C\uDFAF  Website Niche Traffic - Cryptocurrency  [ + 국가 선택 ]",
                "\uD83C\uDFAF Website Niche Traffic - Betting/Gambling/Casino [ + 국가 선택 ]",

                "❖ Crypto.com 마켓플레이스 조회수", //💎 Crypto.com Marketplace 조회수 🔥🔥
                "❖ SuperRare.com 조회수" //💎 SuperRare.com 조회수 🔥🔥
                );

        return categories;
    }


    /*-----------------------------------------------User 관련 서비스------------------------------------------*/
    @Transactional(rollbackFor = Exception.class)
    public Optional<User> validNickname(String nickname){
        return userRepository.findByNickname(nickname);
    }

    @Transactional(rollbackFor = Exception.class)
    public Optional<User> getUserByPrivateId(String privateId){
        return userRepository.findByPrivateid(privateId);
    }



    /*---------------------------------------------Comment 관련 서비스----------------------------------------*/
/*    @Transactional(rollbackFor = Exception.class)
    public List<CommentDTO> getCommentsByServiceNum(String ServiceNum){
        List<Comment> comments = commentRepository.findByServiceNum(ServiceNum);

        List<CommentDTO> dtos = new ArrayList<CommentDTO>();
        for (int i = 0; i < comments.size(); i++){
            Comment c = comments.get(i);
            CommentDTO dto = CommentDTO.createCommentDto(c);
            dtos.add(dto);
        }

        return dtos;
    }*/

}




/*            if (serviceList.getName().toLowerCase().contains("instagram like")) {
                String korname = String.format("인스타그램 좋아요 -- %s원 / 1,000개", decFormat.format(price));
                serviceList.setKorname(korname);
            } else if (serviceList.getName().toLowerCase().contains("instagram follower")) {
                String korname = String.format("인스타그램 팔로워 -- %s원 / 1,000개", decFormat.format(price));
                serviceList.setKorname(korname);
            } else if (serviceList.getName().toLowerCase().contains("instagram view")) {
                String korname = String.format("인스타그램 뷰 -- %s원 / 1,000개", decFormat.format(price));
                serviceList.setKorname(korname);
            } else if (serviceList.getName().toLowerCase().contains("instagram auto like")) {
                String korname = String.format("인스타그램 자동좋아요 -- %s원 / 1,000개", decFormat.format(price));
                serviceList.setKorname(korname);
            } else if (serviceList.getName().toLowerCase().contains("nft marketplace")) {
                String korname = String.format("Crypto.com NFT 마켓플레이스 뷰 -- %s원 / 1,000개", decFormat.format(price));
                serviceList.setKorname(korname);
            } else if (serviceList.getName().toLowerCase().contains("nft view")) {
                String korname = String.format("SuperRare.com NFT 뷰 -- %s원 / 1,000개", decFormat.format(price));
                serviceList.setKorname(korname);
            } else if (serviceList.getName().toLowerCase().contains("youtube auto unique view")) {
                String korname = String.format("유튜브 자동 뷰 -- %s원 / 1,000개", decFormat.format(price));
                serviceList.setKorname(korname);
            } else if (serviceList.getName().toLowerCase().contains("youtube live view")) {
                String korname = String.format("유튜브 라이브뷰 -- %s원 / 1,000개", decFormat.format(price));
                serviceList.setKorname(korname);
            } else if (serviceList.getName().toLowerCase().contains("youtube pre-premiere")) {
                String korname = String.format("유튜브 premiere waiting 뷰 -- %s원 / 1,000개", decFormat.format(price));
                serviceList.setKorname(korname);
            } else if (serviceList.getName().toLowerCase().contains("youtube like")) {
                String korname = String.format("유튜브 좋아요 -- %s원 / 1,000개", decFormat.format(price));
                serviceList.setKorname(korname);
            } else if (serviceList.getName().toLowerCase().contains("youtube community post like")) {
                String korname = String.format("유튜브 커뮤니티 좋아요 -- %s원 / 1,000개", decFormat.format(price));
                serviceList.setKorname(korname);
            } else if (serviceList.getName().toLowerCase().contains("youtube comment like")) {
                String korname = String.format("유튜브 댓글 좋아요 -- %s원 / 1,000개", decFormat.format(price));
                serviceList.setKorname(korname);
            } else if (serviceList.getName().toLowerCase().contains("youtube dislike")) {
                String korname = String.format("유튜브 싫어요 -- %s원 / 1,000개", decFormat.format(price));
                serviceList.setKorname(korname);
            } else if (serviceList.getName().toLowerCase().contains("instagram female like")) {
                String korname = String.format("인스타그램 여성 좋아요 -- %s원 / 1,000개", decFormat.format(price));
                serviceList.setKorname(korname);
            } else if (serviceList.getName().toLowerCase().contains("instagram male like")) {
                String korname = String.format("인스타그램 남성 좋아요 -- %s원 / 1,000개", decFormat.format(price));
                serviceList.setKorname(korname);
            }*/
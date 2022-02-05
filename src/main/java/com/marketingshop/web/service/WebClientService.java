package com.marketingshop.web.service;

import com.marketingshop.web.dto.OrderForm;
import com.marketingshop.web.entity.OrderStatus;
import com.marketingshop.web.entity.ServiceList;
import com.marketingshop.web.entity.Subscription;
import com.marketingshop.web.entity.User;
import com.marketingshop.web.repository.*;
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

        if (orderForm.getType().equals("12")){ //{{!12 Default 2개}}
            if (orderForm.getLink().isEmpty() || orderForm.getQuantity().isEmpty()) return "빈칸을 기입해주세요";
            if ( Integer.parseInt(orderForm.getQuantity()) < min ||
                 Integer.parseInt(orderForm.getQuantity()) > max)
                return "최소, 최대 범위를 확인해주세요";
            params.add("link", orderForm.getLink());
            params.add("quantity", orderForm.getQuantity());
        } else if (orderForm.getType().equals("100")){ //{{!100 Subscriptions 3개}}
            if (orderForm.getUsername().isEmpty() || orderForm.getMin().isEmpty() || orderForm.getMax().isEmpty() || orderForm.getPosts().isEmpty()) return "빈칸을 기입해주세요";
            if ( Integer.parseInt(orderForm.getMin()) < min ||
                 Integer.parseInt(orderForm.getMax()) > max ||
                 Integer.parseInt(orderForm.getMin()) > Integer.parseInt(orderForm.getMax()))
                return "최소, 최대 범위를 확인해주세요";
            params.add("username", orderForm.getUsername());
            params.add("min", orderForm.getMin());
            params.add("max", orderForm.getMax());
            params.add("posts", orderForm.getPosts());
            params.add("delay", "0"); //나중에 설정값 넣을수도
        } else if (orderForm.getType().equals("14") || orderForm.getType().equals("2")){ //{{!14 Custom Comments Package 2개}} {{!2 Custom Comments 3개}}
            if (orderForm.getLink().isEmpty() || orderForm.getComments().isEmpty()) return "빈칸을 기입해주세요";

            String LINE_SEPERATOR=System.getProperty("line.separator");
            int quantity = orderForm.getComments().split(LINE_SEPERATOR).length;
            System.out.println(quantity);
            if ( quantity < min || quantity > max ) return "최소, 최대 범위를 확인해주세요"; //직접 댓글 개수 구해야함

            params.add("link", orderForm.getLink());
            params.add("comments", orderForm.getComments());
        } else if (orderForm.getType().equals("10")){ //{{!10 Package 1개}}
            if (orderForm.getLink().isEmpty()) return "빈칸을 기입해주세요";
            params.add("link", orderForm.getLink());
        } /*else if (orderForm.getType().equals("15"))//{{!15 Comment Likes 3개}} 임의 추정
            params.add("link", orderForm.getLink());
            params.add("quantity", orderForm.getQuantity());
            params.add("comment_username", orderForm.getComment_username);
        }*/
        System.out.println(orderForm);
        System.out.println(params);

        User user = userRepository.findByPrivateid(privateid).get();

        if (!orderForm.getType().equals("100")) {
            int price = Integer.parseInt(orderForm.getCharge().replace(",", ""));
            if (user.getBalance() - price < 0)
                return "잔액이 부족합니다";
            user.setBalance(user.getBalance() - price);
        }
        /*String orderid = webClient.post().uri("/api/v2") //주문 오류입니다. 삭제된 서비스일 수 있습니다.
                .bodyValue(params)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONParser jsonParser = new JSONParser();
        JSONObject orderidJson = (JSONObject)jsonParser.parse(orderid);
        Long order = (Long) orderidJson.get("order");*/ //없어진 서비스 주문하면 어찌되는지 확인하기
        Long order = 7504585l;

        serviceList.salesPlus();
        try {
            if (!orderForm.getType().equals("100")) {
                OrderStatus orderStatus = orderStatusService.getOrderStatus(order); //smm api 값들 업데이트 시킴
                orderStatus.inputValueUpdate(user, serviceList, orderForm);
                orderStatusRepository.save(orderStatus);
            } else if (orderForm.getType().equals("100")) {
                Subscription subscription = subscriptionService.getSubscription(order, user, serviceList); //type에 따라 아예 저장하는 테이블이 달라짐
                subscription.inputValueUpdate(orderForm);
                subscriptionRepository.save(subscription);
            }
        }catch(Exception e){
            return "주문은 정상적으로 처리되었으나 주문내역 업데이트 중 오류가 발생했습니다";
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

            String korname = String.format("%s -- %s원 / 1,000개", serviceList.getName(), decFormat.format(price));
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
        }
    }

    //@Transactional(rollbackFor = Exception.class) //카테고리중 안쓰는거 제외시키고 리턴
    public List<String> getCategories(){
        /*List<String> categories = serviceListRepository.findDistinctCategory();
        categories.remove("- Private");
        categories.remove("♛ Popular on SMMKings");
        categories.remove("\uD83D\uDE80\uD83C\uDDEC\uD83C\uDDE7 Website Traffic from UK [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDEE\uD83C\uDDF3 Website Traffic from India [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDE7\uD83C\uDDF7 Website Traffic from Brazil [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDEE\uD83C\uDDE9 Website Traffic from Indonesia [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDE9\uD83C\uDDEA Website Traffic from Germany [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDEB\uD83C\uDDF7 Website Traffic from France [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDF9\uD83C\uDDF7 Website Traffic from Turkey [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDF7\uD83C\uDDFA Website Traffic from Russia [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDF3\uD83C\uDDF1 Website Traffic from Netherlands [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDF5\uD83C\uDDF1 Website Traffic from Poland [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDEE\uD83C\uDDF9 Website Traffic from Italy [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDEA\uD83C\uDDF8 Website Traffic from Spain [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDE8\uD83C\uDDE6 Website Traffic from Canada [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDFA\uD83C\uDDE6 Website Traffic from Ukraine [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDE8\uD83C\uDDFF Website Traffic from Czech [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDF5\uD83C\uDDF0  Website Traffic from Pakistan [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDEA\uD83C\uDDEC  Website Traffic from Egypt [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDF9\uD83C\uDDED Website Traffic from Thailand [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDF9\uD83C\uDDFC Website Traffic from Taiwan [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDFB\uD83C\uDDF3 Website Traffic from Vietnam [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDF8\uD83C\uDDEC Website Traffic from Singapore [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDF5\uD83C\uDDF9 Website Traffic from Portugal [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDF7\uD83C\uDDF4 Website Traffic from Romania [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDF2\uD83C\uDDFD Website Traffic from Mexico [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDF8\uD83C\uDDEA Website Traffic from Sweden [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDE8\uD83C\uDDED Website Traffic from Switzerland [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDF2\uD83C\uDDE9 Website Traffic from Moldova [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDE6\uD83C\uDDFA Website Traffic from Australia [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDFF\uD83C\uDDE6 Website Traffic from South Africa [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDEE\uD83C\uDDEA Website Traffic from Ireland [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDEE\uD83C\uDDF7 Website Traffic from Iran [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDEE\uD83C\uDDF1 Website Traffic from Israel [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDE6\uD83C\uDDF7 Website Traffic from Argentina [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDE7\uD83C\uDDE9 Website Traffic from Bangladesh [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDE8\uD83C\uDDF4 Website Traffic from Colombia [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDEB\uD83C\uDDEE Website Traffic from Finland [ + Choose Referrer ]");
        categories.remove("\uD83D\uDE80\uD83C\uDDF5\uD83C\uDDED Website Traffic from Philippines [ + Choose Referrer ]");
        categories.remove("YouTube Thailand Shares [ + Choose Referrer ]"); //❖ 🇹🇭
        categories.remove("YouTube India Shares [ + Choose Referrer ]"); //❖ 🇮🇳
        categories.remove("YouTube Vietnam Shares [ + Choose Referrer ]"); //❖ 🇻🇳
        categories.remove("YouTube Brazil Shares [ + Choose Referrer ]"); //❖ 🇧🇷
        categories.remove("YouTube Turkey Shares [ + Choose Referrer ]"); //❖ 🇹🇷
        categories.remove("YouTube Italy Shares [ + Choose Referrer ]"); //❖ 🇮🇹
        categories.remove("YouTube Philippines Shares [ + Choose Referrer ]"); //❖ 🇵🇭
        categories.remove("YouTube Canada Shares [ + Choose Referrer ]"); //❖ 🇨🇦
        categories.remove("YouTube Indonesia Shares [ + Choose Referrer ]"); //❖ 🇮🇩
        categories.remove("YouTube Taiwan Shares [ + Choose Referrer ]"); //❖ 🇹🇼
        categories.remove("YouTube Russia Shares [ + Choose Referrer ]"); //❖ 🇷🇺
        categories.remove("YouTube Nigeria Shares [ + Choose Referrer ]"); //❖ 🇳🇬
        categories.remove("YouTube Bangladesh Shares [ + Choose Referrer ]"); //❖ 🇧🇩
        categories.remove("YouTube Pakistan Shares [ + Choose Referrer ]"); //❖ 🇵🇰*/

        List<String> categories = Arrays.asList(
                "Instagram Post Likes [ LQ/HQ Mixed ]",
                "Instagram Post Likes [ HQ/Real ]",
                "Instagram Post Likes [ Real & Active ] \uD83D\uDD25",
                "Instagram Impressions / Reach / Profile Visits / Saves",
                "Instagram Followers [No Refill]",
                "Instagram Followers [+ Refill Guarantee]",
                "Instagram Followers [ GEO Targeted/Aged ]",
                "Instagram [ \uD835\uDC12\uD835\uDC28\uD835\uDC2E\uD835\uDC2D\uD835\uDC21 \uD835\uDC0A\uD835\uDC28\uD835\uDC2B\uD835\uDC1E\uD835\uDC1A ] Specials", //한국
                "Instagram [ \uD835\uDC09\uD835\uDC1A\uD835\uDC29\uD835\uDC1A\uD835\uDC27 ] Specials", //일본
                "Instagram [ \uD835\uDC02\uD835\uDC21\uD835\uDC22\uD835\uDC27\uD835\uDC1A ] Specials", //중국
                "Instagram Views",
                "Instagram Post Comments",
                /*"Instagram Comment Likes",*/
                "Instagram \uD835\uDC00\uD835\uDC2E\uD835\uDC2D\uD835\uDC28 Post Likes",
                "Instagram \uD835\uDC00\uD835\uDC2E\uD835\uDC2D\uD835\uDC28 Impressions / Profile Visits / Reach",
                "Instagram \uD835\uDC00\uD835\uDC2E\uD835\uDC2D\uD835\uDC28 Views",
                "Instagram \uD835\uDC00\uD835\uDC2E\uD835\uDC2D\uD835\uDC28 Comments",
                "Instagram Story",
                "Instagram TV [IGTV]",
                "Instagram Reel",
                "Instagram Live Video",
                "Instagram Email Extractor",

                "YouTube Video/Shorts Views",
                "YouTube Video/Shorts Views [ \uD835\uDC11\uD835\uDC00\uD835\uDC15-\uD835\uDC06\uD835\uDC12™ - Language Targeted ]", //𝐑𝐀𝐕-𝐆𝐒
                "YouTube Video/Shorts Views [ \uD835\uDC11\uD835\uDC00\uD835\uDC15-\uD835\uDC06\uD835\uDC12™ - GEO Targeted ]", //𝐑𝐀𝐕-𝐆𝐒
                "YouTube Video/Shorts Views [ \uD835\uDC11\uD835\uDC00\uD835\uDC15™ - Language Targeted ]", //𝐑𝐀𝐕
                "YouTube Video/Shorts Views [ \uD835\uDC11\uD835\uDC00\uD835\uDC15™ - GEO Targeted ]", //𝐑𝐀𝐕
                "YouTube Video/Shorts Views [ \uD835\uDC11\uD835\uDC00\uD835\uDC15-\uD835\uDC0C\uD835\uDC13\uD835\uDC12™ - Language Targeted ]", //𝐑𝐀𝐕-𝐌𝐓𝐒
                "YouTube Video/Shorts Views [ \uD835\uDC11\uD835\uDC00\uD835\uDC15-\uD835\uDC0C\uD835\uDC13\uD835\uDC12™ - GEO Targeted ]", //𝐑𝐀𝐕-𝐌𝐓𝐒
                "YouTube Subscribers",
                "YouTube Likes",
                "YouTube Comments",
                "YouTube South-Korea Shares [ + Choose Referrer ]",
                "YouTube Shares  [ + Choose Speed ]",
                "YouTube Worldwide Shares [ + Choose Referrer ]",
                "YouTube Shares [ + Choose GEO ]",
                "YouTube Japan Shares [ + Choose Referrer ]",
                "YouTube USA Shares [ + Choose Referrer ]",
                "YouTube \uD835\uDC00\uD835\uDC2E\uD835\uDC2D\uD835\uDC28 Video/Shorts Views", //𝐀𝐮𝐭𝐨
                "YouTube \uD835\uDC00\uD835\uDC2E\uD835\uDC2D\uD835\uDC28 Video Shares", //𝐀𝐮𝐭𝐨
                "YouTube Live Stream / Premiere [RAV™ - Real & Active Views]",
                "YouTube Live Stream / Premiere [APV™ Automated Passive Views]",

                "TikTok [Views / Shares ]",
                "TikTok Followers",
                "TikTok Likes",
                "TikTok Comments",
                "TikTok \uD835\uDC00\uD835\uDC2E\uD835\uDC2D\uD835\uDC28 Views",

                "Facebook Post Likes",
                "Facebook FanPage Likes",
                "Facebook Video Views",
                "Facebook Live Stream Views",
                "Facebook [Others]",

                "Twitter Followers",
                "Twitter [Views / Others]",

                "Spotify Track Plays",
                "Spotify Monthly Listeners",
                "Soundcloud",
                "Pinterest",
                "Telegram",
                "Quora.com Views",
                "Vimeo.com Views",

                "\uD83D\uDE80\uD83C\uDF10 Website Traffic - WorldWide [ + Choose Referrer ]",
                "\uD83D\uDE80\uD83C\uDF10 Website Traffic - Choose GEO",
                "\uD83D\uDE80\uD83C\uDDF0\uD83C\uDDF7 Website Traffic from South Korea [ + Choose Referrer ]",
                "\uD83D\uDE80\uD83C\uDDEF\uD83C\uDDF5 Website Traffic from Japan [ + Choose Referrer ]",
                "\uD83D\uDE80\uD83C\uDDE8\uD83C\uDDF3 Website Traffic from China [ + Choose Referrer ]",
                "\uD83D\uDE80\uD83C\uDDED\uD83C\uDDF0 Website Traffic from Hong Kong [ + Choose Referrer ]",
                "\uD83D\uDE80\uD83C\uDDFA\uD83C\uDDF8 Website Traffic from USA [ + Choose Referrer ]",
                "\uD83D\uDCF1 Website Traffic [100% Mobile - Custom GEO]",
                "\uD83D\uDD17 Website SEO & Backlinks",
                "\uD83D\uDCCA Website Social Signals",
                "\uD83C\uDFAF  Website Niche Traffic - Cryptocurrency  [ + Choose GEO ]",
                "\uD83C\uDFAF Website Niche Traffic - Betting/Gambling/Casino [ + Choose GEO ]",

                "❖ Crypto.com Marketplace Views", //💎 Crypto.com Marketplace Views 🔥🔥
                "❖ SuperRare.com Views" //💎 SuperRare.com Views 🔥🔥
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
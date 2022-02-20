package com.marketingshop.web.controller;

import com.marketingshop.web.annotation.LoginUser;
import com.marketingshop.web.dto.CommentDTO;
import com.marketingshop.web.entity.Comment;
import com.marketingshop.web.entity.OrderStatus;
import com.marketingshop.web.entity.SessionUser;
import com.marketingshop.web.entity.User;
import com.marketingshop.web.repository.CommentRepository;
import com.marketingshop.web.repository.OrderStatusRepository;
import com.marketingshop.web.repository.ServiceListRepository;
import com.marketingshop.web.repository.UserRepository;
import com.marketingshop.web.service.WebClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

@Controller
@Slf4j
public class CommentController {

	@Autowired
	private CommentRepository commentRepository;
	@Autowired
	private ServiceListRepository serviceListRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private WebClientService webClientService;
	@Autowired
	private OrderStatusRepository orderStatusRepository;

	@PostMapping("/save")
	@Transactional(rollbackFor = Exception.class)
	public String save(Long orderid, CommentDTO commentDTO, String curpage, @LoginUser SessionUser user) {
		OrderStatus orderStatus = orderStatusRepository.findById(orderid).get();

		Comment comment = commentDTO.toEntity(serviceListRepository, userRepository, orderStatus);
		Comment saved = commentRepository.save(comment);

		orderStatus.setComment(saved);

		String serviceNum = commentDTO.getServicenum();
		Float star = Math.round(commentRepository.findAVGByServiceNum(serviceNum)*10)/10.0f;
		serviceListRepository.getById(serviceNum).setStar(star);

		User realUser = userRepository.findByPrivateid(user.getPrivateid()).get();
		int refund = Integer.parseInt(orderStatus.getCharge().replace(",", "")) / 10;
		realUser.setBalance(realUser.getBalance() + refund); //save없이도 저장될듯. 본체를 부른거라

		return "redirect:"+curpage;
	}

	@PostMapping("/changeComment")
	@Transactional(rollbackFor = Exception.class)
	public String changeComment(Long commentid, CommentDTO commentDTO, String curpage) {
		Optional<Comment> comment = commentRepository.findById(commentid);
		comment.get().update(commentDTO);

		String serviceNum = commentDTO.getServicenum();
		Float star = Math.round(commentRepository.findAVGByServiceNum(serviceNum)*10)/10.0f;
		serviceListRepository.getById(serviceNum).setStar(star);

		return "redirect:"+curpage;
	}

}
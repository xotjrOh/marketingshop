package com.marketingshop.web.entity;

import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity //provider google providerId 10974...686
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceList { //리셋시 지움 지움 생김
	@Id
	private String service;
	//@GeneratedValue(strategy = GenerationType.IDENTITY)
	//private Long id;

	private String name;
	private String type;
	private String category;
	private String rate;
	private String min;
	private String max;
	private String refill;

	//이제 추가하는 내용들
	@ColumnDefault("0")
	@Column(insertable=false, updatable=false) //??해결위해 재시작시 두개다 지움
	private int price;
	@Column(insertable=false, updatable=false)// ??해결위해 재시작시 업데이트 지움
	private String korname;
	@Column//(insertable=false, updatable=false)
	private String korcategory;

	@Column(insertable=false, updatable=false, length=1200)
	private String description;

	@ColumnDefault("0")
	@Column(insertable=false, updatable=false)
	private int sequence;

	@ColumnDefault("0")
	private int sales;
	@ColumnDefault("5")
	@Column//(updatable = false) //test로 리셋할때 업데이트 못하게 해야함
	private Float star;

	private String timetocomplete;

	public void salesPlus(){
		sales += 1;
	}

	/*public ServiceList(String service, String name, String type, String category, String rate, String min, String max, boolean refill) {
		this.service = service;
		this.name = name;
		this.type = type;
		this.category = category;
		this.rate = rate;
		this.min = min;
		this.max = max;
		this.refill = refill;
		price = Integer.parseInt(rate)*2000;
		korname= "분류되지 않은 이름";
	}*/
}

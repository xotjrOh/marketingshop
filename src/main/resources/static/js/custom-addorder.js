/*--------------------------------------add order에만 해당하는 거------------------------------------------*/
/*if(document.readyState == "loading"){
    let curCategory = document.querySelector("#field-orderform-fields-cur-category").value;
    $('#orderform-category').val(curCategory).trigger("change");
    *//*$('#orderform-category').val(curCategory).prop("selected");*//*
}*/
$(document).ready(function(){
	console.log("제이쿼리로 문서 불러옴");
});

document.addEventListener('DOMContentLoaded', function(){
    console.log("문서불러와짐");
})

if(document.readyState == "loading"){
    console.log("로딩상황");
    /*$('#orderform-category').val(curCategory).prop("selected");*/
}

document.querySelector("#field-orderform-fields-cur-service").onload = function(){
    console.log("서비스 객체 소환");
}

document.querySelector("#field-orderform-fields-cur-category").onload = function(){
    console.log("카테고리 객체 소환");
}

/*document.querySelector("#field-orderform-fields-cur-category").onload = function(){
    let curCategory = document.querySelector("#field-orderform-fields-cur-category").value;
    $('#orderform-category').val(curCategory).trigger("change");
}*/

$(window).load(function(){
	console.log("제이쿼리 로드");
})

window.onload = function(){ //다른걸로 덮어쓰기로 해결
    console.log("가장 늦게 열리는거");
    /*let curService = document.querySelector("#field-orderform-fields-cur-service").value;
    $('#orderform-service').val(curService).trigger("change");
    console.log("현재서비스 : ",curService);*/
}









/*window.addEventListener("load",()=>{
    let curService = document.querySelector("#field-orderform-fields-cur-service").value;
    $('#orderform-service').val(curService).trigger("change");
});*/

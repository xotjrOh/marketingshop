/*--------------------------------------add order에만 해당하는 거------------------------------------------*/
if(document.readyState == "loading"){
    let curCategory = document.querySelector("#field-orderform-fields-cur-category").value;
    $('#orderform-category').val(curCategory).trigger("change");
    /*$('#orderform-category').val(curCategory).prop("selected");*/
}

/*window.onload = function(){ //다른걸로 덮어쓰기로 해결
    let curService = document.querySelector("#field-orderform-fields-cur-service").value;
    $('#orderform-service').val(curService).trigger("change");
    console.log("현재서비스 : ",curService);
}*/

window.onload = function(){
    window.setTimeout(function(){
        let curService = document.querySelector("#field-orderform-fields-cur-service").value;
        $('#orderform-service').val(curService).trigger("change");
        reorder();
},200)};





/*window.addEventListener("load",()=>{
    let curService = document.querySelector("#field-orderform-fields-cur-service").value;
    $('#orderform-service').val(curService).trigger("change");
});*/

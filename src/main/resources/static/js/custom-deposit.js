/*
function addCharge() { //이거 안씀
    let pg = document.querySelector("#method").value;
    let pay_method;
    if (pg=="kakaopay") pay_method="kakaopay";
    else if (pg=="payco" || pg=="html5_inicis") pay_method="card";
    console.log(pg);
    console.log(pay_method);

    // getter
    let IMP = window.IMP;
    IMP.init('imp67149559');
    let money = document.querySelector("#amount").value;
    console.log(money);
    if (money < 5000){
        alert("최소 5,000원부터 충전이 가능합니다");
        return;
    }

    let msg="";

    IMP.request_pay({ //request_pay(param, callback)
        pg: pg,
        pay_method: pay_method,
        merchant_uid: 'merchant_' + new Date().getTime(),
        amount: money,
        name: "마케팅샵 충전"

        */
/*buyer_tel: '010-1234-5678',*//*
 //pg사에 따라 오류 발생 가능
        */
/*buyer_name: '구매자이름',*//*

        */
/*buyer_email: 'iamport@siot.do',
        buyer_addr: '인천광역시 부평구',
        buyer_postcode: '123-456'*//*

    }, function (rsp) {
        console.log(rsp);
        if (rsp.success) {
            msg += `${money}원 결제가 완료되었습니다.`;
            */
/*msg += '고유ID : ' + rsp.imp_uid;
            msg += '상점 거래ID : ' + rsp.merchant_uid;
            msg += '결제 금액 : ' + rsp.paid_amount;
            msg += '카드 승인번호 : ' + rsp.apply_num;*//*

            $.ajax({
                type: "GET",
                url: "/api/v2/addCharge", //충전 금액값을 보낼 url 설정
                data: {
                    "amount" : money,
                    "pg" : pg,
                    "merchant_uid" : rsp.merchant_uid
                },
            });
        } else {
            msg += '결제에 실패하였습니다.\n';
            msg += '에러내용 : ' + rsp.error_msg;
        }
        alert(msg);
        document.location.href="/customer/deposit/1"; //alert창 확인 후 이동할 url 설정
    });
}
*/


$('ul.nav-tabs li').on('click', function (e) {
    for (let tabList of document.querySelectorAll('ul.nav-tabs li')){
        tabList.classList.remove('active');
    }
    e.preventDefault();
    $(this).tab('show');
})

//클릭했을 경우 클립보드에 복사된다.
function clickCopy(value){
    let tempInput = document.createElement("input");
    tempInput.style = "position: absolute; left: -1000px; top: -1000px"; //input 창 생성 후 위치 선정
    tempInput.value = value; //매개값으로 받은 정보 input 에 삽입
    document.body.appendChild(tempInput); //body태그에 인풋태그 삽입
    tempInput.select(); //해당 인풋테크 select -> value 값이 선택된다.
    document.execCommand("copy"); //해당 값을 복사를 시도
    document.body.removeChild(tempInput); //정상적으로 복사되면 해당 인풋태그를 다시 삭제한다.

    alert(value+" 이 복사되었습니다");
}

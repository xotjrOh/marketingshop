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
    /*if (money < 5000){
        alert("최소 5,000원부터 충전이 가능합니다");
        return;
    }*/

    let msg="";

    IMP.request_pay({ //request_pay(param, callback)
        pg: pg,
        pay_method: pay_method,
        merchant_uid: 'merchant_' + new Date().getTime(),
        amount: money,
        name: "마케팅샵 충전"

        /*buyer_tel: '010-1234-5678',*/ //pg사에 따라 오류 발생 가능
        /*buyer_name: '구매자이름',*/
        /*buyer_email: 'iamport@siot.do',
        buyer_addr: '인천광역시 부평구',
        buyer_postcode: '123-456'*/
    }, function (rsp) {
        console.log(rsp);
        if (rsp.success) {
            msg += `${money}원 결제가 완료되었습니다.`;
            /*msg += '고유ID : ' + rsp.imp_uid;
            msg += '상점 거래ID : ' + rsp.merchant_uid;
            msg += '결제 금액 : ' + rsp.paid_amount;
            msg += '카드 승인번호 : ' + rsp.apply_num;*/
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

window.addEventListener("load", function(){ // userdata값이 header값에 로드시마다 업데이트  //공통->id,돈 업뎃
    let privateId = document.querySelector("#sidebarToggleTop").dataset.privateid;

    fetch(`http://mktingshop.com/api/v2/userByPrivateId?privateId=${privateId}`)
        .then(res => res.json())
        .then(data =>{
            let nicknameBox = document.querySelector("#userDropdown > span");
            nicknameBox.innerHTML=data.nickname;

            let balanceBox = document.querySelector("#balanceBox");
            balanceBox.innerHTML=priceToString(`${data.balance}`);
        });
});

function priceToString(price) { //3자리마다 콤마 표현 / neworder에 있는거 옮겨서 문제되면 다시 복구
    return price.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

function changeNickname(){ //닉네임 변경
    let nicknameVal = document.querySelector("#changeNickname").value;
    let privateId = document.querySelector("#sidebarToggleTop").dataset.privateid;

    fetch(`http://mktingshop.com/api/v2/changeNickname?nickname=${nicknameVal}&privateId=${privateId}`)
        .then(res => res.json())
        .then(data =>{
            if (data==true){
                let nicknameBox = document.querySelector("#userDropdown > span");
                nicknameBox.innerHTML=nicknameVal; //완료 실패 메시지 띄우기
                $('#changeNicknameModal').modal("hide");
                setTimeout(() => alert("변경이 완료되었습니다"),150);
            } else{
                alert("이미 존재하는 닉네임입니다");
            }
        });
}

$(document).ready(function(){
    $('[data-toggle="tooltip"]').tooltip();
}); //툴팁 실행

/*window.addEventListener("beforeunload",()=>{ //페이지 바뀔때뜸
    alert("즐겨찾기를 안하시면 다시 찾기 어려운 사이트입니다.");
});*/

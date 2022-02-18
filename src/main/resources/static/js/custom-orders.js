//별점 마킹 모듈 프로토타입으로 생성
function Rating(){};
Rating.prototype.rate = 0;
Rating.prototype.setRate = function(newrate){
    //별점 마킹 - 클릭한 별 이하 모든 별 체크 처리
    this.rate = newrate;
    document.querySelector("#rate").value=newrate;

    let items = document.querySelectorAll('.rate_radio');
    items.forEach(function(item, idx){
        if(idx < newrate){
            item.checked = true;
        }else{
            item.checked = false;
        }
    });

}
let rating = new Rating();//별점 인스턴스 생성

document.addEventListener('DOMContentLoaded', function(){

    //별점선택 이벤트 리스너
    document.querySelector('.rating').addEventListener('click',function(e){
        let elem = e.target;
        if(elem.classList.contains('rate_radio')){
            rating.setRate(parseInt(elem.value));
        }
    })

    //상품평 작성 글자수 초과 체크 이벤트 리스너
    document.querySelector('.review_textarea').addEventListener('keydown',function(){
        //리뷰 100자 초과 안되게 자동 자름
        let review = document.querySelector('.review_textarea');
        let lengthCheckEx = /^.{100,}$/;
        if(lengthCheckEx.test(review.value)){
            //100자 초과 컷
            review.value = review.value.substr(0,100);
        }
    });

    //저장 전송전 필드 체크 이벤트 리스너
    document.querySelector('#save').addEventListener('click', function(e){
        //별점 선택 안했으면 메시지 표시
        if(rating.rate == 0){
            rating.showMessage('rate');
            return false;
        }
        //리뷰 5자 미만이면 메시지 표시
        if(document.querySelector('.review_textarea').value.length < 5){
            rating.showMessage('review');
            return false;
        }
        //폼 서밋
        document.querySelector(".reviewform").submit();
        alert("리뷰 등록이 완료되었습니다");
    });

});

Rating.prototype.showMessage = function(type){//경고메시지 표시
    switch(type){
        case 'rate':
            //안내메시지 표시
            document.querySelector('.review_rating .warning_msg').style.display = 'block';
            //지정된 시간 후 안내 메시지 감춤
            setTimeout(function(){
                document.querySelector('.review_rating .warning_msg').style.display = 'none';
            },1500);
            break;
        case 'review':
            //안내메시지 표시
            document.querySelector('.review_contents .warning_msg').style.display = 'block';
            //지정된 시간 후 안내 메시지 감춤
            setTimeout(function(){
                document.querySelector('.review_contents .warning_msg').style.display = 'none';
            },1500);
            break;
    }
}




//----------------------------------그대로 change modal로 복사-----------------------
//별점 마킹 모듈 프로토타입으로 생성
function ChangeRating(){};
ChangeRating.prototype.rate = 0;
ChangeRating.prototype.setRate = function(newrate){
    //별점 마킹 - 클릭한 별 이하 모든 별 체크 처리
    this.rate = newrate;
    document.querySelector("#change-rate").value=newrate;

    let items = document.querySelectorAll('.change_rate_radio');
    items.forEach(function(item, idx){
        if(idx < newrate){
            item.checked = true;
        }else{
            item.checked = false;
        }
    });

}
let changeRating = new ChangeRating();//별점 인스턴스 생성

document.addEventListener('DOMContentLoaded', function(){
    //별점선택 이벤트 리스너
    document.querySelector('.change-rating').addEventListener('click',function(e){
        let elem = e.target;
        if(elem.classList.contains('change_rate_radio')){
            changeRating.setRate(parseInt(elem.value));
        }
    })

    //상품평 작성 글자수 초과 체크 이벤트 리스너
    document.querySelector('.review_change_textarea').addEventListener('keydown',function(){
        //리뷰 100자 초과 안되게 자동 자름
        let review = document.querySelector('.review_change_textarea');
        let lengthCheckEx = /^.{100,}$/;
        if(lengthCheckEx.test(review.value)){
            //100자 초과 컷
            review.value = review.value.substr(0,100);
        }
    });

    //저장 전송전 필드 체크 이벤트 리스너
    document.querySelector('#changeComment').addEventListener('click', function(e){
        //별점 선택 안했으면 메시지 표시
        if(changeRating.rate == 0){
            changeRating.showMessage('rate');
            return false;
        }
        //리뷰 5자 미만이면 메시지 표시
        if(document.querySelector('.review_change_textarea').value.length < 5){
            changeRating.showMessage('review');
            return false;
        }
        //폼 서밋
        document.querySelector(".reviewchangeform").submit();
        alert("수정이 완료되었습니다");
/*        let commentDTO ={ //구현이 되었고 db업뎃도 되지만 업뎃된 별표내용이 reload안됨. reload를 호출하기엔 컨트롤러와 동일해짐.
            rate : changeRating.rate,
            content : document.querySelector(".review_change_textarea").value
        };
        console.log(commentDTO);
        let commentid = document.querySelector("#change-rate-commentid").value;
        console.log(commentid);
        fetch(`http://mktingshop.com/api/v2/comments/${commentid}`,{
            method: "PATCH",
            body: JSON.stringify(commentDTO),
            headers:{
                "Content-Type":"application/json"
            }
        })
            .then(res => res.json())
            .then(data =>{
                if (data==true){
                    $('#CommentChangeModal').modal("hide");
                    setTimeout(() => alert("수정이 완료되었습니다"),150);
                } else{
                    alert("알 수 없는 에러입니다");
                }
            });*/
    });

});

ChangeRating.prototype.showMessage = function(type){//경고메시지 표시
    switch(type){
        case 'rate':
            //안내메시지 표시
            document.querySelector('.change_review_rating .warning_msg').style.display = 'block';
            //지정된 시간 후 안내 메시지 감춤
            setTimeout(function(){
                document.querySelector('.change_review_rating .warning_msg').style.display = 'none';
            },1500);
            break;
        case 'review':
            //안내메시지 표시
            document.querySelector('.change_review_contents .warning_msg').style.display = 'block';
            //지정된 시간 후 안내 메시지 감춤
            setTimeout(function(){
                document.querySelector('.change_review_contents .warning_msg').style.display = 'none';
            },1500);
            break;
    }
}


//-----------------------일반적인거-------------------------


function ClickStar(cur){ //orders에서 별 누르면 modal에 자동으로 값 세팅
    let orderid = cur.dataset.orderid;
    let servicenum = cur.dataset.servicenum;

    let serviceNumInputBox = document.querySelector("#rate-servicenum");
    serviceNumInputBox.value=servicenum;

    let orderIdInputBox = document.querySelector("#rate-orderid");
    orderIdInputBox.value=orderid;

    //제목 수정
    let servicename = cur.dataset.servicename;

    let reviewTitle = document.querySelector("#review-title");
    reviewTitle.innerHTML=`${servicenum} 리뷰등록 <br><small>${servicename}</small>`

    //현재페이지 주소 저장
    let pageBox = document.querySelector("#curpage");
    pageBox.value=window.location.pathname;

}

function ClickChangeStar(cur){ //orders에서 별 누르면 change modal에 자동으로 값 세팅

    let commentid = cur.dataset.commentid;
    let star = cur.dataset.star;
    let content = cur.dataset.content;

    let commentidInputBox = document.querySelector("#change-rate-commentid");
    commentidInputBox.value=commentid;

    //별점, 내용 원본 입력
    changeRating.setRate(star);

    let textBox = document.querySelector(".review_change_textarea");
    textBox.innerHTML=content;
    //제목 수정
    let servicenum = cur.dataset.servicenum;
    let servicename = cur.dataset.servicename;

    let serviceNumInputBox = document.querySelector("#change-rate-servicenum");
    serviceNumInputBox.value=servicenum;

    let changeTitle = document.querySelector("#change-title");
    changeTitle.innerHTML=`${servicenum} 리뷰수정 <br><small>${servicename}</small>`

    //현재페이지 주소 저장
    let pageBox = document.querySelector("#change-curpage");
    pageBox.value=window.location.pathname;
}
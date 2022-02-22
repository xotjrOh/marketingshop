/*window.addEventListener("DOMContentLoaded", function(){ //카테고리앞에 아이콘 달기
    let CategoryNodes = document.querySelectorAll(".category-option");
    for (let CategoryNode of CategoryNodes){
        let content = CategoryNode.innerHTML;

        // 우선순위 : 자동 -> 공유 -> 댓글 -> 좋아요 -> 팔로워 -> 뷰
        if (content.toLowerCase().indexOf('automated') == -1 && content.toLowerCase().indexOf('auto') != -1 || content.indexOf('𝐀𝐮𝐭𝐨') != -1){
            content = "&#xf122; " + content;
        } else if (content.toLowerCase().indexOf('share') != -1){
            content = "&#xf1e0; " + content;
        } else if (content.toLowerCase().indexOf('comment') != -1){
            content = "&#xf075; " + content;
        } else if (content.toLowerCase().indexOf('like') != -1 && content.toLowerCase().indexOf('facebook') == -1){
            content = "&#xf004; " + content;
        } else if (content.toLowerCase().indexOf('follower') != -1 || content.toLowerCase().indexOf('subscriber') != -1){
            content = "&#xf007; " + content;
        } else if (content.toLowerCase().indexOf('view') != -1){
            content = "&#xf06e; " + content;
        }

        //플랫폼
        if (content.toLowerCase().indexOf('facebook') != -1){
            if (content.toLowerCase().indexOf('like') != -1){
                content = "&#xf164; " + content;
            }
            content = "&#xf082; " + content;
        } else if (content.toLowerCase().indexOf('instagram') != -1){
            content = "&#xf16d; " + content;
        } else if (content.toLowerCase().indexOf('youtube') != -1){
            content = "&#xf167; " + content;
        } else if (content.toLowerCase().indexOf('twitter') != -1){
            content = "&#xf099; " + content;
        } else if (content.toLowerCase().indexOf('tiktok') != -1){
            content = "&#xe07b; " + content;
        } else if (content.toLowerCase().indexOf('telegram') != -1){
            content = "&#xf3fe; " + content;
        } else if (content.toLowerCase().indexOf('spotify') != -1){
            content = "&#xf1bc; " + content;
        } else if (content.toLowerCase().indexOf('pinterest') != -1){
            content = "&#xf231; " + content;
        } else if (content.toLowerCase().indexOf('quora') != -1){
            content = "&#xf2c4; " + content;
        } else if (content.toLowerCase().indexOf('soundcloud') != -1){
            content = "&#xf1be; " + content;
        } else if (content.toLowerCase().indexOf('vimeo') != -1){
            content = "&#xf27d; " + content;
        }
        CategoryNode.innerHTML = content;
    }
});*/

/*window.addEventListener("DOMContentLoaded", change(document.querySelector("#orderform-category option").value));*/ //첫화면 카테고리에 따라 서비스 업데이트 //뷰단에 바로 만들어두기
window.onload = function(){
    change(document.querySelector("#orderform-category option").value);
}

function change(category){ // 카테고리 선택시 서비스 업데이트, label 채널이름, 유저이름 변경
    let labelName = document.querySelector("#order_username label"); // 유튜브 auto면 label 명칭 변경
    if (category == "YouTube 𝐀𝐮𝐭𝐨 Video Shares" || category == "YouTube 𝐀𝐮𝐭𝐨 Video/Shorts Views"){
        labelName.innerHTML = "채널 이름";
    } else if (category.toLowerCase().indexOf('auto') != -1 || category.indexOf('𝐀𝐮𝐭𝐨') != -1){
        labelName.innerHTML = "유저 이름";
    }

    category = encodeURIComponent(category);
    let target = document.querySelector("#orderform-service");

    fetch(`http://mktingshop.com/api/v2/servicesByCategory?category=${category}`)
        .then(res => res.json())
        .then(serviceLists =>{
            target.options.length = 0;
            for(let serviceList of serviceLists){ //serviceList는 서비스의 모든정보 내포
                let opt = document.createElement("option");
                switch (serviceList.type) {
                    case 'Default':
                        opt.dataset.type=12; //comment에 default인데 0인것도 존재. 오타인듯
                        break;
                    case 'Subscriptions':
                        opt.dataset.type=100;
                        break;
                    case 'Custom Comments Package':
                        opt.dataset.type=14;
                        break;
                    case 'Custom Comments':
                        opt.dataset.type=2;
                        break;
                    case 'Package':
                        opt.dataset.type=10;
                        break;
                    case 'Comment Likes':
                        opt.dataset.type=15;
                        break;
                }
                opt.value = serviceList.service;
                opt.innerHTML = serviceList.korname;
                target.appendChild(opt);
            }
            let ServiceOptions = target.options;
            selectService(ServiceOptions);
        }).catch(e=>console.log("카테고리가 제대로 읽히지 않습니다"));
}


function selectService(options){ // 서비스 선택시 모든 데이터 뷰로 전달
    allHidden();
    let selectedOption = options[options.selectedIndex];
    let servicenum = selectedOption.value;
    let type = selectedOption.dataset.type; //dataset 가져오기 검색해서 알아보기
    let link; let quantity; let username; let posts; let orderMin; let comment;

    document.querySelector("#charge-div").classList.remove('hidden'); //금액 계산 부분 공통.
    document.querySelector("#charge-div > input").value="";

    switch (type) {
        case "12" : //아래로 옮기면 딜레이때문에 사용자 편의에 문제
            link = document.querySelector("#order_link");
            quantity = document.querySelector("#order_quantity");

            link.classList.remove("hidden");
            quantity.classList.remove("hidden");

            document.querySelector("#order_link > input").value="";
            document.querySelector("#field-orderform-fields-quantity").value="";
            document.querySelector("#field-orderform-fields-quantity").readOnly = false;

            document.querySelector("#field-orderform-fields-type").value="12";
            break;
        case "100" :
            username = document.querySelector("#order_username");
            posts = document.querySelector("#order_posts");
            orderMin = document.querySelector("#order_min");

            username.classList.remove("hidden");
            posts.classList.remove("hidden");
            orderMin.classList.remove("hidden");

            document.querySelector("#order_username > input").value="";
            document.querySelector("#order_posts > input").value="";
            for (let elem of document.querySelectorAll("#order_count"))
                elem.value="";

            document.querySelector("#field-orderform-fields-type").value="100";
            break;
        case "14" :
            link = document.querySelector("#order_link");
            comment = document.querySelector("#order_comment");

            link.classList.remove("hidden");
            comment.classList.remove("hidden");

            document.querySelector("#order_link > input").value="";
            document.querySelector("#order_comment > textarea").value="";

            document.querySelector("#field-orderform-fields-type").value="14";
            break;
        case "2" :
            link = document.querySelector("#order_link");
            quantity = document.querySelector("#order_quantity");
            comment = document.querySelector("#order_comment");

            link.classList.remove("hidden");
            quantity.classList.remove("hidden"); //comments에 따라 값이 업데이트 되게 하는게 굳이 필요한가싶음
            comment.classList.remove("hidden");

            document.querySelector("#order_link > input").value="";
            document.querySelector("#field-orderform-fields-quantity").value="";
            document.querySelector("#field-orderform-fields-quantity").readOnly = true;
            document.querySelector("#order_comment > textarea").value="";

            document.querySelector("#field-orderform-fields-type").value="2";
            break;
        case "10" :
            link = document.querySelector("#order_link");

            link.classList.remove("hidden");

            document.querySelector("#order_link > input").value="";

            document.querySelector("#field-orderform-fields-type").value="10";
            break;
        /* case "15" :
            let link = document.querySelector("#order_link");
            let quantity = document.querySelector("#order_quantity");
            let commentUsername = document.querySelector("#order_comment_username");
            link.classList.remove("hidden");
            quantity.classList.remove("hidden");
            commentUsername.classList.remove("hidden");
            document.querySelector("#field-orderform-fields-type").value="15";
            break;*/
    }


    //servicenum을 통해 모든 데이터를 받아와서 각 태그에 값을 대입해놓음 //price min max꽂기, servicenum도 꽂기// description도 꽂기
    fetch(`http://mktingshop.com/api/v2/getService?servicenum=${servicenum}`)
        .then(res => res.json())
        .then(service =>{
            let min = priceToString(`${service.min}`);
            let max = priceToString(`${service.max}`);
            let price;

            let serviceidBox = document.querySelector("#orderform-service-id");
            serviceidBox.innerHTML = `#${service.service}`;

            let quantityBox = document.querySelector("#order_quantity > small");
            quantityBox.innerHTML = `최소: ${min} - 최대: ${max}`;
            switch (type) {
                case "12" :
                    document.querySelector("#field-orderform-fields-quantity").dataset.price = service.price;
                    break;
                case "100" :
                    document.querySelector("#field-orderform-fields-posts").dataset.price = service.price;
                    let autoQuantityBox = document.querySelector("#order_min small");
                    autoQuantityBox.innerHTML = `최소: ${min} - 최대: ${max}`;
                    break;
                case "14" :
                    document.querySelector("#field-orderform-fields-comment").removeEventListener("keyup",commentPrice);
                    document.querySelector("#charge").value= priceToString(`${service.price}`);
                    break;
                case "2" :
                    document.querySelector("#field-orderform-fields-comment").dataset.price=service.price;
                    document.querySelector("#field-orderform-fields-comment").addEventListener("keyup",commentPrice);
                    break;
                case "10" :
                    document.querySelector("#charge").value= priceToString(`${service.price}`);
                    break;
            }

            let descriptionBox = document.querySelector("#service_description > div");
            descriptionBox.innerHTML = service.description;

            let star = service.star;
            if (star==null) star=5;
            document.querySelector("#comment-star-title").innerHTML=`${service.service} 서비스 댓글 ( <i class="fas fa-star"></i> ${star}점 )`;

            if (service.timetocomplete){
                document.querySelector("#order_average_time").classList.remove("hidden");
                document.querySelector("#order_average_time > input").value = service.timetocomplete;
            }

        }).catch(e=>console.log("serviceNum이 이상함",e));

    let targetNode = document.querySelector("#comment-list-show");
    targetNode.innerHTML="";

    fetch(`http://mktingshop.com/api/v2/comments/${servicenum}`)
        .then(res => res.json())
        .then(comments =>{
            document.querySelector("#comment-star-title").innerHTML += ` ${comments.length}개`;
            for (let i = 0; i < comments.length; i++){
                if (i != 0)
                    targetNode.innerHTML += "<hr style='margin-top: 19px; margin-bottom: 19px; border-top-color: rgb(56, 56, 56); color: rgb(92, 92, 92); font-family: &quot;Source Sans Pro&quot;, sans-serif; font-size: 13px;'>";
                let comment=comments[i];

                let date;
                if (!comment.updatedate)
                    date = comment.createdate;
                else
                    date = `${comment.updatedate} <span class="badge rounded-pill bg-secondary">수정</span>`

                let star="";
                for (let j = 0; j < comment.star; j++)
                    star += "<i class='fas fa-star'></i>"
                for (let k = 0; k < (5-comment.star); k++)
                    star += "<i class='far fa-star'></i>"

                targetNode.innerHTML += `<font color='#000'><p><b>${comment.user.nickname}  <small>${date}</small></b></p><p>${star}</p><p>${comment.content}</p></font>`;
            }
        }).catch(e=>console.log("serviceNum이 이상함",e));
}

function allHidden(){ // 우선적으로 모두 히든으로 변경
    let AllDiv = document.querySelectorAll('#fields > div');
    for (let monoDiv of AllDiv){
        monoDiv.classList.add('hidden'); // monoDiv.className += ' hidden'과 다르게 한번만 추가
    }
    document.querySelector("#charge-div").classList.add('hidden');
}

function defaultPrice(option){
    let curPrice = Math.floor(option.dataset.price * option.value / 1000);
    document.querySelector("#charge").value= priceToString(`${curPrice}`);
}

function commentPrice(){
    let commentQuantity = document.querySelector("#field-orderform-fields-comment").value.split('\n').length;
    document.querySelector("#field-orderform-fields-quantity").value = commentQuantity;

    let curPrice = Math.floor(document.querySelector("#field-orderform-fields-comment").dataset.price * commentQuantity / 1000);
    document.querySelector("#charge").value= priceToString(`${curPrice}`);
}

function subsPrice(){
    let price = document.querySelector("#field-orderform-fields-posts").dataset.price;
    let minmax = document.querySelectorAll("#order_count");

    let posts = document.querySelector("#field-orderform-fields-posts").value;
    let min = minmax[0].value;
    let max = minmax[1].value;

    if (!posts) posts=0;
    if (!min) min=0;
    if (!max) max=0;

    posts *= 1; min *= 1; max *= 1;

    let curPrice = Math.floor(price * posts * (min+max) / 2000);
    document.querySelector("#charge").value= priceToString(`${curPrice}`);
}



function qna_unfollow(){
    let curBox = document.querySelector("#QnA-quality");
    curBox.classList.remove("show");

    let unfollow = document.querySelector("#QnA-unfollow");
    unfollow.classList.add("show");
}

function qna_danger(){
    let curBox = document.querySelector("#QnA-unfollow");
    curBox.classList.remove("show");

    let danger = document.querySelector("#QnA-danger");
    danger.classList.add("show");
}

function sortOfTime(){
    let servicenum = document.querySelector("#orderform-service-id").innerHTML.substr(1);
    let target = document.querySelector("#orderform-service");

    fetch(`http://mktingshop.com/api/v2/sortTime?servicenum=${servicenum}`)
          .then(res => res.json())
          .then(serviceLists =>{
                target.options.length = 0;
                for(let serviceList of serviceLists){ //serviceList는 서비스의 모든정보 내포
                    let opt = document.createElement("option");
                    switch (serviceList.type) {
                        case 'Default':
                            opt.dataset.type=12;
                            break;
                        case 'Subscriptions':
                            opt.dataset.type=100;
                            break;
                        case 'Custom Comments Package':
                            opt.dataset.type=14;
                            break;
                        case 'Custom Comments':
                            opt.dataset.type=2;
                            break;
                        case 'Package':
                            opt.dataset.type=10;
                            break;
                        case 'Comment Likes':
                            opt.dataset.type=15;
                            break;
                    }
                    opt.value = serviceList.service;
                    opt.innerHTML = serviceList.korname;
                    target.appendChild(opt);
                }
                let ServiceOptions = target.options;
                selectService(ServiceOptions);
          }).catch(e=>console.log("카테고리가 제대로 읽히지 않습니다"));
}
window.addEventListener("load",()=>{
    let subsid = location.href.split('/')[5];

    fetch(`http://mktingshop.com/api/v2/getSubscription?subsid=${subsid}`)
        .then(res => res.json())
        .then(subscription =>{
            let username = subscription.username;
            let posts = subscription.posts;
            let min = subscription.min;
            let max = subscription.max;
            let price = subscription.serviceList.price;

            posts *= 1; min *= 1; max *= 1; price *= 1;

            document.querySelector("#field-orderform-fields-username").value = username;
            document.querySelector("#field-orderform-fields-posts").value = posts;
            let minmax = document.querySelectorAll("#order_count");
            minmax[0].value = min;
            minmax[1].value = max;
            //subsPrice();
            let curPrice = Math.floor(price * posts * (min + max) / 2000);
            document.querySelector("#charge").value= priceToString(`${curPrice}원`);
    });
});

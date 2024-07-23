function navigateToUserPage(el) {
    window.location = el.getAttribute('data');
}

function timedRefresh(timeoutPeriod) {
    setTimeout("location.reload(true);", timeoutPeriod);
}

function httpPost() {
    let realm = document.getElementById("realmName").value;
    let user = document.getElementById('userName').value;
    let csrf = document.getElementById("_csrf").value;
    let enabled = document.getElementById("enbl").checked;
    let body = "enabled="+enabled+"&_csrf="+csrf;

    req = new XMLHttpRequest();
    req.open("POST", "/user/"+realm+"/"+user, false/*async*/);
    req.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    req.send(body);
    location.reload();
}

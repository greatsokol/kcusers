function navigateToUserPage(el) {
    window.location = el.getAttribute('data');
}

function timedRefresh(timeoutPeriod) {
    setTimeout("location.reload(true);", timeoutPeriod);
}
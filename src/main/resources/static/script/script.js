function navigateToUserPage(el) {
    window.location = el.getAttribute('data');
}

function httpPost() {
    let realm = document.getElementById("realmName").value;
    let user = document.getElementById('userName').value;
    let csrf = document.getElementById("_csrf").value;
    let enabled = document.getElementById("enbl").checked;
    let body = "enabled=" + enabled + "&_csrf=" + csrf;

    req = new XMLHttpRequest();
    req.open("POST", "/user/" + realm + "/" + user, false/*async*/);
    req.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    req.send(body);
    location.reload();
}

function back() {
    history.back();
}

document.getElementById("back-button")?.addEventListener("click", back);
document.getElementById("save-button")?.addEventListener("click", httpPost);

const userRows = document.getElementsByClassName("user-row");
for (let i = 0; i < userRows.length; i++) {
    const userRow = userRows[i];
    userRow.addEventListener("click", () => navigateToUserPage(userRow));
}

document.getElementById("body").onload = () => {
    setTimeout(() => location.reload(), 60000);
};

// const navigationType = performance.getEntriesByType("navigation")[0].type;
// console.log(navigationType)
// if(navigationType === "back_forward") { // always "reload"
//     window.location.reload();
// }

window.addEventListener ('pageshow', function (event) {
    if (event.persisted) {
        window.location.reload();
    }
});

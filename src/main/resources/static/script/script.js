// function promptUserStatusChange(el) {
//     let result = confirm("Are you sure?");
//     if (result) {
//         changeRow(el);
//         alert("proceed");
//     } else {
//         el.checked = !el.checked;
//     }
// }
//
// function changeRow(el) {
//     let elementId = el.getAttribute("id");
//     let label = document.querySelectorAll("label[id='" + elementId + "']")[0];
//     let tr = document.querySelectorAll("tr[id='" + elementId + "']")[0];
//     if (el.checked) {
//         label.textContent = "Enabled";
//         tr.className = "table-success";
//     } else {
//         label.textContent = "Disabled";
//         tr.className = "table-danger";
//     }
//
//
//
// }

function navigateToUserPage(el) {
    window.location = el.getAttribute('data');
}

function timedRefresh(timeoutPeriod) {
    setTimeout("location.reload(true);", timeoutPeriod);
}
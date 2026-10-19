const params = new URLSearchParams(window.location.search);
const message = params.get("message");
const messageBox = document.getElementById("message");

if (message && messageBox) {
    messageBox.textContent = message;
}

// JavaScript Document
// JavaScript Document
window.onload = function showtable() {
    try {
        var tablename = document.getElementById("table_o");
        var li = tablename.getElementsByTagName("tr");
        for (var i = 1; i < li.length; i++) {
            if (i % 2 == 0)
                li[i].style.backgroundColor = "#f5f5f5";
            else
                li[i].style.backgroundColor = "#ffffff";
            li[i].onmouseover = function () {
                this.style.backgroundColor = "#fff9b7";
            }
            li[i].onmouseout = function () {
                if (this.rowIndex % 2 == 0)
                    this.style.backgroundColor = "#f5f5f5";
                else
                    this.style.backgroundColor = "#ffffff";
            }
        }
    } catch (e) {

    }
}






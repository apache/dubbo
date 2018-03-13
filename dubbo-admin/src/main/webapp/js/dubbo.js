//==== init ====
function init() {
    initScroll();
    preloadImage();
    scanAlphaPNG();
    addChangeRowEvent();
}
// ==== utils ====
var isIE = (window.ActiveXObject && navigator.userAgent.toLowerCase().indexOf(" msie ") != -1);
var isIE6 = (isIE && (navigator.userAgent.toLowerCase().indexOf(" msie 5.") > -1
|| navigator.userAgent.toLowerCase().indexOf(" msie 6.") > -1));
String.prototype.trim = function () {
    return this.replace(/(^\\s*)|(\\s*$)/g, "");
}
function checkNumber() {
    if (event.keyCode == 8 || event.keyCode == 46
        || (event.keyCode >= 37 && event.keyCode <= 40)
        || (event.keyCode >= 48 && event.keyCode <= 57)
        || (event.keyCode >= 96 && event.keyCode <= 105))
        return true;
    return false;
}
function randPassword() {
    var text = ['ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz', '1234567890', '~_-+='];
    var rand = function (min, max) {
        return Math.floor(Math.max(min, Math.random() * (max + 1)));
    }
    var len = 16 + rand(0, 16);
    var pwd = '';
    for (i = 0; i < len; ++i) {
        var part = text[rand(0, 3)];
        pwd += part.charAt(rand(0, part.length));
    }
    return pwd;
}
function byId(x) {
    if (typeof x == "string")
        return document.getElementById(x);
    return x;
}
// ==== cookie ====
function setCookie(key, value) {
    var date = new Date();
    date.setTime(date.getTime() + (365 * 24 * 60 * 60 * 1000));
    document.cookie = key + "=" + escape(value) + "; path=/; expires=" + date.toGMTString();
}
function getCookie(objName) {
    var arrStr = document.cookie.split("; ");
    for (var i = 0; i < arrStr.length; i++) {
        var temp = arrStr[i].split("=");
        if (temp[0] == objName) {
            return unescape(temp[1]);
        }
    }
}
function addCookie(objName, objValue, objHours) {
    var str = objName + "=" + escape(objValue);
    if (objHours > 0) {
        var date = new Date();
        var ms = objHours * 3600 * 1000;
        date.setTime(date.getTime() + ms);
        str += ";path=/;expires=" + date.toGMTString();
    }
    document.cookie = str;
}
// ==== search table ====
var lastSequence = 0;
function searchTable(id, column, keyword) {
    var table = byId(id);
    if (table) {
        lastSequence++;
        var sequence = lastSequence;
        for (var i = 1; i < table.rows.length && sequence == lastSequence; i++) {
            var row = table.rows[i];
            var cell = row.cells[column];
            if (keyword == null || keyword.length == 0
                || cell.innerHTML.toLowerCase().indexOf(keyword.toLowerCase()) >= 0) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        }
    }
}
function addChangeRowEvent() {
    var content = document.getElementById('table_o');
    if (content) {
        for (var i = 0; i < content.rows.length; i++) {
            var cell = content.rows[i].cells[0];
            if (cell.nodeName != "TH" && cell.nodeName != "th") {
                var moveFunc = function (ii) {
                    return function () {
                        content.rows[ii].style.background = "#F8F8F8";
                    }
                }(i);
                var outFunc = function (ii) {
                    return function () {
                        content.rows[ii].style.background = "#FFFFFF";
                    }
                }(i);
                if (isIE) {
                    content.rows[i].onmousemove = moveFunc;
                    content.rows[i].onmouseout = outFunc;
                } else {
                    content.rows[i].addEventListener("mousemove", moveFunc, false);
                    content.rows[i].addEventListener("mouseout", outFunc, false);
                }
            }
        }
    }
}
// ==== tab ====
function setTab(name, cursel, n) {
    for (i = 1; i <= n; i++) {
        var menu = byId(name + i);
        var con = byId("con_" + name + "_" + i);
        menu.className = i == cursel ? "active" : "";
        con.style.display = i == cursel ? "block" : "none";
    }
}
function setActiveTab(i) {
    if (i == 1) {
        document.getElementById("unique_tab" + i).className = "current_nosub";
    } else {
        document.getElementById("unique_tab" + i).className = "current";
    }
}

// ==== check box ====
function checkAll(tableId, checkboxName, checked) {
    var checkboxs = document.getElementsByName(checkboxName);
    var checktable = document.getElementById(tableId);
    for (var i = 0; i < checkboxs.length; i++) {
        if (checktable && checktable.rows.length > i + 1 && checktable.rows[i + 1].style.display == 'none') {
            checkboxs[i].checked = false;
        } else {
            checkboxs[i].checked = checked;
        }
    }
}
function hasCheckbox(name) {
    var checkboxs = document.getElementsByName(name);
    return checkboxs && checkboxs.length > 0;
}
function hasChecked(name) {
    var checkboxs = document.getElementsByName(name);
    if (checkboxs && checkboxs.length > 0) {
        for (var i = 0; i < checkboxs.length; i++) {
            if (checkboxs[i].checked) {
                return true;
            }
        }
    }
    return false;
}
function getChecked(name) {
    var result = "";
    var checkboxs = document.getElementsByName(name);
    for (var i = 0; i < checkboxs.length; i++) {
        if (checkboxs[i].checked) {
            if (result.length > 0) {
                result = result + "+";
            }
            result = result + checkboxs[i].value;
        }
    }
    return result;
}
// ==== show box ====
var confirmUrl = "";
function confirmRedirect(msg, data, url) {
    showConfirm(msg, data, url);
}
function showConfirm(msg, data, url) {
    if (!url) {
        url = data;
        data = "";
    }
    if (url == null || url == "") {
        return;
    }
    if (msg == null || msg == "") {
        msg = "Confirm?";
    }
    confirmUrl = url;
    byId("confirmText").innerHTML = msg;
    byId("confirmData").innerHTML = data;
    Box.show("confirmBox");
}
function confirmOK() {
    Box.hide("confirmBox");
    if (confirmUrl == null || confirmUrl == "") {
        return;
    }
    window.location.href = confirmUrl;
}
function confirmCancel() {
    Box.hide("confirmBox");
}
var alertId = "";
function showAlert(msg, data, id) {
    if (id) {
        alertId = id;
    } else {
        alertId = "";
    }
    if (msg == null || msg == "") {
        msg = "Please input!";
    }
    if (data == null) {
        data = "";
    }
    byId("alertText").innerHTML = msg;
    byId("alertData").innerHTML = data;
    Box.show("alertBox");
}
function alertOK() {
    Box.hide("alertBox");
    if (alertId == null || alertId == "") {
        return;
    }
    byId(alertId).focus();
}
// ==== scroll bar ====
var marqueeInterval = null;
function moveScroll() {
    var marqueeBox = document.getElementById("marqueeBox");
    var marqueeText = document.getElementById("marqueeText");
    if (marqueeBox.scrollLeft >= marqueeText.offsetWidth - 3000) {
        marqueeBox.scrollLeft = 0;
    } else {
        marqueeBox.scrollLeft = marqueeBox.scrollLeft + 1;
    }
}
function startScroll() {
    marqueeInterval = window.setInterval(moveScroll, 30)
}
function stopScroll() {
    window.clearInterval(marqueeInterval)
}
function initScroll() {
    var marqueeText = document.getElementById("marqueeText");
    if (marqueeText == null) {
        return;
    }
    var str = marqueeText.innerHTML;
    var i = str.indexOf(">");
    if (i > 0) {
        str = str.substring(i + 1);
    }
    var len = str.length + str.replace(/\x00-\x7f/g, '').length;
    marqueeText.style.width = (len * 6 + 30) + "px";
    startScroll();
}
// ==== show modal ====
$(function () {
    function showModal(src, height, width) {
        jQuery.modal('<iframe src="' + src + '" height="' + height
            + '" width="' + width
            + '" frameborder="0" allowTransparency=true>', {
            closeHTML: "<input type='button' style='display:none'/>",
            closeClass: "modalClose",
            opacity: 35,
            overlayCss: {
                backgroundColor: "#000"
            }
        });
    }

    $(".link").click(function () {
        showModal("tip_message_choose.html", "300", "600");
    });
    function megaHoverOver() {
        $(this).find(".sub").stop().fadeTo('fast', 1).show();
        // Calculate width of all ul's
        (function ($) {
            jQuery.fn.calcSubWidth = function () {
                rowWidth = 0;
                // Calculate row
                $(this).find("ul").each(function () {
                    rowWidth += $(this).width();
                });
            };
        })(jQuery);
        if ($(this).find(".row").length > 0) {
            // If row exists...
            var biggestRow = 0;
            // Calculate each row
            $(this).find(".row").each(function () {
                $(this).calcSubWidth();
                // Find biggest row
                if (rowWidth > biggestRow) {
                    biggestRow = rowWidth;
                }
            });
            // Set width
            $(this).find(".sub").css({
                'width': biggestRow
            });
            $(this).find(".row:last").css({
                'margin': '0'
            });
        } else {
            // If row does not exist...
            $(this).calcSubWidth();
            // Set Width
            $(this).find(".sub").css({
                'width': rowWidth
            });
        }
    }

    function megaHoverOut() {
        $(this).find(".sub").stop().fadeTo('fast', 0, function () {
            $(this).hide();
        });
    }

    var config = {
        sensitivity: 1,
        // number = sensitivity threshold (must be 1 or higher)
        interval: 100,
        // number = milliseconds for onMouseOver polling interval
        over: megaHoverOver,
        // function = onMouseOver callback (REQUIRED)
        timeout: 200,
        // number = milliseconds delay before onMouseOut
        out: megaHoverOut
        // function = onMouseOut callback (REQUIRED)
    };
});
// ==== favorites ====
function fnFavorites(id, title) {
    if (confirm(title + 'confirm favorites')) {
        document.getElementById('search_name').value = title;
        document.getElementById('search_value').value = document.URL;
        document.getElementById('favoritesForm').submit();
    }
    //return false;
}
function fnSelectAll() {
    var flag = false;
    if (document.df.favoritesAll.checked == true) {
        flag = true;
    }

    var ids = document.getElementsByName('favoritesId');
    for (var i = 0; i < ids.length; i++) {
        ids[i].checked = flag;
    }
}
function fnDeleteAll() {
    var idlist = "";
    var ids = document.getElementsByName('favoritesId');
    for (var i = 0; i < ids.length; i++) {
        if (ids[i].checked == true) {
            idlist = idlist + ids[i].value + "+";
        }
    }
    if (idlist.length > 0) {
        idlist = idlist.substring(0, idlist.length - 1);
        window.location.href = "/governance/favorites/" + idlist + "/delete";
    }
}
function fnFolder() {
    document.getElementById('fav_box').style.display = "none";
}
// ==== search service ====
function loadServiceList(keyword) {
    var url = "/governance/" + searchType + "/search?keyword=" + keyword;
    //if (serviceListLoaded) {
    //	return;
    //}
    var table = byId('serviceCompletion');
    for (var i = 0; i < table.rows.length; i++) {
        table.deleteRow(0);
    }
    serviceListLoaded = true;
    Ajax.get(url, function (data) {
        if (data != null) {
            var table = byId('serviceCompletion');
            for (var i = 0; i < data.length; i++) {
                var service = data[i];
                var row = table.insertRow(0);
                var cell = row.insertCell(0);
                cell.innerHTML = "<a href=\"/governance/" + searchType + "/" + service + "/providers\">" + service + "</a>";
            }
        }
    });
}
function hideService() {
    byId('serviceDiv').style.display = 'none';
}
var searchTimes = 0;
function searchService(keyword) {

    if (keyword == null || keyword == "") {
        byId('serviceDiv').style.display = 'none';
        return;
    }
    loadServiceList(keyword);
    var content = byId('serviceCompletion');
    if (content) {
        searchTimes = searchTimes + 1;
        var currentTimes = searchTimes;
        var showIndex = 0;
        for (var i = 0; i < content.rows.length && currentTimes == searchTimes; i++) {
            if (showIndex > 10) {
                break;
            }
            var cell = content.rows[i].cells[0];
            if (cell.innerHTML.toLowerCase().indexOf(keyword.toLowerCase()) >= 0) {
                content.rows[i].style.display = '';
                showIndex++;
            } else {
                content.rows[i].style.display = 'none';
            }
        }
        byId('serviceDiv').style.display = '';
    }
}
function setSearchCookie(key, value) {
    var separatorsB = "\\.\\.\\.\\.\\.\\.";
    var cookie_new_sub = key + "...." + value
    var cookie_new = cookie_new_sub;
    var cookie_name = "HISTORY";
    var cookie_old = getCookie("HISTORY");
    var cookie_old_list = cookiev.split(separatorsB);
    var count = 1;
    for (var i = 0; i < cookie_old_list.length; i++) {
        if (count <= 10) {
            if (cookie_new_sub != cookie_old_list[i]) {
                cookie_new = cookie_new + separatorsB + cookie_old_list[i];
            }
        }
        count++;
    }
    addCookie(cookie_name, cookie_new, 168);
}

// ==== image load ====

var alphapngs = ["logo.png", "pop_close.png", "tip_choose.png", "tip_confirm.png", "tip_del.png", "tip_succeed.png"];
function isAlphaPng(imgName) {
    for (var i = 0; i < alphapngs.length; i++) {
        var ap = "/images/" + alphapngs[i];
        if (imgName.length >= ap.length && imgName.substring(imgName.length - ap.length) == ap) {
            return true;
        }
    }
    return false;
}
function replaceAlphaPNG(img) {
    var strNewHTML = "<div style=\"width:" + img.width + "px; height:" + img.height
        + "px; filter: progid:DXImageTransform.Microsoft.AlphaImageLoader(src=\'"
        + img.src + "\', sizingMethod='nonscale');\" />";
    img.outerHTML = strNewHTML;
}
function scanAlphaPNG() {
    if (isIE6 && document.images) {
        var imgs = [];
        for (var i = 0; i < document.images.length; i++) {
            imgs.push(document.images[i]);
        }
        for (var i = 0; i < imgs.length; i++) {
            var img = imgs[i];
            imgs[i] = null;
            if (isAlphaPng(img.src.toLowerCase())) {
                replaceAlphaPNG(img);
            }
        }
    }
}
var preloads = ["pop_close.png", "tip_choose.png", "tip_confirm.png", "tip_del.png", "tip_succeed.png"];
function preloadImage() {
    for (var i = 0; i < preloads.length; i++) {
        new Image().src = "/images/" + preloads[i];
    }
}

// ==== menu ====

function getAbsoluteLeft(ele) {
    var i = 0;
    while (ele) {
        i += ele.offsetLeft;
        ele = ele.offsetParent;
    }
    return i;
}
function getAbsoluteTop(ele) {
    var i = 0;
    while (ele) {
        i += ele.offsetTop;
        ele = ele.offsetParent;
    }
    return i;
}
function showMenu(c, m, g) {
    var l = getAbsoluteLeft(c);
    var h = c.clientHeight;
    var t = getAbsoluteTop(c);
    m.style.left = (l + 2) + 'px';
    m.style.top = (t + h - 1) + 'px';
    c.style.background = 'url("/images/menu_bg_over.png")';
    m.style.display = '';
    if (isIE6 && g) {
        replaceAlphaPNG(g);
    }
}
function hideMenu(c, m) {
    c.style.background = 'url("/images/menu_bg.png")';
    m.style.display = 'none';
}
function switchToRegistry(r) {
    var url = window.location.href;
    var i = url.indexOf("://");
    i = url.indexOf("/", i + 3);
    url = url.substring(i);
    url = "http://" + r + url;
    window.location.href = url;
}
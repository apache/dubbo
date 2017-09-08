var Ajax = {
    get: function (url, fn) {
        XR.open("GET", url, true);
        XR.onreadystatechange = function () {
            if (XR.readyState == 4) {
                try {
                    fn(eval("(" + XR.responseText + ")"));
                } catch (e) {
                    fn(null);
                }
            }
        };
        XR.send(null)
    },
    post: function (url, fn) {
        XR.open("POST", url, true);
        XR.onreadystatechange = function () {
            if (XR.readyState == 4) {
                try {
                    fn(eval("(" + XR.responseText + ")"));
                } catch (e) {
                    fn(null);
                }
            }
        };
        XR.send(null)
    },
    put: function (url, fn) {
        XR.open("PUT", url, true);
        XR.onreadystatechange = function () {
            if (XR.readyState == 4) {
                try {
                    fn(eval("(" + XR.responseText + ")"));
                } catch (e) {
                    fn(null);
                }
            }
        };
        XR.send(null)
    },
    delete: function (url, fn) {
        XR.open("DELETE", url, true);
        XR.onreadystatechange = function () {
            if (XR.readyState == 4) {
                try {
                    fn(eval("(" + XR.responseText + ")"));
                } catch (e) {
                    fn(null);
                }
            }
        };
        XR.send(null)
    }
};
var XR = false;
try {
    XR = new XMLHttpRequest()
} catch (trymicrosoft) {
    try {
        XR = new ActiveXObject("Msxml2.XMLHTTP")
    } catch (othermicrosoft) {
        try {
            XR = new ActiveXObject("Microsoft.XMLHTTP")
        } catch (failed) {
            XR = false
        }
    }
}
if (!XR) {
    alert("Error Initializing XMLHttpRequest!")
}
;
/* eg: Box.show(div) */
var Box = {
	show: function(id) {
		var isIE = (document.all) ? true : false;
		var isIE6 = isIE && ( [ /MSIE (\d)\.0/i.exec(navigator.userAgent) ][0][1] == 6);
		var box = document.getElementById(id);
		if (! box) {
			return;
		}
		box.style.zIndex = "9999";
		box.style.display = "block"
		box.style.position = !isIE6 ? "fixed" : "absolute";
		box.style.top = box.style.left = "50%";
		box.style.marginTop = -box.offsetHeight / 2 + "px";
		box.style.marginLeft = -box.offsetWidth / 2 + "px";
		var layer = document.getElementById("_box_layer");
		if (! layer) {
			layer = document.createElement("div");
			layer.id = "_box_layer";
			layer.style.width = layer.style.height = "100%";
			layer.style.position = !isIE6 ? "fixed" : "absolute";
			layer.style.top = layer.style.left = 0;
			layer.style.backgroundColor = "#000";
			layer.style.zIndex = "9998";
			layer.style.opacity = "0.6";
			document.body.appendChild(layer);
		} else {
			layer.style.display = "";
		}
		var selects = document.getElementsByTagName("select");
		if (selects) {
			for ( var i = 0; i < selects.length; i++) {
				selects[i].style.visibility = "hidden";
			}
		}
		function layer_iestyle() {
			layer.style.width = Math.max(document.documentElement.scrollWidth,
					document.documentElement.clientWidth)
					+ "px";
			layer.style.height = Math.max(document.documentElement.scrollHeight,
					document.documentElement.clientHeight)
					+ "px";
		}
		function box_iestyle() {
			box.style.marginTop = document.documentElement.scrollTop
					- box.offsetHeight / 2 + "px";
			box.style.marginLeft = document.documentElement.scrollLeft
					- box.offsetWidth / 2 + "px";
		}
		if (isIE) {
			layer.style.filter = "alpha(opacity=60)";
		}
		if (isIE6) {
			layer_iestyle()
			box_iestyle();
			window.attachEvent("onscroll", function() {
				box_iestyle();
			})
			window.attachEvent("onresize", layer_iestyle)
		}
	},
	hide: function (id) {
		var box = document.getElementById(id);
		if (box) {
			box.style.display = "none";
		}
		var layer = document.getElementById("_box_layer");
		if (layer) {
			layer.style.display = "none";
		}
		var selects = document.getElementsByTagName("select");
		if (selects) {
			for ( var i = 0; i < selects.length; i++) {
				selects[i].style.visibility = "visible";
			}
		}
	}
}
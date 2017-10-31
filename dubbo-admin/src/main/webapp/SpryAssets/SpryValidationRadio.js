// SpryValidationRadio.js - version 0.1 - Spry Pre-Release 1.6.1
//
// Copyright (c) 2007. Adobe Systems Incorporated.
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
//   * Redistributions of source code must retain the above copyright notice,
//     this list of conditions and the following disclaimer.
//   * Redistributions in binary form must reproduce the above copyright notice,
//     this list of conditions and the following disclaimer in the documentation
//     and/or other materials provided with the distribution.
//   * Neither the name of Adobe Systems Incorporated nor the names of its
//     contributors may be used to endorse or promote products derived from this
//     software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

var Spry;
if (!Spry) Spry = {};
if (!Spry.Widget) Spry.Widget = {};

Spry.Widget.ValidationRadio = function (element, opts) {
    this.init(element);

    Spry.Widget.Utils.setOptions(this, opts);

    // set validateOn flags
    var validateOn = ['submit'].concat(this.validateOn || []);
    validateOn = validateOn.join(",");
    this.validateOn = 0 | (validateOn.indexOf('submit') != -1 ? Spry.Widget.ValidationRadio.ONSUBMIT : 0);
    this.validateOn = this.validateOn | (validateOn.indexOf('blur') != -1 ? Spry.Widget.ValidationRadio.ONBLUR : 0);
    this.validateOn = this.validateOn | (validateOn.indexOf('change') != -1 ? Spry.Widget.ValidationRadio.ONCHANGE : 0);

    if (this.additionalError)
        this.additionalError = this.getElement(this.additionalError);

    // Unfortunately in some browsers like Safari, the Stylesheets our
    // page depends on may not have been loaded at the time we are called.
    // This means we have to defer attaching our behaviors until after the
    // onload event fires, since some of our behaviors rely on dimensions
    // specified in the CSS.

    if (Spry.Widget.ValidationRadio.onloadDidFire)
        this.attachBehaviors();
    else
        Spry.Widget.ValidationRadio.loadQueue.push(this);
};

Spry.Widget.ValidationRadio.ONCHANGE = 1;
Spry.Widget.ValidationRadio.ONBLUR = 2;
Spry.Widget.ValidationRadio.ONSUBMIT = 4;

Spry.Widget.ValidationRadio.prototype.init = function (element) {
    this.element = this.getElement(element);
    this.additionalError = false;
    this.radioElements = null;
    this.form = null;
    this.event_handlers = [];

    // this.element can be either the container (<span>)
    // or the <input type="radio"> element, when no error messages are used.
    this.requiredClass = "radioRequiredState";
    this.focusClass = "radioFocusState";
    this.invalidClass = "radioInvalidState";
    this.validClass = "radioValidState";

    this.emptyValue = "";
    this.invalidValue = null;
    this.isRequired = true;
    this.validateOn = ["submit"]; // change, submit (blur ?)
};

Spry.Widget.ValidationRadio.onloadDidFire = false;
Spry.Widget.ValidationRadio.loadQueue = [];

Spry.Widget.ValidationRadio.prototype.getElement = function (ele) {
    if (ele && typeof ele == "string")
        return document.getElementById(ele);
    return ele;
};

Spry.Widget.ValidationRadio.processLoadQueue = function (handler) {
    Spry.Widget.ValidationRadio.onloadDidFire = true;
    var q = Spry.Widget.ValidationRadio.loadQueue;
    var qlen = q.length;
    for (var i = 0; i < qlen; i++)
        q[i].attachBehaviors();
};

Spry.Widget.ValidationRadio.addLoadListener = function (handler) {
    if (typeof window.addEventListener != 'undefined')
        window.addEventListener('load', handler, false);
    else if (typeof document.addEventListener != 'undefined')
        document.addEventListener('load', handler, false);
    else if (typeof window.attachEvent != 'undefined')
        window.attachEvent('onload', handler);
};

Spry.Widget.ValidationRadio.addLoadListener(Spry.Widget.ValidationRadio.processLoadQueue);
Spry.Widget.ValidationRadio.addLoadListener(function () {
    Spry.Widget.Utils.addEventListener(window, "unload", Spry.Widget.Form.destroyAll, false);
});

Spry.Widget.ValidationRadio.prototype.attachBehaviors = function () {
    if (!this.element)
        return;
    // find the INPUT type="Radio" element(s) inside current container
    if (this.element.nodeName == "INPUT") {
        this.radioElements = [this.element];
    } else {
        this.radioElements = this.getRadios();
    }
    if (this.radioElements) {
        var self = this;
        this.event_handlers = [];

        var qlen = this.radioElements.length;
        for (var i = 0; i < qlen; i++) {
            // focus
            this.event_handlers.push([this.radioElements[i], "focus", function (e) {
                return self.onFocus(e);
            }]);
            // blur
            this.event_handlers.push([this.radioElements[i], "blur", function (e) {
                return self.onBlur(e);
            }]);
            // add click instead of onChange
            if (this.validateOn & Spry.Widget.ValidationRadio.ONCHANGE) {
                this.event_handlers.push([this.radioElements[i], "click", function (e) {
                    return self.onClick(e);
                }]);
            }
        }

        for (var i = 0; i < this.event_handlers.length; i++) {
            Spry.Widget.Utils.addEventListener(this.event_handlers[i][0], this.event_handlers[i][1], this.event_handlers[i][2], false);
        }

        // submit
        this.form = Spry.Widget.Utils.getFirstParentWithNodeName(this.element, "FORM");
        if (this.form) {
            // if no "onSubmit" handler has been attached to the current form, attach one
            if (!this.form.attachedSubmitHandler && !this.form.onsubmit) {
                this.form.onsubmit = function (e) {
                    e = e || event;
                    return Spry.Widget.Form.onSubmit(e, e.srcElement || e.currentTarget)
                };
                this.form.attachedSubmitHandler = true;
            }
            if (!this.form.attachedResetHandler) {
                Spry.Widget.Utils.addEventListener(this.form, "reset", function (e) {
                    e = e || event;
                    return Spry.Widget.Form.onReset(e, e.srcElement || e.currentTarget)
                }, false);
                this.form.attachedResetHandler = true;
            }
            // add the currrent widget to the "onSubmit" check queue;
            Spry.Widget.Form.onSubmitWidgetQueue.push(this);
        }
    }
};

Spry.Widget.ValidationRadio.prototype.getRadios = function () {
    var arrRadios;
    var elements = this.element.getElementsByTagName("INPUT");
    if (elements.length) {
        arrRadios = [];
        var qlen = elements.length;
        for (var i = 0; i < qlen; i++) {
            if (elements[i].getAttribute('type').toLowerCase() == "radio")
                arrRadios.push(elements[i]);
        }
        return arrRadios;
    }
    return null;
};

Spry.Widget.ValidationRadio.prototype.addClassName = function (ele, className) {
    if (!ele || !className || (ele.className && ele.className.search(new RegExp("\\b" + className + "\\b")) != -1))
        return;
    ele.className += (ele.className ? " " : "") + className;
};

Spry.Widget.ValidationRadio.prototype.removeClassName = function (ele, className) {
    if (!ele || !className || (ele.className && ele.className.search(new RegExp("\\b" + className + "\\b")) == -1))
        return;
    ele.className = ele.className.replace(new RegExp("\\s*\\b" + className + "\\b", "g"), "");
};

Spry.Widget.ValidationRadio.prototype.onFocus = function (e) {
    var eventRadio = (e.srcElement != null) ? e.srcElement : e.target;
    if (eventRadio.disabled) return;

    this.addClassName(this.element, this.focusClass);
    this.addClassName(this.additionalError, this.focusClass);
};

Spry.Widget.ValidationRadio.prototype.onBlur = function (e) {
    var eventRadio = (e.srcElement != null) ? e.srcElement : e.target;
    if (eventRadio.disabled) return;

    var doValidation = false;
    if (this.validateOn & Spry.Widget.ValidationRadio.ONBLUR)
        doValidation = true;
    if (doValidation)
        this.validate();
    this.removeClassName(this.element, this.focusClass);
    this.removeClassName(this.additionalError, this.focusClass);
};

Spry.Widget.ValidationRadio.prototype.onClick = function (e) {
    var eventRadio = (e.srcElement != null) ? e.srcElement : e.target;
    if (eventRadio.disabled) return;
    this.validate();
};

Spry.Widget.ValidationRadio.prototype.reset = function () {
    this.removeClassName(this.element, this.validClass);
    this.removeClassName(this.element, this.requiredClass);
    this.removeClassName(this.element, this.invalidClass);
    this.removeClassName(this.additionalError, this.validClass);
    this.removeClassName(this.additionalError, this.requiredClass);
    this.removeClassName(this.additionalError, this.invalidClass);
};

Spry.Widget.ValidationRadio.prototype.validate = function () {
    this.reset();
    var nochecked = 0;
    var invalid = 0;
    var required = 0;
    if (this.radioElements) {
        var qlen = this.radioElements.length;
        for (var i = 0; i < qlen; i++) {
            if (!this.radioElements[i].disabled && this.radioElements[i].checked) {
                if (this.radioElements[i].value == this.emptyValue) {
                    required++;
                } else if (this.invalidValue && this.radioElements[i].value == this.invalidValue) {
                    invalid++;
                } else {
                    nochecked++;
                }
            }
        }
    }
    if (this.invalidValue && invalid != 0) {
        this.addClassName(this.element, this.invalidClass);
        this.addClassName(this.additionalError, this.invalidClass);
        return false;
    }

    // check isRequired
    if (this.isRequired && (nochecked == 0 || required != 0)) {
        this.addClassName(this.element, this.requiredClass);
        this.addClassName(this.additionalError, this.requiredClass);
        return false;
    }
    this.addClassName(this.element, this.validClass);
    this.addClassName(this.additionalError, this.validClass);
    return true;
};

Spry.Widget.ValidationRadio.prototype.isDisabled = function () {
    var ret = true;
    if (this.radioElements) {
        var qlen = this.radioElements.length;
        for (var i = 0; i < qlen; i++) {
            if (!this.radioElements[i].disabled) {
                ret = false;
                break;
            }
        }
    }
    return ret;
};

Spry.Widget.ValidationRadio.prototype.destroy = function () {
    if (this.event_handlers)
        for (var i = 0; i < this.event_handlers.length; i++) {
            Spry.Widget.Utils.removeEventListener(this.event_handlers[i][0], this.event_handlers[i][1], this.event_handlers[i][2], false);
        }
    try {
        delete this.element;
    } catch (err) {
    }
    if (this.radioElements)
        for (var i = 0; i < this.radioElements.length; i++) {
            try {
                delete this.radioElements[i];
            } catch (err) {
            }
        }
    try {
        delete this.radioElements;
    } catch (err) {
    }
    try {
        delete this.form;
    } catch (err) {
    }
    try {
        delete this.event_handlers;
    } catch (err) {
    }

    var q = Spry.Widget.Form.onSubmitWidgetQueue;
    var qlen = q.length;
    for (var i = 0; i < qlen; i++) {
        if (q[i] == this) {
            q.splice(i, 1);
            break;
        }
    }
};

//////////////////////////////////////////////////////////////////////
//
// Spry.Widget.Form - common for all widgets
//
//////////////////////////////////////////////////////////////////////

if (!Spry.Widget.Form) Spry.Widget.Form = {};
if (!Spry.Widget.Form.onSubmitWidgetQueue) Spry.Widget.Form.onSubmitWidgetQueue = [];

if (!Spry.Widget.Form.validate) {
    Spry.Widget.Form.validate = function (vform) {
        var isValid = true;
        var isElementValid = true;
        var q = Spry.Widget.Form.onSubmitWidgetQueue;
        var qlen = q.length;
        for (var i = 0; i < qlen; i++) {
            if (!q[i].isDisabled() && q[i].form == vform) {
                isElementValid = q[i].validate();
                isValid = isElementValid && isValid;
            }
        }
        return isValid;
    }
}
;

if (!Spry.Widget.Form.onSubmit) {
    Spry.Widget.Form.onSubmit = function (e, form) {
        if (Spry.Widget.Form.validate(form) == false) {
            return false;
        }
        return true;
    };
}
;

if (!Spry.Widget.Form.onReset) {
    Spry.Widget.Form.onReset = function (e, vform) {
        var q = Spry.Widget.Form.onSubmitWidgetQueue;
        var qlen = q.length;
        for (var i = 0; i < qlen; i++) {
            if (!q[i].isDisabled() && q[i].form == vform && typeof(q[i].reset) == 'function') {
                q[i].reset();
            }
        }
        return true;
    };
}
;

if (!Spry.Widget.Form.destroy) {
    Spry.Widget.Form.destroy = function (form) {
        var q = Spry.Widget.Form.onSubmitWidgetQueue;
        for (var i = 0; i < Spry.Widget.Form.onSubmitWidgetQueue.length; i++) {
            if (q[i].form == form && typeof(q[i].destroy) == 'function') {
                q[i].destroy();
                i--;
            }
        }
    }
}
;

if (!Spry.Widget.Form.destroyAll) {
    Spry.Widget.Form.destroyAll = function () {
        var q = Spry.Widget.Form.onSubmitWidgetQueue;
        for (var i = 0; i < Spry.Widget.Form.onSubmitWidgetQueue.length; i++) {
            if (typeof(q[i].destroy) == 'function') {
                q[i].destroy();
                i--;
            }
        }
    }
}
;

//////////////////////////////////////////////////////////////////////
//
// Spry.Widget.Utils
//
//////////////////////////////////////////////////////////////////////

if (!Spry.Widget.Utils) Spry.Widget.Utils = {};

Spry.Widget.Utils.setOptions = function (obj, optionsObj, ignoreUndefinedProps) {
    if (!optionsObj)
        return;
    for (var optionName in optionsObj) {
        if (ignoreUndefinedProps && optionsObj[optionName] == undefined)
            continue;
        obj[optionName] = optionsObj[optionName];
    }
};


Spry.Widget.Utils.getFirstParentWithNodeName = function (node, nodeName) {
    while (node.parentNode
    && node.parentNode.nodeName.toLowerCase() != nodeName.toLowerCase()
    && node.parentNode.nodeName != 'BODY') {
        node = node.parentNode;
    }

    if (node.parentNode && node.parentNode.nodeName.toLowerCase() == nodeName.toLowerCase()) {
        return node.parentNode;
    } else {
        return null;
    }
};

Spry.Widget.Utils.destroyWidgets = function (container) {
    if (typeof container == 'string') {
        container = document.getElementById(container);
    }

    var q = Spry.Widget.Form.onSubmitWidgetQueue;
    for (var i = 0; i < Spry.Widget.Form.onSubmitWidgetQueue.length; i++) {
        if (typeof(q[i].destroy) == 'function' && Spry.Widget.Utils.contains(container, q[i].element)) {
            q[i].destroy();
            i--;
        }
    }
};
Spry.Widget.Utils.contains = function (who, what) {
    if (typeof who.contains == 'object') {
        return what && who && (who == what || who.contains(what));
    } else {
        var el = what;
        while (el) {
            if (el == who) {
                return true;
            }
            el = el.parentNode;
        }
        return false;
    }
};
Spry.Widget.Utils.addEventListener = function (element, eventType, handler, capture) {
    try {
        if (element.addEventListener)
            element.addEventListener(eventType, handler, capture);
        else if (element.attachEvent)
            element.attachEvent("on" + eventType, handler, capture);
    } catch (e) {
    }
};
Spry.Widget.Utils.removeEventListener = function (element, eventType, handler, capture) {
    try {
        if (element.removeEventListener)
            element.removeEventListener(eventType, handler, capture);
        else if (element.detachEvent)
            element.detachEvent("on" + eventType, handler, capture);
    } catch (e) {
    }
};

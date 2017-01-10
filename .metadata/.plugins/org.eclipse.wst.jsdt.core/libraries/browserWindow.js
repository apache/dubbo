/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
function BarProp(){};
BarProp.prototype = new Array();

/**
 * Object Window()
 * @super Global
 * @constructor
 * @since Common Usage, no standard
*/
function Window(){};
Window.prototype = new EventTarget();
Window.prototype.self = new Window();
Window.prototype.window = new Window();
Window.prototype.frames = new Array();
/**
 * Property closed
 * @type Boolean
 * @memberOf Window
 */
Window.prototype.closed = new Boolean();
/**
 * Property defaultStatus
 * @type String
 * @memberOf Window
 */
Window.prototype.defaultStatus = "";
/**
 * Property document
 * @type Document
 * @memberOf Window
 */
Window.prototype.document= new HTMLDocument();
/**
 * Property history
 * @type History
 * @memberOf Window
 */
Window.prototype.history= new History();
/**
 * Property location
 * @type Location
 * @memberOf Window
 */
Window.prototype.location=new Location();
/**
 * Property name
 * @type String
 * @memberOf Window
 */
Window.prototype.name = "";
/**
 * Property navigator
 * @type Navigator
 * @memberOf Window
 */
Window.prototype.navigator = new Navigator();
/**
 * Property opener
 * @type Window
 * @memberOf Window
 */
Window.prototype.opener = new Window();
/**
 * Property outerWidth
 * @type Number
 * @memberOf Window
 */
Window.prototype.outerWidth = 0;
/**
 * Property outerHeight
 * @type Number
 * @memberOf Window
 */
Window.prototype.outerHeight = 0;
/**
 * Property pageXOffset
 * @type Number
 * @memberOf Window
 */
Window.prototype.pageXOffset = 0;
/**
 * Property pageYOffset
 * @type Number
 * @memberOf Window
 */
Window.prototype.pageYOffset = 0;
/**
 * Property parent
 * @type Window
 * @memberOf Window
 */
Window.prototype.parent = new Window();
/**
 * Property screen
 * @type Screen
 * @memberOf Window
 */
Window.prototype.screen = new Screen();
/**
 * Property status
 * @type String
 * @memberOf Window
 */
Window.prototype.status = "";
/**
 * Property top
 * @type Window
 * @memberOf Window
 */
Window.prototype.top = new Window();


/*
 * These properties may need to be moved into a browswer specific library.
 */

 /**
 * Property innerWidth
 * @type Number
 * @memberOf Window
 */
Window.prototype.innerWidth = 0;
/**
 * Property innerHeight
 * @type Number
 * @memberOf Window
 */
Window.prototype.innerHeight = 0;
/**
 * Property screenX
 * @type Number
 * @memberOf Window
 */
Window.prototype.screenX = 0;
/**
 * Property screenY
 * @type Number
 * @memberOf Window
 */
Window.prototype.screenY = 0;
/**
 * Property screenLeft
 * @type Number
 * @memberOf Window
 */
Window.prototype.screenLeft = 0;
/**
 * Property screenTop
 * @type Number
 * @memberOf Window
 */
Window.prototype.screenTop = 0;
//Window.prototype.event = new Event();
Window.prototype.length = 0;
Window.prototype.scrollbars= new BarProp();
Window.prototype.scrollX=0;
Window.prototype.scrollY=0;
Window.prototype.content= new Window();
Window.prototype.menubar= new BarProp();
Window.prototype.toolbar= new BarProp();
Window.prototype.locationbar= new BarProp();
Window.prototype.personalbar= new BarProp();
Window.prototype.statusbar= new BarProp();
Window.prototype.directories= new BarProp();
Window.prototype.scrollMaxX=0;
Window.prototype.scrollMaxY=0;
Window.prototype.fullScreen="";
Window.prototype.frameElement="";
/* End properites */

/**
 * function alert() 
 * @param {String} message
 * @memberOf Window
 */
Window.prototype.alert = function(message){};
/**
 * function blur() 
 * @memberOf Window
 */
Window.prototype.blur = function(){};
/**
 * function clearInterval(intervalID) 
 * @param intervalID
 * @memberOf Window
 */
Window.prototype.clearInterval = function(intervalID){};
/**
 * function clearTimeout(intervalID) 
 * @param intervalID
 * @memberOf Window
 */
Window.prototype.clearTimeout = function(intervalID){};
/**
 * function close() 
 * @memberOf Window
 */
Window.prototype.close = function(){};
/**
 * function confirm() 
 * @param {String} arg
 * @memberOf Window
 * @returns {Boolean}
 */
Window.prototype.confirm = function(arg){return false;};
/**
 * function focus() 
 * @memberOf Window
 */
Window.prototype.focus = function(){};
/**
 * function getComputedStyle(element, pseudoElt ) 
 * @param {Element} element
 * @param {String} pseudoElt 
 * @memberOf Window
 * @returns {Object}
 */
Window.prototype.getComputedStyle = function(element,pseudoElt ){return new Object();};
/**
 * function moveTo(x, y) 
 * @param {Number} x
 * @param {Number} y
 * @memberOf Window
 */
Window.prototype.moveTo = function(x,y){};
/**
 * function moveBy(deltaX, deltaY) 
 * @param {Number} deltaX
 * @param {Number} deltaY
 * @memberOf Window
 */
Window.prototype.moveBy = function(deltaX,deltaY){};
/**
 * function open(optionalArg1, optionalArg2, optionalArg3, optionalArg4) 
 * @param {String} url
 * @param {String} windowName
 * @param {String} windowFeatures
 * @param {Boolean} optionalArg4
 * @memberOf Window
 * @returns {Window}
 */
Window.prototype.open = function(url, windowName, windowFeatures, optionalArg4){return new Window();};
/**
 * function print() 
 * @memberOf Window
 */
Window.prototype.print = function(){};
/**
 * function prompt(text, value) 
 * @param {String} text
 * @param {String} value
 * @memberOf Window
 * @returns {String}
 */
Window.prototype.prompt = function(text, value){return "";};
/**
 * function resizeTo(newOuterWidth,newOuterHeight) 
 * @param {Number} newOuterWidth
 * @param {Number} newOuterHeighr
 * @memberOf Window
 */
Window.prototype.resizeTo=function(newOuterWidth,newOuterHeight){};
/**
 * function resizeBy(deltaX, deltaY) 
 * @param {Number} deltaX
 * @param {Number} deltaY
 * @memberOf Window
 */
Window.prototype.resizeBy=function(deltaX,deltaY){};
/**
 * function scrollTo(x,y) 
 * @param {Number} x
 * @param {Number} y
 * @memberOf Window
 */
Window.prototype.scrollTo=function(x,y){};
/**
 * function scrollBy(pixelX,pixelY) 
 * @param {Number} pixelX
 * @param {Number} pixelY
 * @memberOf Window
 */
Window.prototype.scrollBy=function(pixelX,pixelY){};
/**
 * function setInterval(arg1, arg2) 
 * @param {Function} callback
 * @param {Number} delay
 * @memberOf Window
 * @returns {Number}
 */
Window.prototype.setInterval=function(callback, delay){return 0;};
/**
 * function setTimeout(callback, delay) 
 * @param {Function} callback
 * @param {Number} delay
 * @memberOf Window
 * @returns {Number}
 */
Window.prototype.setTimeout=function(callback, delay){ return 0;};
/**
 * function atob(encodedData) 
 * @param {String} encodedData
 * @memberOf Window
 * @returns {String}
 */
Window.prototype.atob=function(encodedData){return "";};
/**
 * function btoa(arg) 
 * @param {String} stringToEncode
 * @memberOf Window
 * @returns {String}
 */
Window.prototype.btoa=function(stringToEncode){return "";};
/**
 * function setResizable(resizable) 
 * @param {Boolean} resizable
 * @memberOf Window
 */
Window.prototype.setResizable=function(resizable){};

Window.prototype.captureEvents=function(eventType){};
Window.prototype.releaseEvents=function(eventType){};
Window.prototype.routeEvent=function(eventType){};
Window.prototype.enableExternalCapture=function(){};
Window.prototype.disableExternalCapture=function(){};
Window.prototype.find=function(){};
Window.prototype.back=function(){};
Window.prototype.forward=function(){};
Window.prototype.home=function(){};
Window.prototype.stop=function(){};
/**
 * @param {Number} pixelX
 * @param {Number} pixelY
 */
Window.prototype.scroll=function(pixelX,pixelY){};
/* End functions */

/**
  * Object History()
  * @super Object
  * @constructor
  * @since Common Usage, no standard
 */
function History(){};
History.prototype=new Object();
History.prototype.history = new History();
/**
 * Property length
 * @type Number
 * @memberOf History
 */
History.prototype.length = 0;
/**
 * function back()
 * @memberOf History
 */
History.prototype.back = function(){};
/**
 * function forward()
 * @memberOf History
 */
History.prototype.forward = function(){};
/**
 * function back()
 * @param arg
 * @memberOf History
 */
History.prototype.go = function(arg){};

/**
  * Object Location()
  * @super Object
  * @constructor
  * @since Common Usage, no standard
 */
function Location(){};
Location.prototype = new Object();
Location.prototype.location = new Location();
/**
 * Property hash
 * @type String
 * @memberOf Location
 */
Location.prototype.hash = "";
/**
 * Property host
 * @type String
 * @memberOf Location
 */
Location.prototype.host = "";
/**
 * Property hostname
 * @type String
 * @memberOf Location
 */
Location.prototype.hostname = "";
/**
 * Property href
 * @type String
 * @memberOf Location
 */
Location.prototype.href = "";
/**
 * Property pathname
 * @type String
 * @memberOf Location
 */
Location.prototype.pathname = "";
/**
 * Property port
 * @type String
 * @memberOf Location
 */
Location.prototype.port = "";
/**
 * Property protocol
 * @type String
 * @memberOf Location
 */
Location.prototype.protocol = "";
/**
 * Property search
 * @type String
 * @memberOf Location
 */
Location.prototype.search = "";
/**
 * function assign(arg)
 * @param {String} arg
 * @memberOf Location
 */
Location.prototype.assign = function(arg){};
/**
 * function reload(optionalArg)
 * @param {Boolean} optionalArg
 * @memberOf Location
 */
Location.prototype.reload = function(optionalArg){};
/**
 * function replace(arg)
 * @param {String} arg
 * @memberOf Location
 */
Location.prototype.replace = function(arg){};

/**
 * Object Navigator()
 * @super Object
 * @constructor
 * @since Common Usage, no standard
*/
function Navigator(){};
Navigator.prototype = new Object();
Navigator.prototype.navigator = new Navigator();
/**
 * Property appCodeName
 * @type String
 * @memberOf Navigator
 */
Navigator.prototype.appCodeName = "";
/**
 * Property appName
 * @type String
 * @memberOf Navigator
 */
Navigator.prototype.appName = "";
/**
 * Property appVersion
 * @type String
 * @memberOf Navigator
 */
Navigator.prototype.appVersion = "";
/**
 * Property cookieEnabled
 * @type Boolean
 * @memberOf Navigator
 */
Navigator.prototype.cookieEnabled = new Boolean();
/**
 * Property mimeTypes
 * @type Array
 * @memberOf Navigator
 */
Navigator.prototype.mimeTypes = new Array();
/**
 * Property platform
 * @type String
 * @memberOf Navigator
 */
Navigator.prototype.platform = "";
/**
 * Property plugins
 * @type Array
 * @memberOf Navigator
 */
Navigator.prototype.plugins = new Array();
/**
 * Property userAgent
 * @type String
 * @memberOf Navigator
 */
Navigator.prototype.userAgent = "";
/**
 * function javaEnabled()
 * @returns {Boolean}
 * @memberOf Navigator
 */
Navigator.prototype.javaEnabled = function(){return false;};

/**
 * Object Screen()
 * @super Object
 * @constructor
 * @since Common Usage, no standard
*/
function Screen(){};
Screen.prototype = new Object();
Screen.prototype.screen = new Screen();
/**
 * Property availHeight
 * @type Number
 * @memberOf Screen
 */
Navigator.prototype.availHeight = 0;
/**
 * Property availWidth
 * @type Number
 * @memberOf Screen
 */
Navigator.prototype.availWidth = 0;
/**
 * Property colorDepth
 * @type Number
 * @memberOf Screen
 */
Navigator.prototype.colorDepth = 0;
/**
 * Property height
 * @type Number
 * @memberOf Screen
 */
Navigator.prototype.height = 0;
/**
 * Property width
 * @type Number
 * @memberOf Screen
 */
Navigator.prototype.width = 0;

Event.prototype=new Object();
// PhaseType
Event.prototype.CAPTURING_PHASE = 1;
Event.prototype.AT_TARGET = 2;
Event.prototype.BUBBLING_PHASE = 3;

Event.prototype.type="";
Event.prototype.target=new EventTarget();
Event.prototype.currentTarget=new EventTarget();
Event.prototype.eventPhase=0;
Event.prototype.bubbles=false;
Event.prototype.cancelable=false;
Event.prototype.timeStamp=0;
Event.prototype.stopPropagation=function(){};
Event.prototype.preventDefault=function(){};
/**
 * @param {String} eventTypeArg
 * @param {Boolean} canBubbleArg
 * @param {Boolean} cancelableArg
 */
Event.prototype.initEvent=function(eventTypeArg, 
                             canBubbleArg, 
                             cancelableArg){};
function EventListener(){};
EventListener.prototype=new Object();
/**
 * @param {Event} event
 * @memberOf EventListener
 */
EventListener.prototype.handleEvent=function(event){};

function EventTarget(){};
EventTarget.prototype=new Object();
/*
 * These functions may need to be moved into a browser specific library.
 */
/**
 * @memberOf Window
 * @param event {Event}
 * @throws {EventException}
 */
EventTarget.prototype.dispatchEvent=function(event){};

// https://developer.mozilla.org/en-US/docs/DOM/element.addEventListener
/**
 * @memberOf Window
 * @param {String} type
 * @param {EventListener} listener
 * @param {Boolean} useCapture
 */
EventTarget.prototype.addEventListener=function(type, listener, useCapture){};
// https://developer.mozilla.org/en-US/docs/DOM/element.removeEventListener
/**
 * @memberOf Window
 * @param {String} type
 * @param {EventListener} listener
 * @param {Boolean} useCapture
 */
EventTarget.prototype.removeEventListener=function(type, listener, useCapture){};

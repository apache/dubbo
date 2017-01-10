/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

/**
 * function querySelector(selectors)
 * http://www.w3.org/TR/2012/PR-selectors-api-20121213
 * @param {String} selectors
 * @memberOf Document
 * @returns {Element}      
 */ 
Document.prototype.querySelector=function(selectors){return new Element();};

/**
 * function querySelectorAll(selectors)
 * http://www.w3.org/TR/2012/PR-selectors-api-20121213
 * @param {String} selectors
 * @memberOf Document
 * @returns {NodeList}      
 */ 
Document.prototype.querySelectorAll=function(selectors){return new NodeList();};

/**
 * function querySelector(selectors)
 * http://www.w3.org/TR/2012/PR-selectors-api-20121213
 * @param {String} selectors
 * @memberOf DocumentFragment
 * @returns {Element}      
 */ 
DocumentFragment.prototype.querySelector=function(selectors){return new Element();};

/**
 * function querySelectorAll(selectors)
 * http://www.w3.org/TR/2012/PR-selectors-api-20121213
 * @param {String} selectors
 * @memberOf DocumentFragment
 * @returns {NodeList}      
 */ 
DocumentFragment.prototype.querySelectorAll=function(selectors){return new NodeList();};

/**
 * function querySelector(selectors)
 * http://www.w3.org/TR/2012/PR-selectors-api-20121213
 * @param {String} selectors
 * @memberOf Element
 * @returns {Element}      
 */ 
Element.prototype.querySelector=function(selectors){return new Element();};

/**
 * function querySelectorAll(selectors)
 * http://www.w3.org/TR/2012/PR-selectors-api-20121213
 * @param {String} selectors
 * @memberOf Element
 * @returns {NodeList}      
 */ 
Element.prototype.querySelectorAll=function(selectors){return new NodeList();};

/**
 * Property state
 * @type Object
 * @memberOf History
 */
History.prototype.state=new Object();

/**
 * function pushState(data,title,url)
 * http://www.w3.org/TR/2012/CR-html5-20121217/browsers.html#history
 * @param {Object} data
 * @param {String} title
 * @param {String} url - optional
 * @memberOf History
 */
History.prototype.pushState=function(data,title,url){};

/**
 * function replaceState(data,title,url)
 * http://www.w3.org/TR/2012/CR-html5-20121217/browsers.html#history
 * @param {Object} data
 * @param {String} title
 * @param {String} url - optional
 * @memberOf History
 */
History.prototype.replaceState=function(data,title,url){};

/**
 * Property sessionStorage
 * http://www.w3.org/TR/2011/CR-webstorage-20111208
 * @type Storage
 * @memberOf Window
 */
Window.prototype.sessionStorage=new Storage();

/**
 * Property localStorage
 * http://www.w3.org/TR/2011/CR-webstorage-20111208
 * @type Storage
 * @memberOf Window
 */
Window.prototype.localStorage=new Storage();

/**
 * Object Storage
 * http://www.w3.org/TR/2011/CR-webstorage-20111208
 */
function Storage(){};
Storage.prototype=new Object();

/**
 * Property length
 * http://www.w3.org/TR/2011/CR-webstorage-20111208
 * @type Number
 * @memberOf Storage
 */
Storage.prototype.length=new Number();

/**
 * function key(index)
 * http://www.w3.org/TR/2011/CR-webstorage-20111208
 * @param {Number} index
 * @memberOf Storage
 * @returns String
 */
Storage.prototype.key=function(index){return new String();};

/**
 * function getItem(key)
 * http://www.w3.org/TR/2011/CR-webstorage-20111208
 * @param {String} key
 * @memberOf Storage
 * @returns String
 */
Storage.prototype.getItem=function(key){return new String();};

/**
 * function setItem(key,value)
 * http://www.w3.org/TR/2011/CR-webstorage-20111208
 * @param {String} key
 * @param {String} value
 * @memberOf Storage
 */
Storage.prototype.setItem=function(key,value){};

/**
 * function removeItem(key)
 * http://www.w3.org/TR/2011/CR-webstorage-20111208
 * @param {String} key
 * @memberOf Storage
 */
Storage.prototype.removeItem=function(key){};

/**
 * function clear()
 * http://www.w3.org/TR/2011/CR-webstorage-20111208
 * @memberOf Storage
 */
Storage.prototype.clear=function(){};

/**
 * Object WebSocket
 * http://www.w3.org/TR/2012/CR-websockets-20120920
 * @constructor
 * @param {String} url
 */
function WebSocket(url){};
WebSocket.prototype=new Object();

/**
 * Constant WebSocket.CONNECTING=0
 * http://www.w3.org/TR/2012/CR-websockets-20120920
 * @constant
 * @type Number
 */
WebSocket.prototype.CONNECTING=0;

/**
 * Constant WebSocket.OPEN=1
 * http://www.w3.org/TR/2012/CR-websockets-20120920
 * @constant
 * @type Number
 */
WebSocket.prototype.OPEN=1;

/**
 * Constant WebSocket.CLOSING=2
 * http://www.w3.org/TR/2012/CR-websockets-20120920
 * @constant
 * @type Number
 */
WebSocket.prototype.CLOSING=2;

/**
 * Constant WebSocket.CLOSED=3
 * http://www.w3.org/TR/2012/CR-websockets-20120920
 * @constant
 * @type Number
 */
WebSocket.prototype.CLOSED=3;

/**
 * Property url
 * http://www.w3.org/TR/2012/CR-websockets-20120920
 * @type String
 * @memberOf WebSocket
 */
WebSocket.prototype.url=new String();

/**
 * Property readyState
 * http://www.w3.org/TR/2012/CR-websockets-20120920
 * @type Number
 * @memberOf WebSocket
 */
WebSocket.prototype.readyState=new Number();

/**
 * Property bufferedAmount
 * http://www.w3.org/TR/2012/CR-websockets-20120920
 * @type Number
 * @memberOf WebSocket
 */
WebSocket.prototype.bufferedAmount=new Number();

/**
 * Property extensions
 * http://www.w3.org/TR/2012/CR-websockets-20120920
 * @type String
 * @memberOf WebSocket
 */
WebSocket.prototype.extensions=new String();

/**
 * Property protocol
 * http://www.w3.org/TR/2012/CR-websockets-20120920
 * @type String
 * @memberOf WebSocket
 */
WebSocket.prototype.protocol=new String();

/**
 * Property binaryType
 * http://www.w3.org/TR/2012/CR-websockets-20120920
 * @type String
 * @memberOf WebSocket
 */
WebSocket.prototype.binaryType=new String();

/**
 * function close(code,reason)
 * http://www.w3.org/TR/2012/CR-websockets-20120920
 * @param {Number} code - optional
 * @param {String} reason - optional
 * @memberOf WebSocket
 */
WebSocket.prototype.close=function(code,reason){};

/**
 * function send(data)
 * http://www.w3.org/TR/2012/CR-websockets-20120920
 * @param {Object} data - may be a String, Blob, ArrayBuffer, or ArrayBufferView 
 * @memberOf WebSocket
 */
WebSocket.prototype.send=function(data){};

/**
 * Property geolocation
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @type Geolocation
 * @memberOf Navigator
 */
Navigator.prototype.geolocation=new Geolocation();

/**
 * Object Geolocation
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 */
function Geolocation(){};
Geolocation.prototype=new Object();

/**
 * function getCurrentPosition(successCallback,errorCallback,options)
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510/
 * @param {Function} successCallback (Position pos)
 * @param {Function} errorCallback (PositionError error) - optional
 * @param {PositionOptions} options - optional
 * @memberOf Geolocation
 */
Geolocation.prototype.getCurrentPosition=function(successCallback,errorCallback,options){};

/**
 * function watchPosition(successCallback,errorCallback,options)
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510/
 * @param {Function} successCallback (Position pos)
 * @param {Function} errorCallback (PositionError error) - optional
 * @param {PositionOptions} options - optional
 * @memberOf Geolocation
 * @returns {Number}
 */
Geolocation.prototype.watchPosition=function(successCallback,errorCallback,options){return new Number();};

/**
 * function clearWatch(watchId)
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @param {Number} watchId
 * @memberOf Geolocation
 */
Geolocation.prototype.clearWatch=function(watchId){};

/**
 * Object Coordinates
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 */
function Coordinates(){};
Coordinates.prototype=new Object();

/**
 * Property latitude
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @type Number
 * @memberOf Coordinates
 */
Coordinates.prototype.latitude=new Number();;

/**
 * Property longitude
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @type Number
 * @memberOf Coordinates
 */
Coordinates.prototype.longitude=new Number();;

/**
 * Property altitude
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @type Number
 * @memberOf Coordinates
 */
Coordinates.prototype.altitude=new Number();;

/**
 * Property accuracy
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @type Number
 * @memberOf Coordinates
 */
Coordinates.prototype.accuracy=new Number();;

/**
 * Property altitudeAccuracy
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @type Number
 * @memberOf Coordinates
 */
Coordinates.prototype.altitudeAccuracy=new Number();;

/**
 * Property heading
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @type Number
 * @memberOf Coordinates
 */
Coordinates.prototype.heading=new Number();;

/**
 * Property speed
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @type Number
 * @memberOf Coordinates
 */
Coordinates.prototype.speed=new Number();

/**
 * Object Position
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 */
function Position(){};
Position.prototype=new Object();

/**
 * Property coords
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @type Coordinates
 * @memberOf Position
 */
Position.prototype.coords=new Coordinates();

/**
 * Property timestamp
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @type Number
 * @memberOf Position
 */
Position.prototype.timestamp=new Number;

/**
 * Object PositionError
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 */
function PositionError(){};
PositionError.prototype=new Object();

/**
 * Constant PositionError.PERMISSION_DENIED=1
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @constant
 * @type Number
 */
PositionError.prototype.PERMISSION_DENIED=1;

/**
 * Constant PositionError.POSITION_UNAVAILABLE=2
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @constant
 * @type Number
 */
PositionError.prototype.POSITION_UNAVAILABLE=2;

/**
 * Constant PositionError.TIMEOUT=3
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @constant
 * @type Number
 */
PositionError.prototype.TIMEOUT=3;

/**
 * Property code
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @type Number
 * @memberOf PositionError
 */
PositionError.prototype.code=new Number();

/**
 * Property message
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @type String
 * @memberOf PositionError
 */
PositionError.prototype.message=new String();

/**
 * Object PositionOptions
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 */
function PositionOptions(){};
PositionOptions.prototype=new Object();

/**
 * Property enableHighAccuracy
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @type Boolean
 * @memberOf PositionOptions
 */
PositionOptions.prototype.enableHighAccuracy=new Boolean();

/**
 * Property timeout
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @type Number
 * @memberOf PositionOptions
 */
PositionOptions.prototype.timeout=new Number();

/**
 * Property maximumAge
 * http://www.w3.org/TR/2012/PR-geolocation-API-20120510
 * @type Number
 * @memberOf PositionOptions
 */
PositionOptions.prototype.maximumAge=new Number();

/**
 * Object TimeRanges
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 */
function TimeRanges(){};
TimeRanges.prototype=new Object();

/**
 * Property length
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Number
 * @memberOf TimeRanges
 */
TimeRanges.prototype.length=new Number();

/**
 * function start(index)
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @param {Number} index
 * @memberOf TimeRanges
 * @returns {Number}
 */
function start(index) {return new Number();};

/**
 * function end(index)
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @param {Number} index
 * @memberOf TimeRanges
 * @returns {Number}
 */
function end(index) {return new Number();};

/**
 * Object MediaError
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 */
function MediaError(){};
MediaError.prototype=new Object();

/**
 * Constant MediaError.MEDIA_ERR_ABORTED=1
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @constant
 * @type Number
 */
MediaError.prototype.MEDIA_ERR_ABORTED=1;

/**
 * Constant MediaError.MEDIA_ERR_NETWORK=2
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @constant
 * @type Number
 */
MediaError.prototype.MEDIA_ERR_NETWORK=2;

/**
 * Constant MediaError.MEDIA_ERR_DECODED=3
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @constant
 * @type Number
 */
MediaError.prototype.MEDIA_ERR_DECODE=3;

/**
 * Constant MediaError.MEDIA_ERR_SRC_NOT_SUPPORTED=4
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @constant
 * @type Number
 */
MediaError.prototype.MEDIA_ERR_SRC_NOT_SUPPORTED=4;

/**
 * Property code
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Number
 * @memberOf MediaError
 */
MediaError.prototype.code=new Number();

/**
 * Object HTMLMediaElement
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @augments HTMLElement
 * @see HTMLElement
 */
function HTMLMediaElement(){};
HTMLMediaElement.prototype = new HTMLElement();

/**
 * Property src
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type String
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.src=new String();

/**
 * Property currentSrc
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type String
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.currentSrc=new String();

/**
 * Property crossOrigin
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type String
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.crossOrigin=new String();

/**
 * Constant HTMLMediaElement.NETWORK_EMPTY=0
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @constant
 * @type Number
 */
HTMLMediaElement.prototype.NETWORK_EMPTY=0;

/**
 * Constant HTMLMediaElement.NETWORK_IDLE=1
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @constant
 * @type Number
 */
HTMLMediaElement.prototype.NETWORK_IDLE=1;

/**
 * Constant HTMLMediaElement.NETWORK_LOADING=2
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @constant
 * @type Number
 */
HTMLMediaElement.prototype.NETWORK_LOADING=2;

/**
 * Constant HTMLMediaElement.NETWORK_NO_SOURCE=3
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @constant
 * @type Number
 */
HTMLMediaElement.prototype.NETWORK_NO_SOURCE=3;

/**
 * Property networkState
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Number
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.networkState=new Number();

/**
 * Property preload
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type String
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.preload=new String();

/**
 * Property buffered
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type TimeRanges
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.buffered=new TimeRanges();

/**
 * function load()
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.load=function(){};

/**
 * function canPlayType(type)
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @param {String} type
 * @memberOf HTMLMediaElement
 * @returns {String}
 */
HTMLMediaElement.prototype.canPlayType=function(type){new String();};

/**
 * Constant HTMLMediaElement.HAVE_NOTHING=0
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @constant
 * @type Number
 */
HTMLMediaElement.prototype.HAVE_NOTHING=0;

/**
 * Constant HTMLMediaElement.HAVE_METADATA=1
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @constant
 * @type Number
 */
HTMLMediaElement.prototype.HAVE_METADATA=1;

/**
 * Constant HTMLMediaElement.HAVE_CURRENT_DATA=2
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @constant
 * @type Number
 */
HTMLMediaElement.prototype.HAVE_CURRENT_DATA=2;

/**
 * Constant HTMLMediaElement.HAVE_FUTURE_DATA=3
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @constant
 * @type Number
 */
HTMLMediaElement.prototype.HAVE_FUTURE_DATA=3;

/**
 * Constant HTMLMediaElement.HAVE_ENOUGH_DATA=4
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @constant
 * @type Number
 */
HTMLMediaElement.prototype.HAVE_ENOUGH_DATA=4;

/**
 * Property readyState
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Number
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.readyState=new Number();

/**
 * Property seeking
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Boolean
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.seeking=new Boolean();

/**
 * Property currentTime
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Number
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.currentTime=new Number();

/**
 * Property initialTime
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Number
 * @memberOf HTMLMediaElement
 */         
HTMLMediaElement.prototype.initialTime=new Number();

/**
 * Property duration
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Number
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.duration=new Number();

/**
 * Property startOffsetTime
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Date
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.startOffsetTime=new Date();

/**
 * Property paused
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Boolean
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.paused=new Boolean();

/**
 * Property defaultPlaybackRate
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Number
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.defaultPlaybackRate=new Number();

/**
 * Property playbackRate
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Number
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.playbackRate=new Number();

/**
 * Property played
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type TimeRanges
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.played=new TimeRanges();

/**
 * Property seekable
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type TimeRanges
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.seekable=new TimeRanges();

/**
 * Property ended
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Boolean
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.ended=new Boolean();

/**
 * Property autoplay
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Boolean
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.autoplay=new Boolean();

/**
 * Property loop
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Boolean
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.loop=new Boolean();

/**
 * function play()
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.play=function(){};

/**
 * function pause()
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.pause=function(){};

/**
 * Property controls
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Boolean
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.controls=new Boolean();

/**
 * Property volume
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Number
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.volume=new Number();

/**
 * Property muted
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Boolean
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.muted=new Boolean();
         
/**
 * Property defaultMuted
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Boolean
 * @memberOf HTMLMediaElement
 */
HTMLMediaElement.prototype.defaultMuted=new Boolean();

/**
 * Object HTMLAudioElement
 * http://www.w3.org/TR/2012/WD-html5-20120329/the-audio-element.html
 * @augments HTMLMediaElement
 * @constructor
 * @param {String} src
 * @see HTMLMediaElement
 */
function HTMLAudioElement(src){};
HTMLAudioElement.prototype = new HTMLMediaElement();

/**
 * Object HTMLVideoElement
 * http://www.w3.org/TR/2012/WD-html5-20120329/the-audio-element.html
 * @augments HTMLMediaElement
 * @see HTMLMediaElement
 */
function HTMLVideoElement(){};
HTMLVideoElement.prototype = new HTMLMediaElement();

/**
 * Property width
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Number
 * @memberOf HTMLVideoElement
 */
HTMLVideoElement.prototype.width=new Number();

/**
 * Property height
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Number
 * @memberOf HTMLVideoElement
 */
HTMLVideoElement.prototype.height=new Number();

/**
 * Property videoWidth
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Number
 * @memberOf HTMLVideoElement
 */
HTMLVideoElement.prototype.videoWidth=new Number();

/**
 * Property videoHeight
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type Number
 * @memberOf HTMLVideoElement
 */
HTMLVideoElement.prototype.videoHeight=new Number();

/**
 * Property poster
 * http://www.w3.org/TR/2012/WD-html5-20120329/media-elements.html
 * @type String
 * @memberOf HTMLVideoElement
 */
HTMLVideoElement.prototype.poster=new String();


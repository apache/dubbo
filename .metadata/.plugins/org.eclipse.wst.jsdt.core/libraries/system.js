/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************
* Please see http://www.w3.org/TR/2000/REC-DOM-Level-2-Core-20001113/ecma-script-binding.html
*/

/**
  * Object Object()
  * @constructor
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
function Object(){};
 /**
  * function toString() 
  * @memberOf   Object
  * @returns {String}
  * @see     Object
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
Object.prototype.toString = function(){return "";};
 /**
  * function toLocaleString() 
  * @memberOf   Object
  * @returns {String}
  * @see     Object
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
Object.prototype.toLocaleString = function(){return "";};
 /**
  * function valueOf() 
  * @memberOf   Object
  * @returns {Object}
  * @see     Object
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
Object.prototype.valueOf = function(){return new Object();};
 /**
  * function hasOwnProperty(name) 
  * @memberOf   Object
  * @param   {String} name
  * @returns {Boolean}
  * @see     Object
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
Object.prototype.hasOwnProperty = function(name){return true;};
 /**
  * function isPrototypeOf(o) 
  * @memberOf   Object
  * @param   {Object} o
  * @returns {Boolean}
  * @see     Object
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
Object.prototype.isPrototypeOf = function(o){return true;};
 /**
  * function propertyIsEnumerable(name) 
  * @memberOf   Object
  * @param   {Object} name
  * @returns {Boolean}
  * @see     Object
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
Object.prototype.propertyIsEnumerable = function(name){return true;};
/**
  * Property constructor
  * @type  Function
  * @memberOf Object
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */ 
Object.prototype.constructor = new Function();

/**
  * Object String()
  * @constructor
  * @extends Object
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
function String(){}
String.prototype = new Object();
/**
  * static function fromCharCode(charCode1, ...)
  * @memberOf   String
  * @param {Number} charCode
  * @returns {String}
  * @static
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.fromCharCode = function(charCode){return "";};
/**
  * Property length
  * @type    Number
  * @memberOf   String
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.prototype.length = 1;
 /**
  * function charAt(position) 
  * @memberOf   String
  * @param   {Number} position
  * @returns {String}
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.prototype.charAt = function(position){return "";};
 /**
  * function charCodeAt(position) 
  * @memberOf   String
  * @param   {Number} position
  * @returns {Number}
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.prototype.charCodeAt = function(position){return 0;};
 /**
  * function concat(value1, ...) 
  * @memberOf   String
  * @param {String} value
  * @returns {String}
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.prototype.concat = function(value){return "";};
 /**
  * function indexOf(searchString, startPosition) 
  * @memberOf   String
  * @param   {String} searchString
  * @param   {Number} startPosition
  * @returns {Number}
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.prototype.indexOf = function(searchString, startPosition){return 1;};
 /**
  * function lastIndexOf(searchString, startPosition) 
  * @memberOf   String
  * @param   {String} searchString
  * @param   {Number} startPosition
  * @returns {Number}
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.prototype.lastIndexOf = function(searchString, startPosition){return 1;};
 /**
  * function localeCompare(otherString) 
  * @memberOf   String
  * @param   {String} otherString
  * @returns {Number}
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.prototype.localeCompare = function(otherString){return 0;};
 /**
  * function match(regexp) 
  * @memberOf   String
  * @param   {RegExp} regexp
  * @returns {Array}
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.prototype.match = function(regexp){return [];};
 /**
  * function replace(regexp, replaceValue) 
  * @memberOf   String
  * @param   {RegExp} regexp
  * @param   {String} replaceValue
  * @returns {String}
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.prototype.replace = function(regexp, replaceValue){return "";};
 /**
  * function search(regexp) 
  * @memberOf   String
  * @param   {RegExp} regexp
  * @returns {Number}
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.prototype.search = function(regexp){return 1;};
 /**
  * function slice(start, end) 
  * @memberOf   String
  * @param   {Number} start
  * @param   {Number} end
  * @returns {String}
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.prototype.slice = function(start, end){return "";};
 /**
  * function split(separator, limit) 
  * @memberOf   String
  * @param   {String} separator
  * @param   {Number} limit
  * @returns {Array}
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.prototype.split = function(separator, limit){return [];};
 /**
  * function substring(start, end) 
  * @memberOf   String
  * @param   {Number} start
  * @param   {Number} end
  * @returns {String}
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.prototype.substring = function(start, end){return "";};
 /**
  * function toLowerCase() 
  * @memberOf   String
  * @returns {String}
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.prototype.toLowerCase = function(){return "";};
 /**
  * function toLocaleLowerCase() 
  * @memberOf   String
  * @returns {String}
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.prototype.toLocaleLowerCase = function(){return "";};
 /**
  * function toUpperCase() 
  * @memberOf   String
  * @returns {String}
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.prototype.toUpperCase= function (){return "";};
 /**
  * function toLocaleUpperCase() 
  * @memberOf   String
  * @returns {String}
  * @see     String
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
String.prototype.toLocaleUpperCase = function(){return "";};

/**
  * Object Number()
  * @constructor
  * @extends Object
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
function Number(){}
Number.prototype = new Object();
/**
  * property MIN_VALUE
  * @type Number
  * @memberOf Number
  * @static
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Number.MIN_VALUE = 0;
/**
  * property MAX_VALUE
  * @type Number
  * @memberOf Number
  * @static
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Number.MAX_VALUE = 0 ;
/**
  * property NaN
  * @type Number
  * @memberOf Number
  * @static
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Number.NaN = 0;
/**
  * property NEGATIVE_INFINITY
  * @type Number
  * @memberOf Number
  * @static
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition. 
 */
Number.NEGATIVE_INFINITY = 0;
/**
  * property POSITIVE_INFINITY
  * @type Number
  * @memberOf Number
  * @static
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition. 
 */
Number.POSITIVE_INFINITY = 0;
/**
  * function toFixed(fractionDigits)
  * @memberOf Number
  * @param {Number} fractionDigits
  * @returns {String}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Number.prototype.toFixed = function(fractionDigits){return "";};
/**
 * function toExponential(fractionDigits)
 * @memberOf Number
 * @param {Number} fractionDigits
 * @returns {String}
 * @since Standard ECMA-262 3rd. Edition
 * @since Level 2 Document Object Model Core Definition.
*/
Number.prototype.toExponential = function(fractionDigits){return "";};
/**
 * function toPrecision(precision)
 * @memberOf Number
 * @param {Number} fractionDigits
 * @returns {String}
 * @since Standard ECMA-262 3rd. Edition
 * @since Level 2 Document Object Model Core Definition.
*/
Number.prototype.toPrecision = function(fractionDigits){return "";};

/**
 * Object Boolean()
 * @constructor
 * @extends Object
 * @since Standard ECMA-262 3rd. Edition
 * @since Level 2 Document Object Model Core Definition. 
*/
function Boolean(){};
Boolean.prototype = new Object();

/**
  * Object Array()
  * @constructor
  * @extends Object
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
function Array(){};
Array.prototype = new Object();
/**
  * Property length
  * @type    Number
  * @memberOf   Array
  * @see     Array
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
Array.prototype.length = 1;
/**
  * function concat(args)
  * @param {Array} args
  * @returns {Array}
  * @memberOf   Array
  * @see     Array
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
Array.prototype.concat = function(args){return [];};
/**
  * function join(seperator)
  * @param {String} seperator
  * @returns {Array}
  * @memberOf   Array
  * @see     Array
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
Array.prototype.join = function(seperator){return [];};
/**
  * function pop()
  * @returns {Object}
  * @memberOf   Array
  * @see     Array
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.    
 */  
Array.prototype.pop = function(){return new Object();};
/**
  * function push(args)
  * @param {Array} args
  * @memberOf   Array
  * @see     Array
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.   
 */  
Array.prototype.push = function(args){};
/**
  * function reverse()
  * @returns {Array}
  * @memberOf   Array
  * @see     Array
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
Array.prototype.reverse = function(){return [];};
/**
  * function shift()
  * @returns {Object}
  * @memberOf   Array
  * @see     Array
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.     
 */  
Array.prototype.shift = function(){return new Object();};
/**
  * function slice(start, end)
  * @param {Number} start
  * @param {Number} end
  * @returns {Array}
  * @memberOf   Array
  * @see     Array
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.   
 */  
Array.prototype.slice = function(start, end){return [];};
/**
  * function sort(funct)
  * @param {Function} funct
  * @returns {Array}
  * @memberOf   Array
  * @see     Array
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */  
Array.prototype.sort = function(funct){return [];};
/**
  * function splice(start, deletecount, items)
  * @param {Number} start
  * @param {Number} deletecount
  * @param {Array} items
  * @returns {Array}
  * @memberOf   Array
  * @see     Array
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.  
 */  
Array.prototype.splice = function(start, deletecount, items){return [];};
/**
  * function unshift(items)
  * @param {Object} values
  * @returns {Number}
  * @memberOf   Array
  * @see     Array
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.   
 */  
Array.prototype.unshift = function(values){return 1;};

/**
  * Object Function()
  * @constructor
  * @extends Object
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
function Function(){};
Function.prototype = new Object();
/**
 * function apply (thisObject, argArray)
 * @param {Object} thisObject
 * @param {Array} argArray
 * @returns {Object}
 * @since   Standard ECMA-262 3rd. Edition 
 * @since   Level 2 Document Object Model Core Definition.
 */ 
Function.prototype.apply = function(thisArg, argArray){return new Object();};
/**
  * function call (thisObject, args)
  * @param {Object} thisObject
  * @param {Object} args
  * @returns {Object}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.    
 */ 
Function.prototype.call = function(thisObject, args){return new Object();};
/**
  * property length
  * @type    Number
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.    
 */ 
Function.prototype.length = 0;

/**
  * Object Date(s)
  * @constructor
  * @param {String} s
  * @extends Object
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition. 
 */
function Date(s){};
Date.prototype = new Object();
/**
 * function UTC(hour, min, sec, ms)
 * @memberOf Date
 * @param {Number} hour
 * @param {Number} min
 * @param {Number} sec
 * @param {Number} ms  
 * @returns {Number}
 * @static
 * @since Standard ECMA-262 3rd. Edition
 * @since Level 2 Document Object Model Core Definition.
*/
Date.UTC = function(hour, min, sec, ms){return 0;};
/**
  * function parse(string)
  * @memberOf Date
  * @param {String} string
  * @returns {Number}
  * @static
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.parse = function(string){return 0;};
/**
  * function toDateString()
  * @memberOf Date
  * @returns {String}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.toDateString = function(){return "";};
/**
  * function toTimeString()
  * @memberOf Date
  * @returns {String}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.toTimeString = function(){return "";};
/**
  * function toLocaleString()
  * @memberOf Date
  * @returns {String}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition. 
 */
Date.prototype.toLocaleString = function(){return "";};
/**
  * function toLocaleDateString()
  * @memberOf Date
  * @returns {String}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.toLocaleDateString = function(){return "";};
/**
  * function toLocaleTimeString()
  * @memberOf Date
  * @returns {String}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.toLocaleTimeString = function(){return "";};
/**
  * function valueOf()
  * @memberOf Date
  * @returns {Object}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.valueOf = function(){return new Object();};
/**
  * function getFullYear()
  * @memberOf Date
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.getFullYear = function(){return 0;};
/**
  * function getTime()
  * @memberOf Date
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.  
 */
Date.prototype.getTime = function(){return 0;};
/**
  * function getUTCFullYear()
  * @memberOf Date
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition. 
 */
Date.prototype.getUTCFullYear = function(){return 0;};
/**
  * function getMonth()
  * @memberOf Date
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.getMonth = function(){return 0;};
/**
  * function getUTCMonth()
  * @memberOf Date
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.getUTCMonth = function(){return 0;};
/**
  * function getDate()
  * @memberOf Date
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.getDate = function(){return 0;};
/**
  * function getUTCDate()
  * @memberOf Date
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.getUTCDate = function(){return 0;};
/**
  * function getDay()
  * @memberOf Date
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition. 
 */
Date.prototype.getDay = function(){return 0;};
/**
  * function getUTCDay()
  * @memberOf Date
  * @type Number
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
     
 */
Date.prototype.getUTCDay=function(){return 0;};
/**
  * function getHours()
  * @memberOf Date
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition. 
 */
Date.prototype.getHours = function(){return 0;};
/**
  * function getUTCHours()
  * @memberOf Date
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.getUTCHours = function(){return 0;};
/**
  * function getMinutes()
  * @memberOf Date
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.getMinutes = function(){return 0;};
/**
  * function getUTCMinutes()
  * @memberOf Date
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.getUTCMinutes = function(){return 0;};
/**
  * function getSeconds()
  * @memberOf Date
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.getSeconds = function(){return 0;};
/**
  * function getUTCSeconds()
  * @memberOf Date
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.getUTCSeconds = function(){return 0;};
/**
  * function getMilliseconds()
  * @memberOf Date
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.getMilliseconds = function(){return 0;};
/**
  * function getUTCMilliseconds()
  * @memberOf Date
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.getUTCMilliseconds = function(){return 0;};
/**
  * function getTimezoneOffset()
  * @memberOf Date
  * @returns {Number}
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.getTimezoneOffset = function(){return 0;};
/**
  * function setTime(value)
  * @memberOf Date
  * @returns {Number}
  * @param {Number} value
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.setTime = function(value){return 0;};

/**
  * function setMilliseconds(value)
  * @memberOf Date
  * @returns {Number}
  * @param {Number} value
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.setMilliseconds = function(value){return 0;};
/**
  * function setUTCMilliseconds(ms)
  * @memberOf Date
  * @returns {Number}
  * @param {Number} ms
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.setUTCMilliseconds = function(ms){return 0;};
/**
  * function setSeconds(sec,ms)
  * @memberOf Date
  * @returns {Number}
  * @param {Number} sec
  * @param {Number} ms
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.setSeconds = function(sec,ms){return 0;};
/**
  * function setUTCSeconds(sec,ms)
  * @memberOf Date
  * @returns {Number}
  * @param {Number} sec
  * @param {Number} ms
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.setUTCSeconds=function(sec,ms){return 0;};
/**
  * function setMinutes(min,sec,ms)
  * @memberOf Date
  * @returns {Number}
  * @param {Number} min
  * @param {Number} sec
  * @param {Number} ms
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.setMinutes=function(min,sec,ms){return 0;};
/**
  * function setUTCMinute(min,sec,ms)
  * @memberOf Date
  * @returns {Number}
  * @param {Number} min
  * @param {Number} sec
  * @param {Number} ms
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.setUTCMinute = function(min,sec,ms){return 0;};
/**
  * function setHours(hour, min,sec,ms)
  * @memberOf Date
  * @returns {Number}
  * @param {Number} hour
  * @param {Number} min
  * @param {Number} sec
  * @param {Number} ms
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.setHours = function(hour,min,sec,ms){return 0;};
/**
  * function setUTCHours(hour, min,sec,ms)
  * @memberOf Date
  * @returns {Number}
  * @param {Number} hour
  * @param {Number} min
  * @param {Number} sec
  * @param {Number} ms
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.setUTCHours = function(hour,min,sec,ms){return 0;};

/**
  * function setDate(date)
  * @memberOf Date
  * @returns {Number}
  * @param {Number} date
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.setDate = function(date){return 0;};

/**
  * function setUTCDate(date)
  * @memberOf Date
  * @returns {Number}
  * @param {Number} date
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.setUTCDate = function(date){return 0;};

/**
  * function setMonth(month,date)
  * @memberOf Date
  * @returns {Number}
  * @param {Number} date
  * @param {Number} month
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition. 
 */
Date.prototype.setMonth = function(month,date){return 1;};
/**
  * function setUTCMonth(month,date)
  * @memberOf Date
  * @returns {Number}
  * @param {Number} date
  * @param {Number} month
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.setUTCMonth = function(month,date){return 1;};
/**
  * function setFullYear(month,date)
  * @memberOf Date
  * @returns {Number}
  * @param {Number} date
  * @param {Number} month
  * @param {Number} year
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition. 
 */
Date.prototype.setFullYear = function(year, month,date){return 0;};
/**
  * function setUTCFullYear(month,date)
  * @memberOf Date
  * @returns {Date}
  * @param {Number} date
  * @param {Number} month
  * @param {Number} year
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
Date.prototype.setUTCFullYear = function(year, month,date){};
/**
 * function toUTCString()
 * @memberOf Date
 * @returns {String}
 * @since Standard ECMA-262 3rd. Edition
 * @since Level 2 Document Object Model Core Definition.
*/
Date.prototype.toUTCString = function(){return "";};

/**
  * Property NaN
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.  
 */
var NaN=0;
/**
  * Property Infinity
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.  
 */
var Infinity=0;
/**
  * function eval(s)
  * @param {String} s
  * @type Object
  * @returns {Object}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.     
 */
function eval(s){return new Object();};

//@GINO: Bug 197987 (Temp Fix)
/**
  * Property debugger
  * @description Debugger keyword
 */
var debugger=null;

/**
 * Property undefined
 * @description undefined
*/
var undefined=null;

/**
  * function parseInt(s,radix)
  * @param {String} s
  * @param {Number} radix
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.    
 */
function parseInt(s,radix){return 0;};
/**
  * function parseFloat(s)
  * @param {String} s
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.   
 */
function parseFloat(s){return 0;};
/**
 * function escape(s)
 * @param {String} s
 * @type String
 * @returns {String}
 * @since   Standard ECMA-262 3rd. Edition 
 * @since   Level 2 Document Object Model Core Definition.   
*/
function escape(s){return "";};
/**
 * function unescape(s)
 * @param {String} s
 * @type String
 * @returns {String}
 * @since   Standard ECMA-262 3rd. Edition 
 * @since   Level 2 Document Object Model Core Definition.   
*/
function unescape(s){return "";};
/**
  * function isNaN(number)
  * @param {String} number
  * @type Boolean
  * @returns {Boolean}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.  
 */
function isNaN(number){return false;};
/**
  * function isFinite(number)
  * @param {String} number
  * @type Boolean
  * @returns {Boolean}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.    
 */
function isFinite(number){return false;};
/**
 * function decodeURI(encodedURI)
 * @param {String} encodedURI
 * @type String
 * @returns {String}
 * @since   Standard ECMA-262 3rd. Edition 
 * @since   Level 2 Document Object Model Core Definition.  
*/
function decodeURI(encodedURI){return "";};
/**
 * @param {String} uriComponent
 * @type String
 * @returns {String}
 * @since   Standard ECMA-262 3rd. Edition 
 * @since   Level 2 Document Object Model Core Definition.  
*/
function decodeURIComponent(uriComponent){return "";};
/**
 * function encodeURIComponent(uriComponent)
 * @param {String} uriComponent
 * @type String
 * @returns {String}
 * @since   Standard ECMA-262 3rd. Edition 
 * @since   Level 2 Document Object Model Core Definition.    
*/
function encodeURIComponent(uriComponent){return "";};

/**
 * function encodeURIComponent(URI)
 * @param {String} URI
 * @type String
 * @returns {String}
 * @since   Standard ECMA-262 3rd. Edition 
 * @since   Level 2 Document Object Model Core Definition.    
*/
function encodeURI(URI){return "";};

/**
  * Object Math(\s)
  * @super Object
  * @constructor
  * @memberOf Math
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
     
 */
function Math(){};
Math.prototype=new Object();
/**
  * Property E
  * @memberOf Math
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.    
 */
Math.E=0;
/**
  * Property LN10
  * @memberOf Math
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.    
 */
Math.LN10=0;
/**
  * Property LN2
  * @memberOf Math
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */
Math.LN2=0;
/**
  * Property LOG2E
  * @memberOf Math
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.    
 */
Math.LOG2E=0;
/**
  * Property LOG10E
  * @memberOf Math
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition. 
 */
Math.LOG10E=0;
/**
  * Property PI
  * @memberOf Math
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.  
 */
Math.PI=0;
/**
  * Property SQRT1_2
  * @memberOf Math
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.    
 */
Math.SQRT1_2=0;
/**
  * Property SQRT2
  * @memberOf Math
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition. 
 */
Math.SQRT2=0;
/**
  * function abs(x)
  * @memberOf Math
  * @param {Number} x
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.     
 */
Math.abs=function(x){return 0;};
/**
  * function acos(x)
  * @memberOf Math
  * @param {Number} x
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */
Math.acos=function(x){return 0;};
/**
  * function asin(x)
  * @memberOf Math
  * @param {Number} x
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.  
 */
Math.asin=function(x){return 0;};
/**
  * function atan(x)
  * @memberOf Math
  * @param {Number} x
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.
 */
Math.atan=function(x){return 0;};
/**
  * function atan2(x,y)
  * @memberOf Math
  * @param {Number} x
  * @param {Number} y
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.  
 */
Math.atan2=function(x,y){return 0;};
/**
  * function ceil(x)
  * @memberOf Math
  * @param {Number} x
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.    
 */
Math.ceil=function(x){return 0;};
/**
  * function cos(x)
  * @memberOf Math
  * @param {Number} x
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.  
 */
Math.cos=function(x){return 0;};
/**
  * function exp(x)
  * @memberOf Math
  * @param {Number} x
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition. 
 */
Math.exp=function(x){return 0;};
/**
  * function floor(x)
  * @memberOf Math
  * @param {Number} x
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.  
 */
Math.floor=function(x){return 0;};
/**
  * function log(x)
  * @memberOf Math
  * @param {Number} x
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.    
 */
Math.log=function(x){return 0;};
/**
  * function max(arg)
  * @memberOf Math
  * @param {Number} args
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.  
 */
Math.max=function(args){return 0;};
/**
  * function min(arg)
  * @memberOf Math
  * @param {Number} args
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.    
 */
Math.min=function(args){return 0;};
/**
  * function pow(x,y)
  * @memberOf Math
  * @param {Number} x
  * @param {Number} y
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.    
 */
Math.pow=function(x,y){return 0;};
/**
  * function pow()
  * @memberOf Math
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.     
 */
Math.random=function(){return 0;};
/**
  * function round(x)
  * @memberOf Math
  * @param {Number} x
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.   
 */
Math.round=function(x){return 0;};
/**
  * function sin(x)
  * @memberOf Math
  * @param {Number} x
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.    
 */
Math.sin=function(x){return 0;};
/**
  * function sqrt(x)
  * @memberOf Math
  * @param {Number} x
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.     
 */
Math.sqrt=function(x){return 0;};
/**
  * function tan(x)
  * @memberOf Math
  * @param {Number} x
  * @type Number
  * @returns {Number}
  * @since   Standard ECMA-262 3rd. Edition 
  * @since   Level 2 Document Object Model Core Definition.    
 */
Math.tan=function(x){return 0;};
/**
  * Object RegExp()
  * @super Object
  * @constructor
  * @memberOf RegExp
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
function RegExp(){};
RegExp.prototype=new Object();
/**
  * function exec(string)
  * @param {String} string
  * @returns {Array}
  * @type Array
  * @memberOf RegExp
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
RegExp.prototype.exec=function(string){return [];};
/**
  * function test(string)
  * @param {String} string
  * @returns {Boolean}
  * @type Boolean
  * @memberOf RegExp
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.  
 */
RegExp.prototype.test=function(string){return false;};
/**
  * property source
  * @type String
  * @memberOf RegExp
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition. 
 */
RegExp.prototype.source="";
/**
  * property global
  * @type Boolean
  * @memberOf RegExp
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
RegExp.prototype.global=false;

/**
  * property ignoreCase
  * @type Boolean
  * @memberOf RegExp
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition. 
 */
RegExp.prototype.ignoreCase=false;
/**
  * property multiline
  * @type Boolean
  * @memberOf RegExp
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
RegExp.prototype.multiline=false;
/**
  * property lastIndex
  * @type Number
  * @memberOf RegExp
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
RegExp.prototype.lastIndex=0;
/**
  * Object Error(message)
  * @super Object
  * @constructor
  * @param {String} message
  * @memberOf Error
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition. 
 */
function Error(message){};
Error.prototype=new Object();
/**
  * property name
  * @type String
  * @memberOf Error
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition. 
 */
Error.prototype.name="";
/**
  * property message
  * @type String
  * @memberOf Error
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition. 
 */
Error.prototype.message="";
/**
  * Object EvalError()
  * @super Error
  * @constructor
  *
  * @memberOf EvalError
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
function EvalError(){};
EvalError.prototype=new Error("");
/**
  * Object RangeError()
  * @super Error
  * @constructor
  *
  * @memberOf RangeError
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
function RangeError(){};
RangeError.prototype=new Error("");
/**
  * Object ReferenceError()
  * @super Error
  * @constructor
  *
  * @memberOf ReferenceError
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
function ReferenceError(){};
ReferenceError.prototype=new Error("");
/**
  * Object SyntaxError()
  * @super Error
  * @constructor
  *
  * @memberOf SyntaxError
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
function SyntaxError(){};
SyntaxError.prototype=new Error("");
/**
  * Object TypeError()
  * @super Error
  * @constructor
  *
  * @memberOf TypeError
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
function TypeError(){};
TypeError.prototype=new Error("");
/**
  * Object URIError()
  * @super Error
  * @constructor
  *
  * @memberOf URIError
  * @since Standard ECMA-262 3rd. Edition
  * @since Level 2 Document Object Model Core Definition.
 */
function URIError(){};
URIError.prototype=new Error("");

//support for debugger keyword
var debugger = null;
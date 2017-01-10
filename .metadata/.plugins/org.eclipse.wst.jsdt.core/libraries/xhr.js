/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************
 *
 * Based on information from https://developer.mozilla.org/En/XMLHttpRequest
 * and http://msdn2.microsoft.com/en-us/library/ms533062.aspx
 **/

/**
* function createRequest
* @type XMLHttpRequest
* @memberOf Window
*/
Window.prototype.createRequest= function(){return new XMLHttpRequest();};
/**
* Object XMLHttpRequest
* @type constructor
*/
XMLHttpRequest.prototype=new Object();
function XMLHttpRequest(){};

/**
 * function onreadystatechange
 * @memberOf XMLHttpRequest
 */
XMLHttpRequest.prototype.onreadystatechange=function(){};
/**
 * property readyState
 * @type Number
 * @memberOf XMLHttpRequest
 */
XMLHttpRequest.prototype.readyState=0;
/**
 * property responseText
 * @type String
 * @memberOf XMLHttpRequest
 */
XMLHttpRequest.prototype.responseText="";
/**
 * property responseXML
 * @type Document
 * @memberOf XMLHttpRequest
 */
XMLHttpRequest.prototype.responseXML=new Document();
/**
 * property status
 * @type Number
 * @memberOf XMLHttpRequest
 */
XMLHttpRequest.prototype.status=0;
/**
 * property statusText
 * @type String
 * @memberOf XMLHttpRequest
 */
XMLHttpRequest.prototype.statusText="";
/**
 * function abort()
 * @memberOf XMLHttpRequest
 */
XMLHttpRequest.prototype.abort=function(){};
/**
* function getAllResponseHeaders()
* @type String
* @memberOf XMLHttpRequest
*/
XMLHttpRequest.prototype.getAllResponseHeaders=function(){return "";};
/**
* function open(method, url, async, username, password)
* @param {String} method
* @param {String} url
* @param {Boolean} optional async
* @param {String} optional username
* @param {String} optional password
* @memberOf XMLHttpRequest
*/
XMLHttpRequest.prototype.open=function(method, url, async, username, password){};
/**
* function send(body)
* @param {Object} body
* @memberOf XMLHttpRequest
*/
XMLHttpRequest.prototype.send=function(body){};
/**
* function setRequestHeader(header,value)
* @param {String} header
* @param {String} value
* @memberOf XMLHttpRequest
*/
XMLHttpRequest.prototype.setRequestHeader=function(header,value){};
/**
* function getAllResponseHeaders()
* @param {String} header
* @type String
* @memberOf XMLHttpRequest
*/
XMLHttpRequest.prototype.getResponseHeader=function(header){return "";};

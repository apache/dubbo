/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.service;

import com.alibaba.dubbo.common.utils.StringUtils;

/**
 * GenericException
 * 
 * @serial Don't change the class name and properties.
 * @author william.liangf
 * @export
 */
public class GenericException extends RuntimeException {

	private static final long serialVersionUID = -1182299763306599962L;

	private String exceptionClass;

    private String exceptionMessage;
	
	public GenericException() {
	}

    public GenericException(String exceptionClass, String exceptionMessage) {
        super(exceptionMessage);
        this.exceptionClass = exceptionClass;
        this.exceptionMessage = exceptionMessage;
    }

	public GenericException(Throwable cause) {
		super(StringUtils.toString(cause));
		this.exceptionClass = cause.getClass().getName();
		this.exceptionMessage = cause.getMessage();
	}

	public String getExceptionClass() {
		return exceptionClass;
	}

	public void setExceptionClass(String exceptionClass) {
		this.exceptionClass = exceptionClass;
	}

	public String getExceptionMessage() {
		return exceptionMessage;
	}

	public void setExceptionMessage(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}

}
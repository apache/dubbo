/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.service;

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.beans.Transient;

/**
 * GenericException
 *
 * @export
 */
public class GenericException extends RuntimeException {

    private static final long serialVersionUID = -1182299763306599962L;

    private boolean useCause;

    private String exceptionClass;

    private String exceptionMessage;

    public GenericException() {
        this.useCause = false;
    }

    public GenericException(String exceptionClass, String exceptionMessage) {
        super(exceptionMessage);
        this.useCause = false;
        this.exceptionClass = exceptionClass;
        this.exceptionMessage = exceptionMessage;
    }

    public GenericException(String exceptionClass, String exceptionMessage, String message) {
        super(message);
        this.useCause = false;
        this.exceptionClass = exceptionClass;
        this.exceptionMessage = exceptionMessage;
    }

    public GenericException(Throwable cause) {
        super(StringUtils.toString(cause));
        this.useCause = false;
        this.exceptionClass = cause.getClass().getName();
        this.exceptionMessage = cause.getMessage();
    }

    @Transient
    public String getExceptionClass() {
        return exceptionClass;
    }


    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    @Transient
    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }
    
    @Override
    @Transient
    public String getMessage() {
        if(this.useCause) {
            Throwable cause = getCause();
            if(cause != null) {
                return cause.getMessage();
            }
        }
        GenericExceptionInfo genericExceptionInfo = new GenericExceptionInfo(exceptionClass, exceptionMessage, super.getMessage());
        return JsonUtils.getJson().toJson(genericExceptionInfo);
    }

    public String getGenericException() {
        return getMessage();
    }

    public void setGenericException(String json) {
        GenericExceptionInfo info = JsonUtils.getJson().toJavaObject(json, GenericExceptionInfo.class);
        this.useCause = true;
        initCause(new GenericException(info.getExClass(), info.getExMsg(), info.getMsg()));
    }

    @Override
    @Transient
    public String getLocalizedMessage() {
        return getMessage();
    }

    /**
     * create generic exception info
     */
    public static class GenericExceptionInfo {
        private String exClass;
        private String exMsg;
        private String msg;

        public GenericExceptionInfo(String exceptionClass, String exceptionMessage, String message) {
            this.exClass = exceptionClass;
            this.exMsg = exceptionMessage;
            this.msg = message;
        }

        public String getMsg() {
            return msg;
        }

        public String getExClass() {
            return exClass;
        }

        public String getExMsg() {
            return exMsg;
        }

        public void setExClass(String exClass) {
            this.exClass = exClass;
        }

        public void setExMsg(String exMsg) {
            this.exMsg = exMsg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }
    }
}

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
import java.io.Serializable;

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

    private final GenericExceptionInfo genericExceptionInfo;

    public GenericException() {
        this(null, null);
    }

    public GenericException(String exceptionClass, String exceptionMessage) {
        super(exceptionMessage);
        this.useCause = false;
        this.exceptionClass = exceptionClass;
        this.exceptionMessage = exceptionMessage;
        this.genericExceptionInfo = new GenericExceptionInfo(exceptionClass, exceptionMessage, exceptionMessage, getStackTrace());
    }

    public GenericException(Throwable cause) {
        super(StringUtils.toString(cause));
        this.useCause = false;
        this.exceptionClass = cause.getClass().getName();
        this.exceptionMessage = cause.getMessage();
        this.genericExceptionInfo = new GenericExceptionInfo(this.exceptionClass, this.exceptionMessage, super.getMessage(), getStackTrace());
    }

    protected GenericException(GenericExceptionInfo info) {
        super(info.getMsg(), null, true, false);
        setStackTrace(info.getStackTrace());
        this.useCause = false;
        this.exceptionClass = info.getExClass();
        this.exceptionMessage = info.getExMsg();
        this.genericExceptionInfo = info;
    }

    @Transient
    public String getExceptionClass() {
        if(this.useCause) {
            return ((GenericException)getCause()).getExceptionClass();
        }
        return exceptionClass;
    }


    public void setExceptionClass(String exceptionClass) {
        if(this.useCause) {
            ((GenericException)getCause()).setExceptionClass(exceptionClass);
            return;
        }
        this.exceptionClass = exceptionClass;
    }

    @Transient
    public String getExceptionMessage() {
        if(this.useCause) {
            return ((GenericException)getCause()).getExceptionMessage();
        }
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        if(this.useCause) {
            ((GenericException)getCause()).setExceptionMessage(exceptionMessage);
            return;
        }
        this.exceptionMessage = exceptionMessage;
    }

    @Override
    @Transient
    public StackTraceElement[] getStackTrace() {
        if(this.useCause) {
            return ((GenericException)getCause()).getStackTrace();
        }
        return super.getStackTrace();
    }

    @Override
    @Transient
    public String getMessage() {
        if(this.useCause) {
           return getCause().getMessage();
        }
        return JsonUtils.toJson(GenericExceptionInfo.createNoStackTrace(genericExceptionInfo));
    }

    public String getGenericException() {
        if(this.useCause) {
            return ((GenericException)getCause()).getGenericException();
        }
        return JsonUtils.toJson(genericExceptionInfo);
    }

    public void setGenericException(String json) {
        GenericExceptionInfo info = JsonUtils.toJavaObject(json, GenericExceptionInfo.class);
        if(info == null) {
            return;
        }
        this.useCause = true;
        initCause(new GenericException(info));
    }

    @Override
    @Transient
    public String getLocalizedMessage() {
        return getMessage();
    }

    /**
     * create generic exception info
     */
    public static class GenericExceptionInfo implements Serializable {
        private String exClass;
        private String exMsg;
        private String msg;
        private StackTraceElement[] stackTrace;

        public GenericExceptionInfo() {
        }

        public GenericExceptionInfo(String exceptionClass, String exceptionMessage, String message, StackTraceElement[] stackTrace) {
            this.exClass = exceptionClass;
            this.exMsg = exceptionMessage;
            this.msg = message;
            this.stackTrace = stackTrace;
        }

        public static GenericExceptionInfo createNoStackTrace(GenericExceptionInfo info) {
            return new GenericExceptionInfo(info.getExClass(), info.getExMsg(), info.getMsg(), null);
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

        public StackTraceElement[] getStackTrace() {
            return stackTrace;
        }

        public void setStackTrace(StackTraceElement[] stackTrace) {
            this.stackTrace = stackTrace;
        }
    }
}

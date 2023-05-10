package com.alibaba.dubbo.rpc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public class RpcResult extends Result.AbstractResult implements Serializable {
    private static final long serialVersionUID = -6925924956850004727L;
    private Object result;
    private Throwable exception;
    private Map<String, String> attachments = new HashMap();

    public RpcResult() {
    }

    public RpcResult(Object result) {
        this.result = result;
    }

    public RpcResult(Throwable exception) {
        this.exception = exception;
    }

    public Object recreate() throws Throwable {
        if (this.exception != null) {
            throw this.exception;
        } else {
            return this.result;
        }
    }

    /** @deprecated */
    @Deprecated
    public Object getResult() {
        return this.getValue();
    }

    /** @deprecated */
    @Deprecated
    public void setResult(Object result) {
        this.setValue(result);
    }

    public Object getValue() {
        return this.result;
    }

    public void setValue(Object value) {
        this.result = value;
    }

    public Throwable getException() {
        return this.exception;
    }

    public void setException(Throwable e) {
        this.exception = e;
    }

    public boolean hasException() {
        return this.exception != null;
    }

    public Map<String, String> getAttachments() {
        return this.attachments;
    }

    public void setAttachments(Map<String, String> map) {
        this.attachments = (Map)(map == null ? new HashMap() : map);
    }

    public void addAttachments(Map<String, String> map) {
        if (map != null) {
            if (this.attachments == null) {
                this.attachments = new HashMap();
            }

            this.attachments.putAll(map);
        }

    }

    public String getAttachment(String key) {
        return (String)this.attachments.get(key);
    }

    public String getAttachment(String key, String defaultValue) {
        String result = (String)this.attachments.get(key);
        if (result == null || result.length() == 0) {
            result = defaultValue;
        }

        return result;
    }

    public void setAttachment(String key, String value) {
        this.attachments.put(key, value);
    }

    @Override
    public void setAttachment(String key, Object value) {

    }

    @Override
    public void setObjectAttachment(String key, Object value) {

    }

    public String toString() {
        return "RpcResult [result=" + this.result + ", exception=" + this.exception + "]";
    }
}


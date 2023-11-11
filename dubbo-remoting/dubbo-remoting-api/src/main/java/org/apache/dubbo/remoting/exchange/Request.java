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
package org.apache.dubbo.remoting.exchange;

import org.apache.dubbo.common.utils.StringUtils;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.dubbo.common.constants.CommonConstants.HEARTBEAT_EVENT;
import static org.apache.dubbo.remoting.Constants.USE_SECURE_RANDOM_ID;

/**
 * Request.
 */
public class Request {

    private static final AtomicLong INVOKE_ID;

    private final long mId;

    private String mVersion;

    private boolean mTwoWay = true;

    private boolean mEvent = false;

    private boolean mBroken = false;

    private int mPayload;

    private Object mData;

    public Request() {
        mId = newId();
    }

    public Request(long id) {
        mId = id;
    }

    static {
        long startID = ThreadLocalRandom.current().nextLong();
        if (Boolean.parseBoolean(System.getProperty(USE_SECURE_RANDOM_ID, "false"))) {
            try {
                SecureRandom rand = new SecureRandom(SecureRandom.getSeed(20));
                startID = rand.nextLong();
            } catch (Throwable ignore) {
            }
        }
        INVOKE_ID = new AtomicLong(startID);
    }

    private static long newId() {
        // getAndIncrement() When it grows to MAX_VALUE, it will grow to MIN_VALUE, and the negative can be used as ID
        return INVOKE_ID.getAndIncrement();
    }

    private static String safeToString(Object data) {
        if (data == null) {
            return null;
        }

        try {
            return data.toString();
        } catch (Exception e) {
            return "<Fail toString of " + data.getClass() + ", cause: " + StringUtils.toString(e) + ">";
        }
    }

    public long getId() {
        return mId;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        mVersion = version;
    }

    public boolean isTwoWay() {
        return mTwoWay;
    }

    public void setTwoWay(boolean twoWay) {
        mTwoWay = twoWay;
    }

    public boolean isEvent() {
        return mEvent;
    }

    public void setEvent(String event) {
        this.mEvent = true;
        this.mData = event;
    }

    public void setEvent(boolean mEvent) {
        this.mEvent = mEvent;
    }

    public boolean isBroken() {
        return mBroken;
    }

    public void setBroken(boolean mBroken) {
        this.mBroken = mBroken;
    }

    public int getPayload() {
        return mPayload;
    }

    public void setPayload(int mPayload) {
        this.mPayload = mPayload;
    }

    public Object getData() {
        return mData;
    }

    public void setData(Object msg) {
        mData = msg;
    }

    public boolean isHeartbeat() {
        return mEvent && HEARTBEAT_EVENT == mData;
    }

    public void setHeartbeat(boolean isHeartbeat) {
        if (isHeartbeat) {
            setEvent(HEARTBEAT_EVENT);
        }
    }

    public Request copy() {
        Request copy = new Request(mId);
        copy.mVersion = this.mVersion;
        copy.mTwoWay = this.mTwoWay;
        copy.mEvent = this.mEvent;
        copy.mBroken = this.mBroken;
        copy.mPayload = this.mPayload;
        copy.mData = this.mData;
        return copy;
    }

    public Request copyWithoutData() {
        Request copy = new Request(mId);
        copy.mVersion = this.mVersion;
        copy.mTwoWay = this.mTwoWay;
        copy.mEvent = this.mEvent;
        copy.mBroken = this.mBroken;
        copy.mPayload = this.mPayload;
        return copy;
    }

    @Override
    public String toString() {
        return "Request [id=" + mId + ", version=" + mVersion + ", twoWay=" + mTwoWay + ", event=" + mEvent
                + ", broken=" + mBroken + ", mPayload=" + mPayload + ", data="
                + (mData == this ? "this" : safeToString(mData)) + "]";
    }
}

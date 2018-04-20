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
package com.alibaba.dubbo.flowcontrol;


import java.util.concurrent.atomic.AtomicLong;

public class Bucket {
    private AtomicLong startTime= new AtomicLong(0);
    private AtomicLong endTime=new AtomicLong(0);
    private AtomicLong longAdderTotle =new AtomicLong();
    private AtomicLong longAdderFail =new AtomicLong();
    public void addLongAdderTotle(){
        longAdderTotle.incrementAndGet();
    }
    public void addLongAdderFail(){
        longAdderFail.incrementAndGet();
    }

    /**
     * 求和。。。。。
     * @return
     */
    public long sumLongAdderTotle(){
        return longAdderTotle.longValue();
    }
    public long sumLongAdderFail(){
        return longAdderFail.longValue();
    }

    public void setStartTime(){
        startTime.set(System.currentTimeMillis());
    }
   public void setStartTime(long sTime){
        startTime.set(sTime);
    }
    public long getStartTime() {
        return startTime.get();
    }
    public void setEndTime(long edTime){
        endTime.set(edTime);
    }
    public long getEndTime(){
        return endTime.get();
    }
    public Bucket(){
        this.setStartTime();
        this.setEndTime(startTime.get()+1000);
    }
  public void reset(long eTime){
      this.setStartTime(eTime-1000);
      this.setEndTime(eTime);
    longAdderTotle.set(0);
      longAdderFail.set(0);
  }
    @Override
    public String toString() {
        return "Bucket{" +
                "startTime=" + startTime.get() +
                ", endTime=" + endTime.get() +
                ", longAdderTotle=" + longAdderTotle.toString() +
                ", longAdderFail=" + longAdderFail.toString() +
                '}';
    }
}

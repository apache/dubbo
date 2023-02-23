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
package org.apache.dubbo.rpc.flowcontrol.collector;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

//import java.lang.management.ManagementFactory;
//import com.sun.management.OperatingSystemMXBean;

public class HardwareMetricsCollector {
    private static final List<String> OPERATING_SYSTEM_BEAN_CLASS_NAMES = Arrays.asList("com.ibm.lang.management.OperatingSystemMXBean", "com.sun.management.OperatingSystemMXBean");

    private final OperatingSystemMXBean operatingSystemBean;

    private Method systemCpuUsage;

    private Method processCpuUsage;
    private Method getTotalPhysicalMemorySize;
    private Method getFreePhysicalMemorySize;
    private final Class<?> operatingSystemBeanClass;


    public HardwareMetricsCollector(){
        //this.operatingSystemBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        this.operatingSystemBean = ManagementFactory.getOperatingSystemMXBean();
        //this.operatingSystemBeanClass = this.getFirstClassFound(OPERATING_SYSTEM_BEAN_CLASS_NAMES);
        this.operatingSystemBeanClass = this.getSecondClassFound(OPERATING_SYSTEM_BEAN_CLASS_NAMES);
        Method getCpuLoad = this.detectMethod("getCpuLoad");
        this.systemCpuUsage = getCpuLoad != null?getCpuLoad:this.detectMethod("getSystemCpuLoad");
        this.processCpuUsage = this.detectMethod("getProcessCpuLoad");
        this.getTotalPhysicalMemorySize = this.detectMethod("getTotalPhysicalMemorySize");
        this.getFreePhysicalMemorySize = this.detectMethod("getFreePhysicalMemorySize");
    }

    public int jvmClassesLoaded(){
        ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();
        return classLoadingBean.getLoadedClassCount();
    }

    public long jvmClassesUnloaded(){
        ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();
        return classLoadingBean.getUnloadedClassCount();
    }

    public Map<String,Long> jvmBufferCount(){
        Iterator var2 = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class).iterator();
        Map<String,Long> jvmBufferCount = new ConcurrentHashMap<>();
        while(var2.hasNext()){
            BufferPoolMXBean bufferPoolBean = (BufferPoolMXBean)var2.next();
            jvmBufferCount.put(bufferPoolBean.getName(), bufferPoolBean.getCount());
            //System.out.println(bufferPoolBean.getName() + " "+ bufferPoolBean.getCount());
        }
        return jvmBufferCount;
    }

    public Map<String,Long> jvmBufferMemoryUsed(){
        Iterator var2 = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class).iterator();
        Map<String,Long> jvmMemoryUsed = new ConcurrentHashMap<>();
        while(var2.hasNext()){
            BufferPoolMXBean bufferPoolBean = (BufferPoolMXBean)var2.next();
            jvmMemoryUsed.put(bufferPoolBean.getName(), bufferPoolBean.getMemoryUsed());
            //System.out.println(bufferPoolBean.getName() + " " + bufferPoolBean.getMemoryUsed());
        }
        return jvmMemoryUsed;
    }

    public Map<String,Long> jvmBufferTotalCapacity(){
        Iterator var2 = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class).iterator();
        Map<String,Long> jvmTotalCapacity = new ConcurrentHashMap<>();
        while(var2.hasNext()){
            BufferPoolMXBean bufferPoolBean = (BufferPoolMXBean)var2.next();
            jvmTotalCapacity.put(bufferPoolBean.getName(), bufferPoolBean.getTotalCapacity());
            //System.out.println(bufferPoolBean.getName() + " " + bufferPoolBean.getTotalCapacity());
        }
        return jvmTotalCapacity;
    }

    public Map<String,Long> jvmMemoryUsed(){
        Iterator var2 = ManagementFactory.getPlatformMXBeans(MemoryPoolMXBean.class).iterator();
        Map<String,Long> jvmMemoryUsed = new ConcurrentHashMap<>();
        while(var2.hasNext()){
            MemoryPoolMXBean memoryPoolMXBean = (MemoryPoolMXBean) var2.next();
            String area = MemoryType.HEAP.equals(memoryPoolMXBean.getType()) ? "heap" : "nonheap";
            MemoryUsage memoryUsage = memoryPoolMXBean.getUsage();
            jvmMemoryUsed.put(memoryPoolMXBean.getName() + " " + area,memoryUsage.getUsed());
            System.out.println(memoryPoolMXBean.getName() + " " + area + " " + memoryUsage.getUsed());
        }
        return jvmMemoryUsed;
    }

    public Map<String,Long> jvmMemoryCommitted(){
        Iterator var2 = ManagementFactory.getPlatformMXBeans(MemoryPoolMXBean.class).iterator();
        Map<String,Long> jvmMemoryCommitted = new ConcurrentHashMap<>();
        while(var2.hasNext()){
            MemoryPoolMXBean memoryPoolMXBean = (MemoryPoolMXBean) var2.next();
            String area = MemoryType.HEAP.equals(memoryPoolMXBean.getType()) ? "heap" : "nonheap";
            MemoryUsage memoryUsage = memoryPoolMXBean.getUsage();
            jvmMemoryCommitted.put(memoryPoolMXBean.getName() + " " + area,memoryUsage.getCommitted());
        }
        return jvmMemoryCommitted;
    }

    public Map<String,Long> jvmMemoryMax(){
        Iterator var2 = ManagementFactory.getPlatformMXBeans(MemoryPoolMXBean.class).iterator();
        Map<String,Long> jvmMemoryMax = new ConcurrentHashMap<>();
        while(var2.hasNext()){
            MemoryPoolMXBean memoryPoolMXBean = (MemoryPoolMXBean) var2.next();
            String area = MemoryType.HEAP.equals(memoryPoolMXBean.getType()) ? "heap" : "nonheap";
            MemoryUsage memoryUsage = memoryPoolMXBean.getUsage();
            jvmMemoryMax.put(memoryPoolMXBean.getName() + " " + area,memoryUsage.getMax());
        }
        return jvmMemoryMax;
    }

    public double memoryLoad(){

        //OperatingSystemMXBean operator = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        //double totalVirtualMemory = operator.getTotalPhysicalMemorySize();
        //double freePhysicalMemorySize = operator.getFreePhysicalMemorySize();
        long totalPhysicalMemory = this.invokeLong(this.getTotalPhysicalMemorySize);
        long totalFreePhysicalMemorySize = this.invokeLong(this.getFreePhysicalMemorySize);
        //System.out.println(1.0*totalPhysicalMemory/1024/1024/1024 + " "+ 1.0*totalFreePhysicalMemorySize/1024/1024/1024);
        double value = 1.0*totalFreePhysicalMemorySize/totalPhysicalMemory;
        return (1-value)*100;
    }

    public int systemCpuCount(){
        Runtime runtime = Runtime.getRuntime();
        return runtime.availableProcessors();
    }

    //The sum of the number of runnable entities queued to available processors and the number of runnable entities running on the available processors averaged over a period of time
    public double systemLoadAverage1m(){
        return this.operatingSystemBean.getSystemLoadAverage();
    }

    public double systemCpuUsage(){
        if(this.systemCpuUsage != null){
            return this.invoke(this.systemCpuUsage);
        }
        return Double.NaN;
    }

    public double processCpuUsage(){

        if(this.processCpuUsage != null){
            return this.invoke(this.processCpuUsage);
        }
        return Double.NaN;
    }

    public int jvmThreadsPeak(){
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        return threadBean.getPeakThreadCount();
    }

    public int jvmThreadsDaemon(){
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        return threadBean.getDaemonThreadCount();
    }

    public int jvmThreadsLive(){
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        return threadBean.getThreadCount();
        //return ManagementFactory.getThreadMXBean().getThreadCount();
    }

    private Method detectMethod(String name) {
        Objects.requireNonNull(name);
        if (this.operatingSystemBeanClass == null) {
            return null;
        } else {
            try {
                this.operatingSystemBeanClass.cast(this.operatingSystemBean);
                return this.operatingSystemBeanClass.getDeclaredMethod(name);
            } catch (NoSuchMethodException | SecurityException | ClassCastException var3) {
                return null;
            }
        }
    }


    private Class<?> getFirstClassFound(List<String> classNames) {
        Iterator var2 = classNames.iterator();

        while(var2.hasNext()) {
            String className = (String)var2.next();

            try {
                return Class.forName(className);
            } catch (ClassNotFoundException var5) {
            }
        }
        return null;
    }

    private Class<?> getSecondClassFound(List<String> classNames){
        try{
            return Class.forName((String)classNames.get(1));
        }catch (ClassNotFoundException e){
            e.printStackTrace();
        }
        return null;
    }

    private double invoke(Method method) {
        try {
            return method != null ? (Double)method.invoke(this.operatingSystemBean) : Double.NaN;
        } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException var3) {
            return Double.NaN;
        }
    }

    private long invokeLong(Method method){
        try {
            return method != null ? (long)method.invoke(this.operatingSystemBean) : 0;
        } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException var3) {
            return 0;
        }
    }
}

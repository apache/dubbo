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
package com.alibaba.dubbo.monitor.simple;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.RectangleInsets;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.monitor.MonitorService;

/**
 * SimpleMonitorService
 * 
 * @author william.liangf
 */
public class SimpleMonitorService implements MonitorService {
    
    private static final String[] types = {SUCCESS, FAILURE, ELAPSED, CONCURRENT, MAX_ELAPSED, MAX_CONCURRENT};
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleMonitorService.class);

    // 定时任务执行器
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("DubboRegistryReconnectTimer", true));

    // 图表绘制定时器
    private ScheduledFuture<?> chartFuture;

    private String statisticsDirectory = "statistics";

    private String chartsDirectory = "charts";
    
    private static SimpleMonitorService INSTANCE = null;

    public static SimpleMonitorService getInstance() {
        return INSTANCE;
    }

    public String getStatisticsDirectory() {
        return statisticsDirectory;
    }
    
    public void setStatisticsDirectory(String statistics) {
        if (statistics != null) {
            this.statisticsDirectory = statistics;
        }
    }

    public String getChartsDirectory() {
        return chartsDirectory;
    }

    public void setChartsDirectory(String charts) {
        this.chartsDirectory = charts;
    }
    
    public SimpleMonitorService() {
        chartFuture = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                try {
                    draw(); // 绘制图表
                } catch (Throwable t) { // 防御性容错
                    logger.error("Unexpected error occur at reconnect, cause: " + t.getMessage(), t);
                }
            }
        }, 1, 1, TimeUnit.MINUTES);
        INSTANCE = this;
    }

    public void close() {
        chartFuture.cancel(true);
    }

    private void draw() {
        File rootDir = new File(statisticsDirectory);
        if (! rootDir.exists()) {
            return;
        }
        File[] dateDirs = rootDir.listFiles();
        for (File dateDir : dateDirs) {
            File[] serviceDirs = dateDir.listFiles();
            for (File serviceDir : serviceDirs) {
                File[] methodDirs = serviceDir.listFiles();
                for (File methodDir : methodDirs) {
                    String methodUri = chartsDirectory + "/" + dateDir.getName() + "/" + serviceDir.getName() + "/" + methodDir.getName();
                    
                    File successFile = new File(methodUri + "/" + SUCCESS + ".png");
                    long successModified = successFile.lastModified();
                    boolean successChanged = false;
                    Map<String, long[]> successData = new HashMap<String, long[]>();
                    long[] successSummary = new long[4];
                    
                    File elapsedFile = new File(methodUri + "/" + ELAPSED + ".png");
                    long elapsedModified = elapsedFile.lastModified();
                    boolean elapsedChanged = false;
                    Map<String, long[]> elapsedData = new HashMap<String, long[]>();
                    long[] elapsedSummary = new long[4];
                    long elapsedMax = 0;
                    
                    File[] consumerDirs = methodDir.listFiles();
                    for (File consumerDir : consumerDirs) {
                        File[] providerDirs = consumerDir.listFiles();
                        for (File providerDir : providerDirs) {
                            File consumerSuccessFile = new File(providerDir, CONSUMER + "." + SUCCESS);
                            File providerSuccessFile = new File(providerDir, PROVIDER + "." + SUCCESS);
                            appendData(new File[] {consumerSuccessFile, providerSuccessFile}, successData, successSummary);
                            if (consumerSuccessFile.lastModified() > successModified 
                                    || providerSuccessFile.lastModified() > successModified) {
                                successChanged = true;
                            }
                            
                            File consumerElapsedFile = new File(providerDir, CONSUMER + "." + ELAPSED);
                            File providerElapsedFile = new File(providerDir, PROVIDER + "." + ELAPSED);
                            appendData(new File[] {consumerElapsedFile, providerElapsedFile}, elapsedData, elapsedSummary);
                            elapsedMax = Math.max(elapsedMax, CountUtils.max(new File(providerDir, CONSUMER + "." + MAX_ELAPSED)));
                            elapsedMax = Math.max(elapsedMax, CountUtils.max(new File(providerDir, PROVIDER + "." + MAX_ELAPSED)));
                            if (consumerElapsedFile.lastModified() > elapsedModified 
                                    || providerElapsedFile.lastModified() > elapsedModified) {
                                elapsedChanged = true;
                            }
                        }
                    }
                    if (elapsedChanged) {
                        divData(elapsedData, successData);
                        elapsedSummary[0] = elapsedMax;
                        elapsedSummary[1] = -1;
                        elapsedSummary[2] = successSummary[3] == 0 ? 0 : elapsedSummary[3] / successSummary[3];
                        elapsedSummary[3] = -1;
                        createChart(ELAPSED + "(ms)", dateDir.getName(), new String[] {CONSUMER, PROVIDER}, elapsedData, elapsedSummary, elapsedFile.getAbsolutePath());
                    }
                    if (successChanged) {
                        divData(successData, 60);
                        successSummary[0] = successSummary[0] / 60;
                        successSummary[1] = successSummary[1] / 60;
                        successSummary[2] = successSummary[2] / 60;
                        createChart(SUCCESS + "/s", dateDir.getName(), new String[] {CONSUMER, PROVIDER}, successData, successSummary, successFile.getAbsolutePath());
                    }
                }
            }
        }
    }
    
    private void divData(Map<String, long[]> successMap, long unit) {
        for (long[] success : successMap.values()) {
            for (int i = 0; i < success.length; i ++) {
                success[i] = success[i] / unit;
            }
        }
    }
    
    private void divData(Map<String, long[]> elapsedMap, Map<String, long[]> successMap) {
        for (Map.Entry<String, long[]> entry : elapsedMap.entrySet()) {
            long[] elapsed = entry.getValue();
            long[] success = successMap.get(entry.getKey());
            for (int i = 0; i < elapsed.length; i ++) {
                elapsed[i] = success[i] == 0 ? 0 : elapsed[i] / success[i];
            }
        }
    }

    private void appendData(File[] files, Map<String, long[]> data, long[] summary) {
        for (int i = 0; i < files.length; i ++) {
            File file = files[i];
            if (! file.exists()) {
                continue;
            }
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                try {
                    int t = 0;
                    String line;
                    while ((line = reader.readLine()) != null) {
                        int index = line.indexOf(" ");
                        if (index > 0) {
                            String key = line.substring(0, index).trim();
                            long value = Long.parseLong(line.substring(index + 1).trim());
                            long[] values = data.get(key);
                            if (values == null) {
                                values = new long[files.length];
                                data.put(key, values);
                            }
                            values[i] += value;
                            summary[0] = Math.max(summary[0], values[i]);
                            summary[1] = summary[1] == 0 ? values[i] : Math.min(summary[1], values[i]);
                            summary[3] += value;
                            t ++;
                        }
                    }
                    summary[2] = summary[3] / t;
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }
    
    private static void createChart(String key, String date, String[] types, Map<String, long[]> data, long[] summary, String path) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
        TimeSeriesCollection xydataset = new TimeSeriesCollection();
        for (int i = 0; i < types.length; i ++) {
            String type = types[i];
            TimeSeries timeseries = new TimeSeries(type);
            for (Map.Entry<String, long[]> entry : data.entrySet()) {
                try {
                    timeseries.add(new Minute(format.parse(date + entry.getKey())), entry.getValue()[i]);
                } catch (ParseException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            xydataset.addSeries(timeseries);
        }
        JFreeChart jfreechart = ChartFactory.createTimeSeriesChart(
                "max: " + summary[0] + (summary[1] >=0 ? ", min: " + summary[1] : "") + ", avg: " + summary[2] + (summary[3] >=0 ? ", sum: " + summary[3] : ""), toDisplayDate(date), key, xydataset, true, true, false);
        jfreechart.setBackgroundPaint(Color.WHITE);
        XYPlot xyplot = (XYPlot) jfreechart.getPlot();
        xyplot.setBackgroundPaint(Color.WHITE);
        xyplot.setDomainGridlinePaint(Color.WHITE);
        xyplot.setRangeGridlinePaint(Color.WHITE);
        xyplot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        DateAxis dateaxis = (DateAxis) xyplot.getDomainAxis();
        dateaxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
        BufferedImage image = jfreechart.createBufferedImage(500, 200);
        try {
            if (logger.isInfoEnabled()) {
                logger.info("write chart: " + path);
            }
            File methodChartFile = new File(path);
            File methodChartDir = methodChartFile.getParentFile();
            if (methodChartDir != null && ! methodChartDir.exists()) {
                methodChartDir.mkdirs();
            }
            FileOutputStream output = new FileOutputStream(methodChartFile);
            try {
                ImageIO.write(image, "png", output);
                output.flush();
            } finally {
                output.close();
            }
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
    }
    
    private static String toDisplayDate(String date) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").format(new SimpleDateFormat("yyyyMMdd").parse(date));
        } catch (ParseException e) {
            return date;
        }
    }

    public void count(URL statistics) {
        try {
            Date now = new Date();
            String day = new SimpleDateFormat("yyyyMMdd").format(now);
            SimpleDateFormat format = new SimpleDateFormat("HHmm");
            for (String key : types) {
                try {
                    String type;
                    String consumer;
                    String provider;
                    if (statistics.hasParameter(PROVIDER)) {
                        type = PROVIDER;
                        consumer = statistics.getHost();
                        provider = statistics.getParameter(PROVIDER);
                        int i = provider.indexOf(':');
                        if (i > 0) {
                            provider = provider.substring(0, i);
                        }
                    } else {
                        type = CONSUMER;
                        consumer = statistics.getParameter(CONSUMER);
                        provider = statistics.getHost();
                    }
                    String filename = statisticsDirectory 
                            + "/" + day 
                            + "/" + statistics.getServiceName() 
                            + "/" + statistics.getParameter(METHOD) 
                            + "/" + consumer 
                            + "/" + provider 
                            + "/" + type + "." + key;
                    File file = new File(filename);
                    File dir = file.getParentFile();
                    if (dir != null && ! dir.exists()) {
                        dir.mkdirs();
                    }
                    FileWriter writer = new FileWriter(file, true);
                    try {
                        writer.write(format.format(now) + " " + statistics.getParameter(key, 0) + "\n");
                        writer.flush();
                    } finally {
                        writer.close();
                    }
                } catch (Throwable t) {
                    logger.error(t.getMessage(), t);
                }
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
    }

}
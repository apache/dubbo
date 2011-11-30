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
        }, 10, 20, TimeUnit.SECONDS);
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
                    File methodChartFile = new File(chartsDirectory + "/" + dateDir.getName() + "/" + serviceDir.getName() + "/" + methodDir.getName() + "/success.png");
                    File methodChartDir = methodChartFile.getParentFile();
                    if (methodChartDir != null && ! methodChartDir.exists()) {
                        methodChartDir.mkdirs();
                    }
                    long methodChartModified = methodChartFile.lastModified();
                    boolean changed = false;
                    Map<String, long[]> methodData = new HashMap<String, long[]>();
                    File[] consumerDirs = methodDir.listFiles();
                    for (File consumerDir : consumerDirs) {
                        File[] providerDirs = consumerDir.listFiles();
                        for (File providerDir : providerDirs) {
                            File consumerFile = new File(providerDir, CONSUMER + ".success");
                            File providerFile = new File(providerDir, PROVIDER + ".success");
                            appendData(new File[] {consumerFile, providerFile}, methodData);
                            if (consumerFile.lastModified() > methodChartModified || consumerFile.lastModified() > methodChartModified) {
                                changed = true;
                            }
                        }
                    }
                    if (changed) {
                        JFreeChart chart = createChart("Success", dateDir.getName(), new String[] {CONSUMER, PROVIDER}, methodData);
                        BufferedImage image = chart.createBufferedImage(500, 200);
                        try {
                            if (logger.isInfoEnabled()) {
                                logger.info("write chart: " + methodChartFile.getAbsolutePath());
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
                }
            }
        }
    }

    private void appendData(File[] files, Map<String, long[]> map) {
        for (int i = 0; i < files.length; i ++) {
            File file = files[i];
            if (! file.exists()) {
                continue;
            }
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        int index = line.indexOf(" ");
                        if (index > 0) {
                            String key = line.substring(0, index).trim();
                            long value = Long.parseLong(line.substring(index + 1).trim());
                            long[] values = map.get(key);
                            if (values == null) {
                                values = new long[files.length];
                                map.put(key, values);
                            }
                            values[i] += value;
                        }
                    }
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }
    
    private static JFreeChart createChart(String key, String date, String[] types, Map<String, long[]> data) {
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
        JFreeChart jfreechart = ChartFactory.createTimeSeriesChart("Max: 1, Min: 1, Avg: 1, Sum: 1", toDisplayDate(date), key, xydataset, true, true, false);
        jfreechart.setBackgroundPaint(Color.WHITE);
        XYPlot xyplot = (XYPlot) jfreechart.getPlot();
        xyplot.setBackgroundPaint(Color.WHITE);
        xyplot.setDomainGridlinePaint(Color.WHITE);
        xyplot.setRangeGridlinePaint(Color.WHITE);
        xyplot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        /*xyplot.setDomainCrosshairVisible(true);
        xyplot.setRangeCrosshairVisible(true);
        XYItemRenderer xyitemrenderer = xyplot.getRenderer();
        if (xyitemrenderer instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer xylineandshaperenderer = (XYLineAndShapeRenderer) xyitemrenderer;
            xylineandshaperenderer.setBaseShapesVisible(true);
            xylineandshaperenderer.setBaseShapesFilled(true);
        }*/
        DateAxis dateaxis = (DateAxis) xyplot.getDomainAxis();
        dateaxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
        return jfreechart;
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
                    String filename = statistics 
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
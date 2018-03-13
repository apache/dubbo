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
package com.alibaba.dubbo.monitor.simple;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.monitor.MonitorService;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * SimpleMonitorService
 */
public class SimpleMonitorService implements MonitorService {

    private static final Logger logger = LoggerFactory.getLogger(SimpleMonitorService.class);

    private static final String[] types = {SUCCESS, FAILURE, ELAPSED, CONCURRENT, MAX_ELAPSED, MAX_CONCURRENT};

    private static final String POISON_PROTOCOL = "poison";
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("DubboMonitorTimer", true));
    private final ScheduledFuture<?> chartFuture;
    private final Thread writeThread;
    private final BlockingQueue<URL> queue;
    private String statisticsDirectory = "statistics";
    private String chartsDirectory = "charts";
    private volatile boolean running = true;

    public SimpleMonitorService() {
        queue = new LinkedBlockingQueue<URL>(Integer.parseInt(ConfigUtils.getProperty("dubbo.monitor.queue", "100000")));
        writeThread = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    try {
                        write(); // write statistics
                    } catch (Throwable t) {
                        logger.error("Unexpected error occur at write stat log, cause: " + t.getMessage(), t);
                        try {
                            Thread.sleep(5000); // retry after 5 secs
                        } catch (Throwable t2) {
                        }
                    }
                }
            }
        });
        writeThread.setDaemon(true);
        writeThread.setName("DubboMonitorAsyncWriteLogThread");
        writeThread.start();
        chartFuture = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                try {
                    draw(); // draw chart
                } catch (Throwable t) {
                    logger.error("Unexpected error occur at draw stat chart, cause: " + t.getMessage(), t);
                }
            }
        }, 1, 300, TimeUnit.SECONDS);
        statisticsDirectory = ConfigUtils.getProperty("dubbo.statistics.directory");
        chartsDirectory = ConfigUtils.getProperty("dubbo.charts.directory");
    }

    private static void createChart(String key, String service, String method, String date, String[] types, Map<String, long[]> data, double[] summary, String path) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
        DecimalFormat numberFormat = new DecimalFormat("###,##0.##");
        TimeSeriesCollection xydataset = new TimeSeriesCollection();
        for (int i = 0; i < types.length; i++) {
            String type = types[i];
            TimeSeries timeseries = new TimeSeries(type);
            for (Map.Entry<String, long[]> entry : data.entrySet()) {
                try {
                    timeseries.add(new Minute(dateFormat.parse(date + entry.getKey())), entry.getValue()[i]);
                } catch (ParseException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            xydataset.addSeries(timeseries);
        }
        JFreeChart jfreechart = ChartFactory.createTimeSeriesChart(
                "max: " + numberFormat.format(summary[0]) + (summary[1] >= 0 ? " min: " + numberFormat.format(summary[1]) : "")
                        + " avg: " + numberFormat.format(summary[2]) + (summary[3] >= 0 ? " sum: " + numberFormat.format(summary[3]) : ""),
                toDisplayService(service) + "  " + method + "  " + toDisplayDate(date), key, xydataset, true, true, false);
        jfreechart.setBackgroundPaint(Color.WHITE);
        XYPlot xyplot = (XYPlot) jfreechart.getPlot();
        xyplot.setBackgroundPaint(Color.WHITE);
        xyplot.setDomainGridlinePaint(Color.GRAY);
        xyplot.setRangeGridlinePaint(Color.GRAY);
        xyplot.setDomainGridlinesVisible(true);
        xyplot.setRangeGridlinesVisible(true);
        DateAxis dateaxis = (DateAxis) xyplot.getDomainAxis();
        dateaxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
        BufferedImage image = jfreechart.createBufferedImage(600, 300);
        try {
            if (logger.isInfoEnabled()) {
                logger.info("write chart: " + path);
            }
            File methodChartFile = new File(path);
            File methodChartDir = methodChartFile.getParentFile();
            if (methodChartDir != null && !methodChartDir.exists()) {
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

    private static String toDisplayService(String service) {
        int i = service.lastIndexOf('.');
        if (i >= 0) {
            return service.substring(i + 1);
        }
        return service;
    }

    private static String toDisplayDate(String date) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").format(new SimpleDateFormat("yyyyMMdd").parse(date));
        } catch (ParseException e) {
            return date;
        }
    }

    public void close() {
        try {
            running = false;
            queue.offer(new URL(POISON_PROTOCOL, NetUtils.LOCALHOST, 0));
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
        try {
            chartFuture.cancel(true);
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    private void write() throws Exception {
        URL statistics = queue.take();
        if (POISON_PROTOCOL.equals(statistics.getProtocol())) {
            return;
        }
        String timestamp = statistics.getParameter(Constants.TIMESTAMP_KEY);
        Date now;
        if (timestamp == null || timestamp.length() == 0) {
            now = new Date();
        } else if (timestamp.length() == "yyyyMMddHHmmss".length()) {
            now = new SimpleDateFormat("yyyyMMddHHmmss").parse(timestamp);
        } else {
            now = new Date(Long.parseLong(timestamp));
        }
        String day = new SimpleDateFormat("yyyyMMdd").format(now);
        SimpleDateFormat format = new SimpleDateFormat("HHmm");
        for (String key : types) {
            try {
                String type;
                String consumer;
                String provider;
                if (statistics.hasParameter(PROVIDER)) {
                    type = CONSUMER;
                    consumer = statistics.getHost();
                    provider = statistics.getParameter(PROVIDER);
                    int i = provider.indexOf(':');
                    if (i > 0) {
                        provider = provider.substring(0, i);
                    }
                } else {
                    type = PROVIDER;
                    consumer = statistics.getParameter(CONSUMER);
                    int i = consumer == null ? -1 : consumer.indexOf(':');
                    if (i > 0) {
                        consumer = consumer.substring(0, i);
                    }
                    provider = statistics.getHost();
                }
                String filename = statisticsDirectory
                        + "/" + day
                        + "/" + statistics.getServiceInterface()
                        + "/" + statistics.getParameter(METHOD)
                        + "/" + consumer
                        + "/" + provider
                        + "/" + type + "." + key;
                File file = new File(filename);
                File dir = file.getParentFile();
                if (dir != null && !dir.exists()) {
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
    }

    private void draw() {
        File rootDir = new File(statisticsDirectory);
        if (!rootDir.exists()) {
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
                    double[] successSummary = new double[4];

                    File elapsedFile = new File(methodUri + "/" + ELAPSED + ".png");
                    long elapsedModified = elapsedFile.lastModified();
                    boolean elapsedChanged = false;
                    Map<String, long[]> elapsedData = new HashMap<String, long[]>();
                    double[] elapsedSummary = new double[4];
                    long elapsedMax = 0;

                    File[] consumerDirs = methodDir.listFiles();
                    for (File consumerDir : consumerDirs) {
                        File[] providerDirs = consumerDir.listFiles();
                        for (File providerDir : providerDirs) {
                            File consumerSuccessFile = new File(providerDir, CONSUMER + "." + SUCCESS);
                            File providerSuccessFile = new File(providerDir, PROVIDER + "." + SUCCESS);
                            appendData(new File[]{consumerSuccessFile, providerSuccessFile}, successData, successSummary);
                            if (consumerSuccessFile.lastModified() > successModified
                                    || providerSuccessFile.lastModified() > successModified) {
                                successChanged = true;
                            }

                            File consumerElapsedFile = new File(providerDir, CONSUMER + "." + ELAPSED);
                            File providerElapsedFile = new File(providerDir, PROVIDER + "." + ELAPSED);
                            appendData(new File[]{consumerElapsedFile, providerElapsedFile}, elapsedData, elapsedSummary);
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
                        createChart("ms/t", serviceDir.getName(), methodDir.getName(), dateDir.getName(), new String[]{CONSUMER, PROVIDER}, elapsedData, elapsedSummary, elapsedFile.getAbsolutePath());
                    }
                    if (successChanged) {
                        divData(successData, 60);
                        successSummary[0] = successSummary[0] / 60;
                        successSummary[1] = successSummary[1] / 60;
                        successSummary[2] = successSummary[2] / 60;
                        createChart("t/s", serviceDir.getName(), methodDir.getName(), dateDir.getName(), new String[]{CONSUMER, PROVIDER}, successData, successSummary, successFile.getAbsolutePath());
                    }
                }
            }
        }
    }

    private void divData(Map<String, long[]> successMap, long unit) {
        for (long[] success : successMap.values()) {
            for (int i = 0; i < success.length; i++) {
                success[i] = success[i] / unit;
            }
        }
    }

    private void divData(Map<String, long[]> elapsedMap, Map<String, long[]> successMap) {
        for (Map.Entry<String, long[]> entry : elapsedMap.entrySet()) {
            long[] elapsed = entry.getValue();
            long[] success = successMap.get(entry.getKey());
            for (int i = 0; i < elapsed.length; i++) {
                elapsed[i] = success[i] == 0 ? 0 : elapsed[i] / success[i];
            }
        }
    }

    private void appendData(File[] files, Map<String, long[]> data, double[] summary) {
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (!file.exists()) {
                continue;
            }
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                try {
                    int sum = 0;
                    int cnt = 0;
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
                            sum += value;
                            cnt++;
                        }
                    }
                    if (i == 0) {
                        summary[3] += sum;
                        summary[2] = (summary[2] + (sum / cnt)) / 2;
                    }
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    public void count(URL statistics) {
        collect(statistics);
    }

    public void collect(URL statistics) {
        queue.offer(statistics);
        if (logger.isInfoEnabled()) {
            logger.info("collect statistics: " + statistics);
        }
    }

    public List<URL> lookup(URL query) {
        // TODO Auto-generated method stub
        return null;
    }

}
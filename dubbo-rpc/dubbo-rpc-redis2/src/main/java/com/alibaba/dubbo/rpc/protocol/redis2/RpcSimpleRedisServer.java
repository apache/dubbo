package com.alibaba.dubbo.rpc.protocol.redis2;

import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.config.spring.util.SpringUtil;
import io.netty.buffer.ByteBuf;
import redis.netty4.BulkReply;
import redis.netty4.Reply;
import redis.netty4.StatusReply;
import redis.server.netty.RedisException;
import redis.server.netty.SimpleRedisServer;
import redis.util.BytesKeyObjectMap;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by wuyu on 2017/2/8.
 */
public class RpcSimpleRedisServer extends SimpleRedisServer {

    private Set<String> serviceSet = new ConcurrentHashSet<>();


    @Override
    public BulkReply info(byte[] section) throws RedisException {
        BulkReply reply = super.info(section);
        String info = threadInfo() + datasourceInfo() + reply.asUTF8String();

        info = info + systemInfo();
        for (String key : serviceSet) {
            info = info + key + "\n";
        }
        return new BulkReply(info.getBytes());
    }


    @Override
    public StatusReply select(byte[] index0) throws RedisException {
        byte b = index0[0];
        if ((int) b > 48) {
            return new StatusReply("") {
                @Override
                public void write(ByteBuf os) throws IOException {
                    os.writeByte('-');
                    os.writeBytes("ERR invalid DB index".getBytes());
                    os.writeBytes(Reply.CRLF);
                }
            };
        }
        return StatusReply.OK;
    }


    public void addServiceKey(String key) {
        serviceSet.add(key);
    }

    public static StringBuffer systemInfo() {
        Runtime r = Runtime.getRuntime();
        BigDecimal divide = new BigDecimal((r.totalMemory() - r.freeMemory())).divide(new BigDecimal(r.totalMemory()),
                2, 4);
        StringBuffer sb = new StringBuffer();
        sb.append("JVMInfo\n");
        sb.append("jvmTotal:").append(r.totalMemory() / 1000).append("\n");// java总内存
        sb.append("jvmUse:").append(r.totalMemory() - r.freeMemory()).append("\n");
        sb.append("jvmFree:").append(r.freeMemory()).append("\n");
        sb.append("jvmUsage:").append(divide.doubleValue()).append("\n");
        sb.append("cpu:").append(r.availableProcessors()).append("\n");
        sb.append("user.home:").append(System.getProperty("user.home")).append("\n");
        sb.append("user.timezone:").append(System.getProperty("user.timezone")).append("\n");
        sb.append("file.encoding:").append(System.getProperty("file.encoding")).append("\n");
        sb.append("user.name:").append(System.getProperty("user.name")).append("\n");

        File[] files = File.listRoots();
        sb.append("DiskInfo\n");
        for (File f : files) {
            Map<String, Object> diskInfo = new LinkedHashMap<>();
            diskInfo.put("path", f.getPath());
            diskInfo.put("freeSpace", f.getFreeSpace());
            diskInfo.put("usableSpace", f.getUsableSpace());
            diskInfo.put("totalSpace", f.getTotalSpace());
            sb.append(diskInfo.toString()).append("\n");
        }
        return sb;
    }

    public static String threadInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("ThreadInfo\n");
        long[] allThreadIds = ManagementFactory.getThreadMXBean().getAllThreadIds();
        for (long l : allThreadIds) {
            ThreadInfo threadInfo = ManagementFactory.getThreadMXBean().getThreadInfo(l);
            sb.append(threadInfo.toString());
        }
        return sb.toString();
    }

    public static String datasourceInfo() {
        Set<DataSource> beans = SpringUtil.getBeans(DataSource.class);
        if (beans.size() == 0) {
            return "";
        }
        StringBuffer datasourceInfo = new StringBuffer("DatasourceInfo\n");
        int i = 1;
        for (DataSource dataSource : beans) {
            datasourceInfo.append(i).append("\n");
            Method[] methods = dataSource.getClass().getMethods();
            for (Method method : methods) {
                try {
                    String methodName = method.getName();
                    if (methodName.equals("getMaxActive")) {
                        datasourceInfo.append("maxActive:").append(method.invoke(dataSource));
                    }
                    if (methodName.equals("getMaxIdle")) {
                        datasourceInfo.append("maxIdle:").append(method.invoke(dataSource));
                    }

                    if (methodName.equals("getMinIdle")) {
                        datasourceInfo.append("minIdle:").append(method.invoke(dataSource));

                    }

                    if (methodName.equals("getMaxWait")) {
                        datasourceInfo.append("maxWait:").append(method.invoke(dataSource));

                    }

                    if (methodName.equals("maxActive")) {
                        datasourceInfo.append("maxActive:").append(method.invoke(dataSource));
                    }
                    if (methodName.equals("getExecuteCount")) {
                        datasourceInfo.append("executeCount:").append(method.invoke(dataSource));
                    }
                    if (methodName.equals("getInitialSize")) {
                        datasourceInfo.append("initialSize:").append(method.invoke(dataSource));
                    }
                    if (methodName.equals("getTimeBetweenConnectErrorMillis")) {
                        datasourceInfo.append("timeBetweenConnectErrorMillis:").append(method.invoke(dataSource));
                    }
                    if (methodName.equals("getTimeBetweenEvictionRunsMillis()")) {
                        datasourceInfo.append("timeBetweenEvictionRunsMillis:").append(method.invoke(dataSource));
                    }
                    if (methodName.equals("getMinEvictableIdleTimeMillis()")) {
                        datasourceInfo.append("minEvictableIdleTimeMillis:").append(method.invoke(dataSource));
                    }

                } catch (Exception ignored) {
                }

            }
        }
        return datasourceInfo.toString();
    }
}

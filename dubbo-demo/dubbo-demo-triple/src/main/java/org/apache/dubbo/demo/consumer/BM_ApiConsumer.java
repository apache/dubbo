package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.demo.GreeterService;
import org.apache.dubbo.demo.hello.HelloReply;
import org.apache.dubbo.demo.hello.HelloRequest;

import org.springframework.util.StopWatch;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class BM_ApiConsumer {
    public static void main(String[] args) throws InterruptedException, IOException {
        ReferenceConfig<GreeterService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(GreeterService.class);
        referenceConfig.setCheck(false);
        referenceConfig.setProtocol(CommonConstants.TRIPLE);
        referenceConfig.setLazy(true);
        referenceConfig.setTimeout(1000 * 60 * 30);
        referenceConfig.setRetries(0);

        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap
                .application(new ApplicationConfig("dubbo-demo-triple-api-consumer"))
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .protocol(new ProtocolConfig(CommonConstants.TRIPLE, -1))
                .reference(referenceConfig)
                .start();

        GreeterService greeterService = referenceConfig.get();
        System.out.println("dubbo referenceConfig started");

        int i=0;
        try {
            HelloRequest request = HelloRequest.newBuilder().setName("triple").build();
            final int NUM_WARMUP = 10;
            final int NUM_TEST = 100;

            warmUp(NUM_WARMUP, greeterService, request);

            long[] durations = new long[NUM_TEST];
            StopWatch stopWatch = new StopWatch();
            for (; i<NUM_TEST; ++i) {
                stopWatch.start();
                greeterService.sayHello(request);
                stopWatch.stop();

                long duration = stopWatch.getLastTaskTimeMillis();
                durations[i] = duration;
            }
            System.out.println("测试完毕："+NUM_TEST);

            printDurations(durations);
        } catch (Throwable t) {
            System.out.println("Error occurs when i="+i);
            t.printStackTrace();
        }
        System.in.read();
    }

    private static void warmUp(final int NUM_WARMUP, GreeterService greeterService, HelloRequest request) {
        for (int i=0; i<NUM_WARMUP; ++i) {
            greeterService.sayHello(request);
        }
        System.out.println("预热完毕："+NUM_WARMUP);
    }

    private static void printDurations(long[] durations) {
        /*for (int i=0; i<durations.length; ++i) {
            System.out.println(i+": "+durations[i]+" ms");
        }*/

        final String csvFile = "http3.csv";
        try (FileWriter writer = new FileWriter(csvFile)) {
            for (long duration : durations) {
                writer.write(String.valueOf(duration));
                writer.write(System.lineSeparator()); // 换行符
            }
            System.out.println("数据已成功写入到 " + csvFile);
        } catch (IOException e) {
            System.err.println("写入数据到 CSV 文件时出现错误：" + e.getMessage());
            e.printStackTrace();
        }
    }
}

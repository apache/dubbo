# Prometheus Metrics Reporter

This is about how to use Prometheus to collector metrics from Apache Dubbo.

## Usage

As far as we know, Prometheus support both pull and push mode to collector metrics, Dubbo provided both way to let you 
choose which one you prefer.

### Pull Mode

For pull mode, you can add this config to your code.

```java
@Configuration
public class DubboConfiguration {
    @Bean
    public MetricsConfig metricsConfig() {
        MetricsConfig metricsConfig = new MetricsConfig();
        metricsConfig.setProtocol(MetricsConstants.PROTOCOL_PROMETHEUS);
        
        // collect jvm metrics
        metricsConfig.setEnableJvmMetrics(true);

        // add this config if you want to aggregate metrics in local machine.
        // or you can aggregate by PromQL
        AggregationConfig aggregationConfig = new AggregationConfig();
        aggregationConfig.setEnabled(true);
        aggregationConfig.setTimeWindowSeconds(120);
        aggregationConfig.setBucketNum(5);
        metricsConfig.setAggregation(aggregationConfig);

        PrometheusConfig prometheusConfig = new PrometheusConfig();
        PrometheusConfig.Exporter exporter = new PrometheusConfig.Exporter();
        // this must be true if you are using exporter.
        exporter.setEnabled(true);

        // this will expose a httpserver to public, and the metrics url is http://localhost:20888/metrics.
        exporter.setMetricsPath("/metrics");
        exporter.setMetricsPort(20888);

        prometheusConfig.setExporter(exporter);
        metricsConfig.setPrometheus(prometheusConfig);

        return metricsConfig;
    }
}
```

Or if you prefer xml config.

```xml
<dubbo:metrics protocol="prometheus"  enable-jvm-metrics="true">
    <dubbo:prometheus-exporter enabled="true" metrics-port="20888" metrics-path="/metrics" />
</dubbo:metrics>
```

After set, add scrape config to your Prometheus config file like this.

```yaml
scrape_configs:
  - job_name: 'dubbo'
    scrape_interval: 15s
    metrics_path: '/metrics'
    static_configs:
      - targets: [ "localhost:20888" ]
```

### Push Mode

For pull mode, you can add this config to your code.

```java
@Configuration
public class DubboConfiguration {
    @Bean
    public MetricsConfig metricsConfig() {
        MetricsConfig metricsConfig = new MetricsConfig();
        metricsConfig.setProtocol(MetricsConstants.PROTOCOL_PROMETHEUS);

        // collect jvm metrics
        metricsConfig.setEnableJvmMetrics(true);

        // add this config if you want to aggregate metrics in local machine.
        // or you can aggregate by PromQL
        AggregationConfig aggregationConfig = new AggregationConfig();
        aggregationConfig.setEnabled(true);
        aggregationConfig.setTimeWindowSeconds(120);
        aggregationConfig.setBucketNum(5);
        metricsConfig.setAggregation(aggregationConfig);

        PrometheusConfig prometheusConfig = new PrometheusConfig();
        PrometheusConfig.Pushgateway pushgateway = new PrometheusConfig.Pushgateway();
        // this must be true if you are using pushgateway.
        pushgateway.setEnabled(true);
        // your prometheus push gateway address
        pushgateway.setBaseUrl("localhost:9091");
        // job name display in prometheus
        pushgateway.setJob("job");
        // your credential
        pushgateway.setUsername("username");
        pushgateway.setPassword("password");
        // push interval, this means push metrics to pushgateway every 15 seconds
        pushgateway.setPushInterval(15);


        prometheusConfig.setPushgateway(pushgateway);
        metricsConfig.setPrometheus(prometheusConfig);

        return metricsConfig;
    }
}
```

Or if you prefer xml config.

```xml
<dubbo:metrics protocol="prometheus"  enable-jvm-metrics="true">
    <dubbo:prometheus-pushgateway enabled="true" base-url="localhost:9091" push-interval="30" username="username" password="password" job="job" />
</dubbo:metrics>
```

This will auto push metrics to your prometheus pushgateway server.
And you should add pushgateway to your scrape config.

```yaml
scrape_configs:
  - job_name: 'Pushgateway'
    honor_labels: true
    static_configs:
      - targets: [ "localhost:9091" ]
```

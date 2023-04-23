package org.apache.dubbo.metrics;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.MetricsPlaceValue;

import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_CODEC_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_LIMIT;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_NETWORK_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_PROCESSING;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_SERVICE_UNAVAILABLE_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_SUCCEED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_TIMEOUT;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_TOTAL_FAILED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUEST_BUSINESS_FAILED;

public interface DefaultConstants {

    String INVOCATION = "metric_filter_invocation";

    String INVOCATION_SIDE = "metric_filter_side";

    String METRIC_FILTER_EVENT = "metric_filter_event";

    String METRIC_THROWABLE = "metric_filter_throwable";

    List<MetricsKeyWrapper> SERVICE_LEVEL_KEYS = Arrays.asList(
        new MetricsKeyWrapper(METRIC_REQUESTS, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_PROCESSING, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_PROCESSING, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_SUCCEED, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_SUCCEED, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUEST_BUSINESS_FAILED, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUEST_BUSINESS_FAILED, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_TIMEOUT, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_TIMEOUT, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_LIMIT, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_LIMIT, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_FAILED, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_FAILED, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_TOTAL_FAILED, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_TOTAL_FAILED, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_NETWORK_FAILED, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_NETWORK_FAILED, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_SERVICE_UNAVAILABLE_FAILED, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_SERVICE_UNAVAILABLE_FAILED, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_CODEC_FAILED, MetricsPlaceValue.of(CommonConstants.PROVIDER, MetricsLevel.SERVICE)),
        new MetricsKeyWrapper(METRIC_REQUESTS_CODEC_FAILED, MetricsPlaceValue.of(CommonConstants.CONSUMER, MetricsLevel.SERVICE))
        );
}

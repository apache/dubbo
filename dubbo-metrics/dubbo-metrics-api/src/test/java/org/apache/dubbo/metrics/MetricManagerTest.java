package org.apache.dubbo.metrics;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author wangtao 2016-07-09 08:43.
 */
public class MetricManagerTest {

    @Test
    public void testNOPMetricManager() {
        Assert.assertTrue(MetricManager.getIMetricManager() instanceof NOPMetricManager);
    }

    @Test
    public void testNOPCompass() {
        Compass compass = MetricManager.getCompass("test", MetricName.build("test"));
        compass.record(10, "success");

        Assert.assertEquals(0, compass.getCountAndRtPerCategory().size());
        Assert.assertEquals(0, compass.getMethodCountPerCategory().size());
        Assert.assertEquals(0, compass.getMethodRtPerCategory().size());
    }

    @Test
    public void testNopCounter() {
        Counter counter = MetricManager.getCounter("test", MetricName.build("test2"));
        counter.inc();
        Assert.assertEquals(0, counter.getCount());
    }

    @Test
    public void testBucketCounter() {
        BucketCounter bc = MetricManager.getBucketCounters("test", MetricName.build("test3"));
        bc.update();
        Assert.assertEquals(0, bc.getBucketInterval());
        Assert.assertEquals(0, bc.getBucketCounts().size());
    }
}

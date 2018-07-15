package org.apache.dubbo.metrics;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;


/**
 * @author wangtao 2016-07-08 17:56.
 */
public class MetricNameTest {

    @Test
    public void testEmpty() {
        Assert.assertEquals(MetricName.EMPTY.getTags(), MetricName.EMPTY_TAGS);
        Assert.assertNull(MetricName.EMPTY.getKey());
        Assert.assertEquals(new MetricName().getTags(), MetricName.EMPTY_TAGS);

        Assert.assertEquals(MetricName.EMPTY, new MetricName());
        Assert.assertEquals(MetricName.build(), MetricName.EMPTY);
        Assert.assertEquals(MetricName.EMPTY.resolve(null), MetricName.EMPTY);
    }

    @Test
    public void testEmptyResolve() {
        final MetricName name = new MetricName();
        Assert.assertEquals(name.resolve("foo"), new MetricName("foo"));
    }

    @Test
    public void testResolveToEmpty() {
        final MetricName name = new MetricName("foo");
        Assert.assertEquals(name.resolve(null), new MetricName("foo"));
    }

    @Test
    public void testResolve() {
        final MetricName name = new MetricName("foo");
        Assert.assertEquals(name.resolve("bar"), new MetricName("foo.bar"));
    }

    @Test
    public void testResolveWithTags() {
        final MetricName name = new MetricName("foo").tag("key", "value");
        Assert.assertEquals(name.resolve("bar"), new MetricName("foo.bar").tag("key", "value"));
    }

    @Test
    public void testResolveWithoutTags() {
        final MetricName name = new MetricName("foo").tag("key", "value");
        Assert.assertEquals(name.resolve("bar", false), new MetricName("foo.bar"));
    }

    @Test
    public void testResolveBothEmpty() {
        final MetricName name = new MetricName(null);
        Assert.assertEquals(name.resolve(null), new MetricName());
    }

    @Test
    public void testAddTagsVarious() {
        final Map<String, String> refTags = new HashMap<String, String>();
        refTags.put("foo", "bar");
        final MetricName test = MetricName.EMPTY.tag("foo", "bar");
        final MetricName test2 = MetricName.EMPTY.tag(refTags);

        Assert.assertEquals(test, new MetricName(null, refTags));
        Assert.assertEquals(test.getTags(), refTags);

        Assert.assertEquals(test2, new MetricName(null, refTags));
        Assert.assertEquals(test2.getTags(), refTags);
    }

    @Test
    public void testTaggedMoreArguments() {
        final Map<String, String> refTags = new HashMap<String, String>();
        refTags.put("foo", "bar");
        refTags.put("baz", "biz");
        Assert.assertEquals(MetricName.EMPTY.tag("foo", "bar", "baz", "biz").getTags(), refTags);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testTaggedNotPairs() {
        MetricName.EMPTY.tag("foo");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testTaggedNotPairs2() {
        MetricName.EMPTY.tag("foo", "bar", "baz");
    }

    @Test
    public void testCompareTo() {
        final MetricName a = MetricName.EMPTY.tag("foo", "bar");
        final MetricName b = MetricName.EMPTY.tag("foo", "baz");

        Assert.assertTrue(a.compareTo(b) < 0);
        Assert.assertTrue(b.compareTo(a) > 0);
        Assert.assertTrue(b.compareTo(b) == 0);
        Assert.assertTrue(b.resolve("key").compareTo(b) < 0);
        Assert.assertTrue(b.compareTo(b.resolve("key")) > 0);
    }

    @Test
    public void testTaggedWithLevel() {
        MetricName name = MetricName.build("test").level(MetricLevel.CRITICAL);
        MetricName tagged = name.tag("foo", "bar");
        Assert.assertEquals(tagged.getMetricLevel(), MetricLevel.CRITICAL);
    }

    @Test
    public void testJoinWithLevel() {
        MetricName name = MetricName.build("test").level(MetricLevel.CRITICAL);
        MetricName tagged = MetricName.join(name, MetricName.build("abc"));
        Assert.assertEquals(tagged.getMetricLevel(), MetricLevel.CRITICAL);
    }

    @Test
    public void testResolveWithLevel() {
        final MetricName name = new MetricName("foo").level(MetricLevel.CRITICAL).tag("key", "value");
        Assert.assertEquals(name.resolve("bar"), new MetricName("foo.bar").tag("key", "value").level(MetricLevel.CRITICAL));
    }
}

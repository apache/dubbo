package org.apache.dubbo.xds.resource_new.common;

public final class Range {

    private final long start;

    private final long end;

    public Range(
            long start, long end) {
        this.start = start;
        this.end = end;
    }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    @Override
    public String toString() {
        return "Range{" + "start=" + start + ", " + "end=" + end + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Range) {
            Range that = (Range) o;
            return this.start == that.start() && this.end == that.end();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= (int) ((start >>> 32) ^ start);
        h$ *= 1000003;
        h$ ^= (int) ((end >>> 32) ^ end);
        return h$;
    }

}

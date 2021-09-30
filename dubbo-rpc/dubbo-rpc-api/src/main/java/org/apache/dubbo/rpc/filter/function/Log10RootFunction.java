package org.apache.dubbo.rpc.filter.function;

import java.util.function.Function;
import java.util.stream.IntStream;

public final class Log10RootFunction implements Function<Integer, Integer> {
    static final int[] lookup = new int[1000];

    static {
        IntStream.range(0, 1000).forEach(i -> lookup[i] = Math.max(1, (int)Math.log10(i)));
    }

    //单例
    private static final Log10RootFunction INSTANCE = new Log10RootFunction();

    /**
     * Create an instance of a function that returns : baseline + log10(limit)
     *
     * @param baseline
     * @return
     */
    public static Function<Integer, Integer> create(int baseline) {
        return INSTANCE.andThen(t -> t + baseline);
    }

    @Override
    public Integer apply(Integer t) {
        return t < 1000 ? lookup[t] : (int)Math.log10(t);
    }
}

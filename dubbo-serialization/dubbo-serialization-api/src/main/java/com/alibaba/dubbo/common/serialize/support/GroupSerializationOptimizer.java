package com.alibaba.dubbo.common.serialize.support;

import java.util.Set;
import java.util.regex.Pattern;

/**
 */
public interface GroupSerializationOptimizer extends SerializationOptimizer{

    Set<String> interfaces();

    Set<Pattern> interfaceExps();
}

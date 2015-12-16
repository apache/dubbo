package com.alibaba.dubbo.common.serialize.support;

import java.util.Collection;

/**
 * This class can be replaced with the contents in config file, but for now I think the class is easier to write
 *
 * @author lishen
 */
public interface SerializationOptimizer {

    Collection<Class> getSerializableClasses();
}

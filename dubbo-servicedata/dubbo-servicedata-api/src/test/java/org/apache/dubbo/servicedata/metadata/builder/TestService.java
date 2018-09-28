package org.apache.dubbo.servicedata.metadata.builder;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author ON 2018/9/28
 */
public interface TestService {

    void testWriteSimpleArray(String[] args);

    String[] testReadSimpleArray(int i);

    void testWriteComplexArray(String[] args, ComplexObject[] complexObjects);

    ComplexObject[] testReadComplexArray(int i);

    void testWriteSimpleCollection(List<String> args);

    List<Integer> testReadSimpleCollection(int i);

    void testWriteComplexCollection(List<Long> args, List<ComplexObject> complexObjects);

    Set<ComplexObject> testReadComplexCollection(int i);

    void testWriteSingleEnum(SingleEnum singleEnum);

    SingleEnum testReadSingleEnum(int i);

    void testWriteComplexEnum(ComplexEnum complexEnum);

    ComplexEnum testReadComplexEnum(int i);

    void testWriteSimpleMap(Map<String, Integer> args);

    Map<String, Integer> testReadSimpleMap(int i);

    void testWriteComplexMap(Map<String, String> arg, Map<String, ComplexObject> complexObjects);

    Map<String, ComplexObject> testReadComplexMap(int i);
}

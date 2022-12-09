package org.apache.dubbo.metadata;

import org.apache.dubbo.metadata.rest.PathMatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PathMatcherTest {

    @Test
    public void testPathMatcher() {
        PathMatcher pathMather = new PathMatcher("/a/b/c/{path1}/d/{path2}/e");

        PathMatcher pathMather1 = new PathMatcher("/a/b/c/1/d/2/e");
        Assertions.assertEquals(true, pathMather.equals(pathMather1));
    }
}

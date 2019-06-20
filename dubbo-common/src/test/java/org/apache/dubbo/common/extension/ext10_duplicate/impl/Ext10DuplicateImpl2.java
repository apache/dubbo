package org.apache.dubbo.common.extension.ext10_duplicate.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ext10_duplicate.Ext10Duplicate;

public class Ext10DuplicateImpl2 implements Ext10Duplicate {
    @Override
    public String echo(URL url, String s) {
        return "ext10DuplicateImpl2";
    }
}

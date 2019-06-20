package org.apache.dubbo.common.extension.ext10_duplicate;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;

@SPI
public interface Ext10Duplicate {
    String echo(URL url, String s);
}

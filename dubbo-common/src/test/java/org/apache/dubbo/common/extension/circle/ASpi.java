package org.apache.dubbo.common.extension.circle;

import org.apache.dubbo.common.extension.SPI;


@SPI
public interface ASpi {

    BSpi getBSpi();

    void setBSpi(BSpi BSpi);
}

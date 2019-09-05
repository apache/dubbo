package org.apache.dubbo.common.extension.circle;

import org.apache.dubbo.common.extension.SPI;


@SPI
public interface BSpi {

    ASpi getASpi();

    void setASpi(ASpi ASpi);
}

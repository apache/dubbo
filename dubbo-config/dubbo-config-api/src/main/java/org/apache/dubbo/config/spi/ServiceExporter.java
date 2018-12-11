package org.apache.dubbo.config.spi;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.config.ApplicationConfig;

/**
 * Created by 278076999@qq.com 2018/12/10 17:47
 * 暴露服务接口
 * @author <a href="mailto:278076999@qq.com">simple</a>
 */
@SPI("dubboServiceExporter")
public interface ServiceExporter {
     void doExport(String beanName, Object bean);
     /**
     void doExport(String beanName, Object bean,ApplicationConfig applicationConfig);
      **/
}

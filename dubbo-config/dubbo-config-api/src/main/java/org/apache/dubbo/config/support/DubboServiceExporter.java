package org.apache.dubbo.config.support;

import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.spi.ServiceExporter;

/**
 * Created by lvxiang@ganji.com 2018/12/10 18:14
 *
 * @author <a href="mailto:lvxiang@ganji.com">simple</a>
 */
public class DubboServiceExporter implements ServiceExporter {
    @Override
    public void doExport(String beanName, Object bean) {
        ((ServiceConfig)bean).export();
    }
}

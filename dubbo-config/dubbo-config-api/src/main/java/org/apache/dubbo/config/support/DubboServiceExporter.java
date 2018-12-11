package org.apache.dubbo.config.support;

import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.spi.ServiceExporter;

/**
 * Created by lvxiang2018/12/10 18:14
 *
 * @author <a href="mailto:278076999@qq.com">simple</a>
 */
public class DubboServiceExporter implements ServiceExporter {

    @Override
    public void doExport(String beanName, Object bean) {
        //todo 将Bean 暴露和引入的逻辑抽取为单独模块
        ((ServiceConfig)bean).export();
    }
}

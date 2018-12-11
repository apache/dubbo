package org.apache.dubbo.config.spi;
import org.apache.dubbo.common.extension.SPI;

/**
 * Created by 278076999@qq.com 2018/12/10 17:47
 * 引用远程服务接口
 * @author <a href="mailto:278076999@qq.com">simple</a>
 */
@SPI("dubboServiceImporter")
public interface ServiceImporter {
       void doImport(String beanName, Object bean);
}

package com.alibaba.dubbo.common.extensionloader;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.ext1.SimpleExt;
import org.apache.dubbo.common.extension.ext2.Ext2;
import org.junit.Test;

/**
 * @author luokai
 * @date 2018/4/22
 */
public class SPITest {
    //普通加载器
    @Test
    public void getExtensionLoadergetExtensionLoader() {
        SimpleExt ext = ExtensionLoader.getExtensionLoader(SimpleExt.class).getDefaultExtension();
        System.out.println(ext.bang(null, 1));
        System.out.println(ext.echo(null, "1"));
        System.out.println(ext.yell(null, "2"));

        SimpleExt ext2 = ExtensionLoader.getExtensionLoader(SimpleExt.class).getDefaultExtension();
        Ext2 Ext2 = ExtensionLoader.getExtensionLoader(Ext2.class).getDefaultExtension();
        //Ext2.echo(null, s     null);

        SimpleExt ext3 = ExtensionLoader.getExtensionLoader(SimpleExt.class).getAdaptiveExtension();

    }
}

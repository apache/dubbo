package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;

import java.util.Set;

@SPI(scope = ExtensionScope.FRAMEWORK)
public interface AllowClassNotifyListener {

    void notify(Set<String> prefixList);
}

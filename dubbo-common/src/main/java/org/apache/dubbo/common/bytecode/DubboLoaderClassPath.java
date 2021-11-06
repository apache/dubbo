package org.apache.dubbo.common.bytecode;

import javassist.LoaderClassPath;
import javassist.NotFoundException;

import java.io.InputStream;
import java.net.URL;

/**
 * Ensure javassist will load Dubbo's class from Dubbo's classLoader
 */
public class DubboLoaderClassPath extends LoaderClassPath {
    public DubboLoaderClassPath() {
        super(DubboLoaderClassPath.class.getClassLoader());
    }

    @Override
    public InputStream openClassfile(String classname) throws NotFoundException {
        if (!classname.startsWith("org.apache.dubbo")) {
            return null;
        }
        return super.openClassfile(classname);
    }

    @Override
    public URL find(String classname) {
        if (!classname.startsWith("org.apache.dubbo")) {
            return null;
        }
        return super.find(classname);
    }
}

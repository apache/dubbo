/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.extensionloader;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.extensionloader.activate.ActivateExt1;
import com.alibaba.dubbo.common.extensionloader.activate.impl.*;
import com.alibaba.dubbo.common.extensionloader.compatible.CompatibleExt;
import com.alibaba.dubbo.common.extensionloader.compatible.impl.CompatibleExtImpl1;
import com.alibaba.dubbo.common.extensionloader.compatible.impl.CompatibleExtImpl2;
import com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt;
import com.alibaba.dubbo.common.extensionloader.ext1.impl.SimpleExtImpl1;
import com.alibaba.dubbo.common.extensionloader.ext1.impl.SimpleExtImpl2;
import com.alibaba.dubbo.common.extensionloader.ext1.impl.SimpleExtImpl_ManualAdd;
import com.alibaba.dubbo.common.extensionloader.ext2.Ext2;
import com.alibaba.dubbo.common.extensionloader.ext6_wrap.WrappedExt;
import com.alibaba.dubbo.common.extensionloader.ext6_wrap.impl.Ext5Wrapper1;
import com.alibaba.dubbo.common.extensionloader.ext6_wrap.impl.Ext5Wrapper2;
import com.alibaba.dubbo.common.extensionloader.ext7.InitErrorExt;
import junit.framework.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * @author ding.lid
 */
public class ExtensionLoader_Compatible_Test {

    @Test
    public void test_getExtension() throws Exception {
        assertTrue(ExtensionLoader.getExtensionLoader(CompatibleExt.class).getExtension("impl1") instanceof CompatibleExtImpl1);
        assertTrue(ExtensionLoader.getExtensionLoader(CompatibleExt.class).getExtension("impl2") instanceof CompatibleExtImpl2);
    }
}
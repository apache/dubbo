/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.monitor.simple.pages;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.monitor.simple.RegistryContainer;
import com.alibaba.dubbo.registry.NotifyListener;

import java.util.List;

/**
 * UnsubscribePageHandler
 *
 * @author william.liangf
 */
public class UnsubscribePageHandler implements PageHandler {

    public Page handle(URL url) {
        String consumer = url.getParameterAndDecoded("consumer");
        if (consumer == null || consumer.length() == 0) {
            throw new IllegalArgumentException("Please input consumer parameter.");
        }
        URL consumerUrl = URL.valueOf(consumer);
        RegistryContainer.getInstance().getRegistry().unsubscribe(consumerUrl, NotifyListenerAdapter.NOTIFY_LISTENER);
        String parameter;
        if (url.hasParameter("service")) {
            parameter = "service=" + url.getParameter("service");
        } else if (url.hasParameter("host")) {
            parameter = "host=" + url.getParameter("host");
        } else if (url.hasParameter("application")) {
            parameter = "application=" + url.getParameter("application");
        } else {
            parameter = "service=" + consumerUrl.getServiceInterface();
        }
        return new Page("<script type=\"text/javascript\">window.location.href=\"consumers.html?" + parameter + "\";</script>");
    }

    private static class NotifyListenerAdapter implements NotifyListener {

        public static final NotifyListener NOTIFY_LISTENER = new NotifyListenerAdapter();

        private NotifyListenerAdapter() {
        }

        public void notify(List<URL> urls) {
        }

    }

}

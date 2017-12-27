/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.governance.web.governance.module.screen;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.governance.service.OverrideService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.Override;
import com.alibaba.dubbo.registry.common.domain.Provider;
import com.alibaba.dubbo.registry.common.route.OverrideUtils;
import com.alibaba.dubbo.registry.common.util.Tool;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>Providers.</p>
 * URI: <br>
 * GET /providers, show all providers<br>
 * GET /providers/add, show web form for add a static provider<br>
 * POST /provider/create, create a static provider, save form<br>
 * GET /providers/$id, show provider details<br>
 * GET /providers/$id/edit, web form for edit provider<br>
 * POST /providers/$id, update provider, save form<br>
 * GET /providers/$id/delete, delete a provider<br>
 * GET /providers/$id/tostatic, transfer to static<br>
 * GET /providers/$id/todynamic, transfer to dynamic<br>
 * GET /providers/$id/enable, enable a provider<br>
 * GET /providers/$id/disable, disable a provider<br>
 * GET /providers/$id/reconnect, reconnect<br>
 * GET /providers/$id/recover, recover<br>
 * <br>
 * GET /services/$service/providers, show all provider of a specific service<br>
 * GET /services/$service/providers/add, show web form for add a static provider<br>
 * POST /services/$service/providers, save a static provider<br>
 * GET /services/$service/providers/$id, show provider details<br>
 * GET /services/$service/providers/$id/edit, show web form for edit provider<br>
 * POST /services/$service/providers/$id, save changes of provider<br>
 * GET /services/$service/providers/$id/delete, delete provider<br>
 * GET /services/$service/providers/$id/tostatic, transfer to static<br>
 * GET /services/$service/providers/$id/todynamic, transfer to dynamic<br>
 * GET /services/$service/providers/$id/enable, enable<br>
 * GET /services/$service/providers/$id/disable, diable<br>
 * GET /services/$service/providers/$id/reconnect, reconnect<br>
 * GET /services/$service/providers/$id/recover, recover<br>
 *
 */
public class Providers extends Restful {

    @Autowired
    private ProviderService providerService;

    @Autowired
    private OverrideService overrideService;

    @Autowired
    private HttpServletResponse response;

    @Autowired
    private HttpServletRequest request;

    public void index(Provider provider, Map<String, Object> context) {
        String service = (String) context.get("service");
        String application = (String) context.get("application");
        String address = (String) context.get("address");

        String value = "";
        String separators = "....";

        List<Provider> providers = null;

        // service
        if (service != null && service.length() > 0) {
            providers = providerService.findByService(service);

            value = service + separators + request.getRequestURI();
        }
        // address
        else if (address != null && address.length() > 0) {
            providers = providerService.findByAddress(address);

            value = address + separators + request.getRequestURI();
        }
        // application
        else if (application != null && application.length() > 0) {
            providers = providerService.findByApplication(application);

            value = application + separators + request.getRequestURI();
        }
        // all
        else {
            providers = providerService.findAll();
        }

        context.put("providers", providers);
        context.put("serviceAppMap", getServiceAppMap(providers));

        // record search history to cookies
        setSearchHistroy(context, value);
    }

    /**
     *
     * Calculate the application list corresponding to each service, to facilitate the "repeat" prompt on service page
     * @param providers app services
     */
    private Map<String, Set<String>> getServiceAppMap(List<Provider> providers) {
        Map<String, Set<String>> serviceAppMap = new HashMap<String, Set<String>>();
        if (providers != null && providers.size() >= 0) {
            for (Provider provider : providers) {
                Set<String> appSet;
                String service = provider.getService();
                if (serviceAppMap.get(service) == null) {
                    appSet = new HashSet<String>();
                } else {
                    appSet = serviceAppMap.get(service);
                }
                appSet.add(provider.getApplication());
                serviceAppMap.put(service, appSet);
            }
        }
        return serviceAppMap;
    }

    /**
     * Record search history to cookies, steps:
     * Check whether the added record exists in the cookie, and if so, update the list order; if it does not exist, insert it to the front
     *
     * @param context
     * @param value
     */
    private void setSearchHistroy(Map<String, Object> context, String value) {
        // Analyze existing cookies
        String separatorsB = "\\.\\.\\.\\.\\.\\.";
        String newCookiev = value;
        Cookie[] cookies = request.getCookies();
        for (Cookie c : cookies) {
            if (c.getName().equals("HISTORY")) {
                String cookiev = c.getValue();
                String[] values = cookiev.split(separatorsB);
                int count = 1;
                for (String v : values) {
                    if (count <= 10) {
                        if (!value.equals(v)) {
                            newCookiev = newCookiev + separatorsB + v;
                        }
                    }
                    count++;
                }
                break;
            }
        }

        Cookie _cookie = new Cookie("HISTORY", newCookiev);
        _cookie.setMaxAge(60 * 60 * 24 * 7); // Set the cookie's lifetime to 30 minutes
        _cookie.setPath("/");
        response.addCookie(_cookie); // Write to client hard disk
    }

    public void show(Long id, Map<String, Object> context) {
        Provider provider = providerService.findProvider(id);
        if (provider != null && provider.isDynamic()) {
            List<Override> overrides = overrideService.findByServiceAndAddress(provider.getService(), provider.getAddress());
            OverrideUtils.setProviderOverrides(provider, overrides);
        }
        context.put("provider", provider);
    }

    /**
     * Load new service page, get all the service name
     *
     * @param context
     */
    public void add(Long id, Map<String, Object> context) {
        if (context.get("service") == null) {
            List<String> serviceList = Tool.sortSimpleName(new ArrayList<String>(providerService.findServices()));
            context.put("serviceList", serviceList);
        }
        if (id != null) {
            Provider p = providerService.findProvider(id);
            if (p != null) {
                context.put("provider", p);
                String parameters = p.getParameters();
                if (parameters != null && parameters.length() > 0) {
                    Map<String, String> map = StringUtils.parseQueryString(parameters);
                    map.put("timestamp", String.valueOf(System.currentTimeMillis()));
                    map.remove("pid");
                    p.setParameters(StringUtils.toQueryString(map));
                }
            }
        }
    }

    public void edit(Long id, Map<String, Object> context) {
        show(id, context);
    }

    public boolean create(Provider provider, Map<String, Object> context) {
        String service = provider.getService();
        if (!super.currentUser.hasServicePrivilege(service)) {
            context.put("message", getMessage("HaveNoServicePrivilege", service));
            return false;
        }
        if (provider.getParameters() == null) {
            String url = provider.getUrl();
            if (url != null) {
                int i = url.indexOf('?');
                if (i > 0) {
                    provider.setUrl(url.substring(0, i));
                    provider.setParameters(url.substring(i + 1));
                }
            }
        }
        provider.setDynamic(false); // Provider add through web page must be static
        providerService.create(provider);
        return true;
    }

    public boolean update(Provider newProvider, Map<String, Object> context) {
        Long id = newProvider.getId();
        String parameters = newProvider.getParameters();
        Provider provider = providerService.findProvider(id);
        if (provider == null) {
            context.put("message", getMessage("NoSuchOperationData", id));
            return false;
        }
        String service = provider.getService();
        if (!super.currentUser.hasServicePrivilege(service)) {
            context.put("message", getMessage("HaveNoServicePrivilege", service));
            return false;
        }
        Map<String, String> oldMap = StringUtils.parseQueryString(provider.getParameters());
        Map<String, String> newMap = StringUtils.parseQueryString(parameters);
        for (Map.Entry<String, String> entry : oldMap.entrySet()) {
            if (entry.getValue().equals(newMap.get(entry.getKey()))) {
                newMap.remove(entry.getKey());
            }
        }
        if (provider.isDynamic()) {
            String address = provider.getAddress();
            List<Override> overrides = overrideService.findByServiceAndAddress(provider.getService(), provider.getAddress());
            OverrideUtils.setProviderOverrides(provider, overrides);
            Override override = provider.getOverride();
            if (override != null) {
                if (newMap.size() > 0) {
                    override.setParams(StringUtils.toQueryString(newMap));
                    override.setEnabled(true);
                    override.setOperator(operator);
                    override.setOperatorAddress(operatorAddress);
                    overrideService.updateOverride(override);
                } else {
                    overrideService.deleteOverride(override.getId());
                }
            } else {
                override = new Override();
                override.setService(service);
                override.setAddress(address);
                override.setParams(StringUtils.toQueryString(newMap));
                override.setEnabled(true);
                override.setOperator(operator);
                override.setOperatorAddress(operatorAddress);
                overrideService.saveOverride(override);
            }
        } else {
            provider.setParameters(parameters);
            providerService.updateProvider(provider);
        }
        return true;
    }

    public boolean delete(Long[] ids, Map<String, Object> context) {
        for (Long id : ids) {
            Provider provider = providerService.findProvider(id);
            if (provider == null) {
                context.put("message", getMessage("NoSuchOperationData", id));
                return false;
            } else if (provider.isDynamic()) {
                context.put("message", getMessage("CanNotDeleteDynamicData", id));
                return false;
            } else if (!super.currentUser.hasServicePrivilege(provider.getService())) {
                context.put("message", getMessage("HaveNoServicePrivilege", provider.getService()));
                return false;
            }
        }
        for (Long id : ids) {
            providerService.deleteStaticProvider(id);
        }
        return true;
    }

    public boolean enable(Long[] ids, Map<String, Object> context) {
        Map<Long, Provider> id2Provider = new HashMap<Long, Provider>();
        for (Long id : ids) {
            Provider provider = providerService.findProvider(id);
            if (provider == null) {
                context.put("message", getMessage("NoSuchOperationData", id));
                return false;
            } else if (!super.currentUser.hasServicePrivilege(provider.getService())) {
                context.put("message", getMessage("HaveNoServicePrivilege", provider.getService()));
                return false;
            }
            id2Provider.put(id, provider);
        }
        for (Long id : ids) {
            providerService.enableProvider(id);
        }
        return true;
    }

    public boolean disable(Long[] ids, Map<String, Object> context) {
        for (Long id : ids) {
            Provider provider = providerService.findProvider(id);
            if (provider == null) {
                context.put("message", getMessage("NoSuchOperationData", id));
                return false;
            } else if (!super.currentUser.hasServicePrivilege(provider.getService())) {
                context.put("message", getMessage("HaveNoServicePrivilege", provider.getService()));
                return false;
            }
        }
        for (Long id : ids) {
            providerService.disableProvider(id);
        }
        return true;
    }

    public boolean doubling(Long[] ids, Map<String, Object> context) {
        for (Long id : ids) {
            Provider provider = providerService.findProvider(id);
            if (provider == null) {
                context.put("message", getMessage("NoSuchOperationData", id));
                return false;
            } else if (!super.currentUser.hasServicePrivilege(provider.getService())) {
                context.put("message", getMessage("HaveNoServicePrivilege", provider.getService()));
                return false;
            }
        }
        for (Long id : ids) {
            providerService.doublingProvider(id);
        }
        return true;
    }

    public boolean halving(Long[] ids, Map<String, Object> context) {
        for (Long id : ids) {
            Provider provider = providerService.findProvider(id);
            if (provider == null) {
                context.put("message", getMessage("NoSuchOperationData", id));
                return false;
            } else if (!super.currentUser.hasServicePrivilege(provider.getService())) {
                context.put("message", getMessage("HaveNoServicePrivilege", provider.getService()));
                return false;
            }
        }
        for (Long id : ids) {
            providerService.halvingProvider(id);
        }
        return true;
    }

}

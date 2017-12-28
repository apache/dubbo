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

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.governance.service.ConsumerService;
import com.alibaba.dubbo.governance.service.OverrideService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.Override;
import com.alibaba.dubbo.registry.common.route.OverrideUtils;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Providers.
 * URI: /applications
 *
 */
public class Applications extends Restful {

    @Autowired
    private ProviderService providerService;

    @Autowired
    private ConsumerService consumerService;

    @Autowired
    private OverrideService overrideService;

    public void index(Map<String, Object> context) {
        String service = (String) context.get("service");
        if (context.get("service") != null) {
            Set<String> applications = new TreeSet<String>();
            List<String> providerApplications = providerService.findApplicationsByServiceName(service);
            if (providerApplications != null && providerApplications.size() > 0) {
                applications.addAll(providerApplications);
            }
            List<String> consumerApplications = consumerService.findApplicationsByServiceName(service);
            if (consumerApplications != null && consumerApplications.size() > 0) {
                applications.addAll(consumerApplications);
            }
            context.put("applications", applications);
            context.put("providerApplications", providerApplications);
            context.put("consumerApplications", consumerApplications);
            if (service != null && service.length() > 0) {
                List<Override> overrides = overrideService.findByService(service);
                Map<String, List<Override>> application2Overrides = new HashMap<String, List<Override>>();
                if (overrides != null && overrides.size() > 0
                        && applications != null && applications.size() > 0) {
                    for (String a : applications) {
                        if (overrides != null && overrides.size() > 0) {
                            List<Override> appOverrides = new ArrayList<Override>();
                            for (Override override : overrides) {
                                if (override.isMatch(service, null, a)) {
                                    appOverrides.add(override);
                                }
                            }
                            Collections.sort(appOverrides, OverrideUtils.OVERRIDE_COMPARATOR);
                            application2Overrides.put(a, appOverrides);
                        }
                    }
                }
                context.put("overrides", application2Overrides);
            }
            return;
        }
        if (context.get("service") == null
                && context.get("application") == null
                && context.get("address") == null) {
            context.put("application", "*");
        }
        Set<String> applications = new TreeSet<String>();
        List<String> providerApplications = providerService.findApplications();
        if (providerApplications != null && providerApplications.size() > 0) {
            applications.addAll(providerApplications);
        }
        List<String> consumerApplications = consumerService.findApplications();
        if (consumerApplications != null && consumerApplications.size() > 0) {
            applications.addAll(consumerApplications);
        }

        Set<String> newList = new HashSet<String>();
        Set<String> newProviders = new HashSet<String>();
        Set<String> newConsumers = new HashSet<String>();
        context.put("applications", applications);
        context.put("providerApplications", providerApplications);
        context.put("consumerApplications", consumerApplications);

        String keyword = (String) context.get("keyword");
        if (StringUtils.isNotEmpty(keyword) && !"*".equals(keyword)) {
            keyword = keyword.toLowerCase();
            for (String o : applications) {
                if (o.toLowerCase().indexOf(keyword) != -1) {
                    newList.add(o);
                }
            }
            for (String o : providerApplications) {
                if (o.toLowerCase().indexOf(keyword) != -1) {
                    newProviders.add(o);
                }
            }
            for (String o : consumerApplications) {
                if (o.toLowerCase().indexOf(keyword) != -1) {
                    newConsumers.add(o);
                }
            }
            context.put("applications", newList);
            context.put("providerApplications", newProviders);
            context.put("consumerApplications", newConsumers);
        }
    }

    public void search(Map<String, Object> context) {
        index(context);

        Set<String> newList = new HashSet<String>();
        @SuppressWarnings("unchecked")
        Set<String> apps = (Set<String>) context.get("applications");
        String keyword = (String) context.get("keyword");
        if (StringUtils.isNotEmpty(keyword)) {
            keyword = keyword.toLowerCase();
            for (String o : apps) {
                if (o.toLowerCase().indexOf(keyword) != -1) {
                    newList.add(o);
                }
            }
        }
        context.put("applications", newList);
    }

    public boolean shield(Map<String, Object> context) throws Exception {
        return mock(context, "force:return null");
    }

    public boolean tolerant(Map<String, Object> context) throws Exception {
        return mock(context, "fail:return null");
    }

    public boolean recover(Map<String, Object> context) throws Exception {
        return mock(context, "");
    }

    private boolean mock(Map<String, Object> context, String mock) throws Exception {
        String service = (String) context.get("service");
        String applications = (String) context.get("application");
        if (service == null || service.length() == 0
                || applications == null || applications.length() == 0) {
            context.put("message", getMessage("NoSuchOperationData"));
            return false;
        }
        if (!super.currentUser.hasServicePrivilege(service)) {
            context.put("message", getMessage("HaveNoServicePrivilege", service));
            return false;
        }
        for (String application : SPACE_SPLIT_PATTERN.split(applications)) {
            List<Override> overrides = overrideService.findByServiceAndApplication(service, application);
            if (overrides != null && overrides.size() > 0) {
                for (Override override : overrides) {
                    Map<String, String> map = StringUtils.parseQueryString(override.getParams());
                    if (mock == null || mock.length() == 0) {
                        map.remove("mock");
                    } else {
                        map.put("mock", URL.encode(mock));
                    }
                    if (map.size() > 0) {
                        override.setParams(StringUtils.toQueryString(map));
                        override.setEnabled(true);
                        override.setOperator(operator);
                        override.setOperatorAddress(operatorAddress);
                        overrideService.updateOverride(override);
                    } else {
                        overrideService.deleteOverride(override.getId());
                    }
                }
            } else if (mock != null && mock.length() > 0) {
                Override override = new Override();
                override.setService(service);
                override.setApplication(application);
                override.setParams("mock=" + URL.encode(mock));
                override.setEnabled(true);
                override.setOperator(operator);
                override.setOperatorAddress(operatorAddress);
                overrideService.saveOverride(override);
            }
        }
        return true;
    }

    public boolean allshield(Map<String, Object> context) throws Exception {
        return allmock(context, "force:return null");
    }

    public boolean alltolerant(Map<String, Object> context) throws Exception {
        return allmock(context, "fail:return null");
    }

    public boolean allrecover(Map<String, Object> context) throws Exception {
        return allmock(context, "");
    }

    private boolean allmock(Map<String, Object> context, String mock) throws Exception {
        String service = (String) context.get("service");
        if (service == null || service.length() == 0) {
            context.put("message", getMessage("NoSuchOperationData"));
            return false;
        }
        if (!super.currentUser.hasServicePrivilege(service)) {
            context.put("message", getMessage("HaveNoServicePrivilege", service));
            return false;
        }
        List<Override> overrides = overrideService.findByService(service);
        Override allOverride = null;
        if (overrides != null && overrides.size() > 0) {
            for (Override override : overrides) {
                if (override.isDefault()) {
                    allOverride = override;
                    break;
                }
            }
        }
        if (allOverride != null) {
            Map<String, String> map = StringUtils.parseQueryString(allOverride.getParams());
            if (mock == null || mock.length() == 0) {
                map.remove("mock");
            } else {
                map.put("mock", URL.encode(mock));
            }
            if (map.size() > 0) {
                allOverride.setParams(StringUtils.toQueryString(map));
                allOverride.setEnabled(true);
                allOverride.setOperator(operator);
                allOverride.setOperatorAddress(operatorAddress);
                overrideService.updateOverride(allOverride);
            } else {
                overrideService.deleteOverride(allOverride.getId());
            }
        } else if (mock != null && mock.length() > 0) {
            Override override = new Override();
            override.setService(service);
            override.setParams("mock=" + URL.encode(mock));
            override.setEnabled(true);
            override.setOperator(operator);
            override.setOperatorAddress(operatorAddress);
            overrideService.saveOverride(override);
        }
        return true;
    }

}

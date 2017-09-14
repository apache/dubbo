/*
 * Copyright 1999-2101 Alibaba Group.
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
package com.alibaba.dubbo.governance.web.governance.module.screen;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.governance.service.ConsumerService;
import com.alibaba.dubbo.governance.service.OverrideService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.Override;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ding.lid
 */
public class Overrides extends Restful {
    static final Pattern AND = Pattern.compile("\\&");
    static final Pattern EQUAL = Pattern.compile("([^=\\s]*)\\s*=\\s*(\\S*)");
    static final String DEFAULT_MOCK_JSON_KEY = "mock";
    static final String MOCK_JSON_KEY_POSTFIX = ".mock";
    static final String FORM_OVERRIDE_KEY = "overrideKey";
    static final String FORM_OVERRIDE_VALUE = "overrideValue";
    static final String FORM_DEFAULT_MOCK_METHOD_FORCE = "mockDefaultMethodForce";
    static final String FORM_DEFAULT_MOCK_METHOD_JSON = "mockDefaultMethodJson";
    static final String FORM_ORIGINAL_METHOD_FORCE_PREFIX = "mockMethodForce.";
    static final String FORM_ORIGINAL_METHOD_PREFIX = "mockMethod.";
    static final String FORM_DYNAMIC_METHOD_NAME_PREFIX = "mockMethodName";
    static final String FORM_DYNAMIC_METHOD_FORCE_PREFIX = "mockMethodForce";
    static final String FORM_DYNAMIC_METHOD_JSON_PREFIX = "mockMethodJson";
    @Autowired
    private OverrideService overrideService;

    // FORM KEY
    @Autowired
    private ProviderService providerService;
    @Autowired
    private ConsumerService consumerService;

    static Map<String, String> parseQueryString(String query) {
        HashMap<String, String> ret = new HashMap<String, String>();
        if (query == null || (query = query.trim()).length() == 0) return ret;

        String[] kvs = AND.split(query);
        for (String kv : kvs) {
            Matcher matcher = EQUAL.matcher(kv);
            if (!matcher.matches()) continue;
            String key = matcher.group(1);
            String value = matcher.group(2);
            ret.put(key, value);
        }

        return ret;
    }

    public void index(Map<String, Object> context) {
        String service = (String) context.get("service");
        String application = (String) context.get("application");
        String address = (String) context.get("address");
        List<Override> overrides;
        if (StringUtils.isNotEmpty(service)) {
            overrides = overrideService.findByService(service);
        } else if (StringUtils.isNotEmpty(application)) {
            overrides = overrideService.findByApplication(application);
        } else if (StringUtils.isNotEmpty(address)) {
            overrides = overrideService.findByAddress(address);
        } else {
            overrides = overrideService.findAll();
        }
        context.put("overrides", overrides);
    }

    public void show(Long id, Map<String, Object> context) {
        Override override = overrideService.findById(id);

        Map<String, String> parameters = parseQueryString(override.getParams());

        if (parameters.get(DEFAULT_MOCK_JSON_KEY) != null) {
            String mock = URL.decode(parameters.get(DEFAULT_MOCK_JSON_KEY));
            String[] tokens = parseMock(mock);
            context.put(FORM_DEFAULT_MOCK_METHOD_FORCE, tokens[0]);
            context.put(FORM_DEFAULT_MOCK_METHOD_JSON, tokens[1]);
            parameters.remove(DEFAULT_MOCK_JSON_KEY);
        }

        Map<String, String> method2Force = new LinkedHashMap<String, String>();
        Map<String, String> method2Json = new LinkedHashMap<String, String>();

        for (Iterator<Map.Entry<String, String>> iterator = parameters.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, String> e = iterator.next();
            String key = e.getKey();

            if (key.endsWith(MOCK_JSON_KEY_POSTFIX)) {
                String m = key.substring(0, key.length() - MOCK_JSON_KEY_POSTFIX.length());
                parseMock(m, e.getValue(), method2Force, method2Json);
                iterator.remove();
            }
        }

        context.put("methodForces", method2Force);
        context.put("methodJsons", method2Json);
        context.put("parameters", parameters);
        context.put("override", override);
    }

    public void add(Map<String, Object> context) {
        List<String> serviceList = new ArrayList<String>();
        List<String> applicationList = new ArrayList<String>();
        String service = (String) context.get("service");
        String application = (String) context.get("application");
        if (StringUtils.isNotEmpty(application)) {
            serviceList.addAll(providerService.findServicesByApplication(application));
            serviceList.addAll(consumerService.findServicesByApplication(application));
            context.put("serviceList", serviceList);
        } else if (StringUtils.isNotEmpty(service)) {
            applicationList.addAll(providerService.findApplicationsByServiceName(service));
            applicationList.addAll(consumerService.findApplicationsByServiceName(service));
            context.put("applicationList", applicationList);
        } else {
            serviceList.addAll(providerService.findServices());
            serviceList.addAll(consumerService.findServices());
            providerService.findServicesByApplication(application);
            consumerService.findServicesByApplication(application);
        }
        context.put("serviceList", serviceList);

        if (StringUtils.isNotEmpty(service) && !service.contains("*")) {
            context.put("methods", CollectionUtils.sort(new ArrayList<String>(providerService.findMethodsByService(service))));
        }
    }

    public void edit(Long id, Map<String, Object> context) {
        Override override = overrideService.findById(id);

        Map<String, String> parameters = parseQueryString(override.getParams());

        if (parameters.get(DEFAULT_MOCK_JSON_KEY) != null) {
            String mock = URL.decode(parameters.get(DEFAULT_MOCK_JSON_KEY));
            String[] tokens = parseMock(mock);
            context.put(FORM_DEFAULT_MOCK_METHOD_FORCE, tokens[0]);
            context.put(FORM_DEFAULT_MOCK_METHOD_JSON, tokens[1]);
            parameters.remove(DEFAULT_MOCK_JSON_KEY);
        }

        Map<String, String> method2Force = new LinkedHashMap<String, String>();
        Map<String, String> method2Json = new LinkedHashMap<String, String>();

        List<String> methods = CollectionUtils.sort(new ArrayList<String>(providerService.findMethodsByService(override.getService())));
        if (methods != null && methods.isEmpty()) {
            for (String m : methods) {
                parseMock(m, parameters.get(m + MOCK_JSON_KEY_POSTFIX), method2Force, method2Json);
                parameters.remove(m + MOCK_JSON_KEY_POSTFIX);
            }
        }
        for (Iterator<Map.Entry<String, String>> iterator = parameters.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, String> e = iterator.next();
            String key = e.getKey();

            if (key.endsWith(MOCK_JSON_KEY_POSTFIX)) {
                String m = key.substring(0, key.length() - MOCK_JSON_KEY_POSTFIX.length());
                parseMock(m, e.getValue(), method2Force, method2Json);
                iterator.remove();
            }
        }

        context.put("methods", methods);
        context.put("methodForces", method2Force);
        context.put("methodJsons", method2Json);
        context.put("parameters", parameters);
        context.put("override", override);
    }

    private void parseMock(String m, String mock, Map<String, String> method2Force, Map<String, String> method2Json) {
        String[] tokens = parseMock(mock);
        method2Force.put(m, tokens[0]);
        method2Json.put(m, tokens[1]);
    }

    private String[] parseMock(String mock) {
        mock = URL.decode(mock);
        String force;
        if (mock.startsWith("force:")) {
            force = "force";
            mock = mock.substring("force:".length());
        } else if (mock.startsWith("fail:")) {
            force = "fail";
            mock = mock.substring("fail:".length());
        } else {
            force = "fail";
        }
        String[] tokens = new String[2];
        tokens[0] = force;
        tokens[1] = mock;
        return tokens;
    }

    boolean catchParams(Override override, Map<String, Object> context) {
        String service = (String) context.get("service");
        if (service == null || service.trim().length() == 0) {
            context.put("message", getMessage("service is blank!"));
            return false;
        }
        if (!super.currentUser.hasServicePrivilege(service)) {
            context.put("message", getMessage("HaveNoServicePrivilege", service));
            return false;
        }

        String defaultMockMethodForce = (String) context.get(FORM_DEFAULT_MOCK_METHOD_FORCE);
        String defaultMockMethodJson = (String) context.get(FORM_DEFAULT_MOCK_METHOD_JSON);

        Map<String, String> override2Value = new HashMap<String, String>();
        Map<String, String> method2Json = new HashMap<String, String>();

        for (Map.Entry<String, Object> param : context.entrySet()) {
            String key = param.getKey().trim();
            if (!(param.getValue() instanceof String)) continue;

            String value = (String) param.getValue();

            if (key.startsWith(FORM_OVERRIDE_KEY) && value != null && value.trim().length() > 0) {
                String index = key.substring(FORM_OVERRIDE_KEY.length());
                String overrideValue = (String) context.get(FORM_OVERRIDE_VALUE + index);
                if (overrideValue != null && overrideValue.trim().length() > 0) {
                    override2Value.put(value.trim(), overrideValue.trim());
                }
            }

            if (key.startsWith(FORM_ORIGINAL_METHOD_PREFIX) && value != null && value.trim().length() > 0) {
                String method = key.substring(FORM_ORIGINAL_METHOD_PREFIX.length());
                String force = (String) context.get(FORM_ORIGINAL_METHOD_FORCE_PREFIX + method);
                method2Json.put(method, force + ":" + value.trim());
            }

            if (key.startsWith(FORM_DYNAMIC_METHOD_NAME_PREFIX) && value != null && value.trim().length() > 0) {
                String index = key.substring(FORM_DYNAMIC_METHOD_NAME_PREFIX.length());
                String force = (String) context.get(FORM_DYNAMIC_METHOD_FORCE_PREFIX + index);
                String json = (String) context.get(FORM_DYNAMIC_METHOD_JSON_PREFIX + index);

                if (json != null && json.trim().length() > 0) {
                    method2Json.put(value.trim(), force + ":" + json.trim());
                }
            }
        }

        StringBuilder paramters = new StringBuilder();
        boolean isFirst = true;
        if (defaultMockMethodJson != null && defaultMockMethodJson.trim().length() > 0) {
            paramters.append("mock=").append(URL.encode(defaultMockMethodForce + ":" + defaultMockMethodJson.trim()));
            isFirst = false;
        }
        for (Map.Entry<String, String> e : method2Json.entrySet()) {
            if (isFirst) isFirst = false;
            else paramters.append("&");

            paramters.append(e.getKey()).append(MOCK_JSON_KEY_POSTFIX).append("=").append(URL.encode(e.getValue()));
        }
        for (Map.Entry<String, String> e : override2Value.entrySet()) {
            if (isFirst) isFirst = false;
            else paramters.append("&");

            paramters.append(e.getKey()).append("=").append(URL.encode(e.getValue()));
        }

        String p = paramters.toString();
        if (p.trim().length() == 0) {
            context.put("message", getMessage("Please enter Parameters!"));
            return false;
        }

        override.setParams(p);
        return true;
    }

    public boolean create(Override override, Map<String, Object> context) {
        if (!catchParams(override, context)) return false;

        overrideService.saveOverride(override);
        return true;
    }

    public boolean update(Override override, Map<String, Object> context) {
        Override o = overrideService.findById(override.getId());
        override.setService(o.getService());
        override.setAddress(o.getAddress());
        override.setApplication(o.getApplication());

        if (!catchParams(override, context)) return false;

        overrideService.updateOverride(override);

        return true;
    }

    public boolean delete(Long[] ids, Map<String, Object> context) {
        for (Long id : ids) {
            overrideService.deleteOverride(id);
        }

        return true;
    }

    public boolean enable(Long[] ids, Map<String, Object> context) {
        for (Long id : ids) {
            Override override = overrideService.findById(id);
            if (override == null) {
                context.put("message", getMessage("NoSuchOperationData", id));
                return false;
            } else {
                if (!super.currentUser.hasServicePrivilege(override.getService())) {
                    context.put("message", getMessage("HaveNoServicePrivilege", override.getService()));
                    return false;
                }
            }
        }

        for (Long id : ids) {
            overrideService.enableOverride(id);
        }

        return true;
    }

    public boolean disable(Long[] ids, Map<String, Object> context) {
        for (Long id : ids) {
            Override override = overrideService.findById(id);
            if (override == null) {
                context.put("message", getMessage("NoSuchOperationData", id));
                return false;
            } else {
                if (!super.currentUser.hasServicePrivilege(override.getService())) {
                    context.put("message", getMessage("HaveNoServicePrivilege", override.getService()));
                    return false;
                }
            }
        }

        for (Long id : ids) {
            overrideService.disableOverride(id);
        }

        return true;
    }

}

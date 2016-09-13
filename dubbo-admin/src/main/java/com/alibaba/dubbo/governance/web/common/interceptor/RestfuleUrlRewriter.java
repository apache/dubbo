/**
 * Project: dubbo.registry.console-2.1.0-SNAPSHOT
 * 
 * File Created at Sep 5, 2011
 * $Id: RestfuleUrlRewriter.java 181192 2012-06-21 05:05:47Z tony.chenl $
 * 
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.common.interceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.alibaba.citrus.service.requestcontext.rewrite.RewriteSubstitutionContext;
import com.alibaba.citrus.service.requestcontext.rewrite.RewriteSubstitutionHandler;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

/**
 * Restful URL Rewrite成 WebX的URL。
 * 
 * @author ding.lid
 */
public class RestfuleUrlRewriter implements RewriteSubstitutionHandler {

    private static final Logger              logger              = LoggerFactory.getLogger(RestfuleUrlRewriter.class);

    private static final Map<String, String> pl2single;
    private static final Set<String>         appParameter;
    static {
        Map<String, String> map = new HashMap<String, String>();

        map.put("services", "service");
        map.put("users", "user");
        map.put("addresses", "address");
        map.put("applications", "application");

        map.put("providers", "provider");
        map.put("comsumers", "comsumer");

        pl2single = Collections.unmodifiableMap(map);
        Set<String> set = new HashSet<String>();

        set.add("_path");
        set.add("currentPage");
        set.add("_method");
        set.add("_type");
        set.add("id");

        appParameter = Collections.unmodifiableSet(set);
    }

    private final static String              METHOD_KEY          = "_method";                                         // show,
    private final static String              TYPE_KEY            = "_type";
    private final static String              ID_KEY              = "id";
    private final static String              PAGES_KEY           = "currentPage";
    private final static String              PATH_KEY            = "_path";

    private final static Pattern             SLASH_PATTERN       = Pattern.compile("/+");
    private final static Pattern             NUM_PATTERN         = Pattern.compile("\\d+");
    private final static Pattern             MULTI_NUM_PATTERN   = Pattern.compile("[+\\d]+");
    private final static Pattern             PAGES_SPLIT_PATTERN = Pattern.compile("/+pages/+");
    private final static Pattern             PAGES_PATTERN       = Pattern.compile(".*/+pages/+\\d+$");

    public void postSubstitution(RewriteSubstitutionContext context) {
        final String oldPath = context.getPath();
        String path = oldPath;
        while (path.startsWith("/")) {
            path = path.substring(1);
        }

        Map<String, String> param = new HashMap<String, String>();

        // 处理分页
        if (PAGES_PATTERN.matcher(path).matches()) {
            String[] page_path = PAGES_SPLIT_PATTERN.split(path);
            path = page_path[0];
            param.put(PATH_KEY, page_path[0]);
            param.put(PAGES_KEY, page_path[1]);
        } else {
            param.put(PATH_KEY, path);
        }
        List<String> temp = Arrays.asList(SLASH_PATTERN.split(path));
        //兼容2.0.x注册中心的shell风格url 如：http://root:hello1234@127.0.0.1:8080/status/dubbo.test.api.HelloService:1.1
        if("status".equals(temp.get(0))&&temp.size()>1){
            context.setPath("servicestatus");
            return;
        }
        //兼容包含group的path
        String[] split = temp.toArray(new String[temp.size()]);
        
        if(temp.size()>2&&temp.contains("services")){
            List<String> parts = new ArrayList<String>();
            parts.addAll(temp);
            for(int i = 0;i<temp.size();i++){
                if ("services".equals(temp.get(i)) && i < (temp.size() - 1) && (!temp.get(i + 1).contains("."))&&(!temp.get(i + 1).matches("\\d+"))) {
                    String group = parts.get(i + 1);
                    String service = parts.get(i + 2);
                    parts.remove(i + 1);
                    parts.set(i+1, group + "/" + service);
                    break;
                }
            }
            split = parts.toArray(new String[parts.size()]);
        }
      
        int index = split.length;
        // module/action
        if (split.length < 2) return;
        // 最后一段不包含 '.'，如 .htm .xsd .css etc
        if (split[index - 1].contains(".")) {
            return;
        }

        final String type;
        // 偶数段
        // module/k/v/type or module/k/v/type/id/method
        if (index % 2 == 0) {
            if (MULTI_NUM_PATTERN.matcher(split[index - 2]).matches()) {
                // module/k/v/action/id/operate
                if (index < 4) return;

                param.put(METHOD_KEY, split[index - 1]);
                param.put(ID_KEY, split[index - 2]);
                type = split[index - 3];
                param.put(TYPE_KEY, type);
                index -= 3;
            } else {
                // module/k/v/type
                type = split[index - 1];
                param.put(TYPE_KEY, type);
                --index;
            }
        }
        // 奇数段
        // module/k/v/type/method or module/k/v/type/id
        else {
            if (index < 3) return;
            // module/k/v/type/id
            if (NUM_PATTERN.matcher(split[index - 1]).matches()) {
                param.put(ID_KEY, split[index - 1]);
            }
            // module/k/v/type/method
            else {
                param.put(METHOD_KEY, split[index - 1]);
            }
            type = split[index - 2];
            param.put(TYPE_KEY, type);
            index -= 2;
        }

        // 提取 KV对
        for (int i = 1; i < index; i += 2) {
            param.put(split[i], split[i + 1]);
        }

        String method = param.get(METHOD_KEY);
        
        String defaultRedirect = null;
        if(method == null || method.equals("index")){
            defaultRedirect = oldPath;
        }else{
            defaultRedirect = oldPath.split("/" + method)[0];
        }
        String id = param.get(ID_KEY);
        if(id != null){
            int i = defaultRedirect.lastIndexOf("/");
            defaultRedirect = defaultRedirect.substring(0,i);
        }
        context.getParameters().setString("defaultRedirect", defaultRedirect);
        
        final String module = split[0];
        context.setPath("/" + module + "/" + type + ".htm");

        for (Map.Entry<String, String> entry : param.entrySet()) {
            String key = entry.getKey();
            if (pl2single.containsKey(key)) {
                key = pl2single.get(key);
            } else if (appParameter.contains(key)) {
                // nothing
            } else {
                logger.info("key " + key + " is not pl noun!");
            }
            context.getParameters().setString(key, entry.getValue());
        }

        if (logger.isInfoEnabled()) {
            logger.info("REWRITE restful uri " + oldPath + " to uri " + module + "/" + type + ".htm?" + param);
        }
    }
}

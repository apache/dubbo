/**
 * Project: dubbo.registry.server-1.1.0-SNAPSHOT
 * 
 * File Created at 2010-7-27
 * 
 * Copyright 1999-2010 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.common.pulltool;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.governance.service.OverrideService;
import com.alibaba.dubbo.governance.service.RouteService;
import com.alibaba.dubbo.registry.common.domain.Consumer;
import com.alibaba.dubbo.registry.common.domain.Override;
import com.alibaba.dubbo.registry.common.domain.Provider;
import com.alibaba.dubbo.registry.common.domain.Route;
import com.alibaba.dubbo.registry.common.domain.User;
import com.alibaba.dubbo.registry.common.route.RouteUtils;
import com.alibaba.dubbo.registry.common.util.StringEscapeUtils;

/**
 * Tool
 * 
 * @author william.liangf
 */
public class Tool {
	OverrideService overrideService;
	
	RouteService routeService;
	
	public void setOverrideService(OverrideService overrideService) {
		this.overrideService = overrideService;
	}

	public void setRouteService(RouteService routeService) {
		this.routeService = routeService;
	}

    
    public static String toStackTraceString(Throwable t) {
        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        t.printStackTrace(pw);
        return writer.toString();
    }

    public static boolean isContains(String[] values, String value) {
        return StringUtils.isContains(values, value);
    }
    
    public static boolean startWith(String value, String prefix) {
        return value.startsWith(prefix);
    }

    public static String getHostName(String address) {
        if (address != null && address.length() > 0) {
            String hostname = NetUtils.getHostName(address);
            if (! address.equals(hostname)) {
                return "(" + hostname + ")";
            }
        }
        return "";
    }
    
    public static String getHostAddress(String address) {
        if (address != null && address.length() > 0) {
            int i = address.indexOf(':');
            String port = address.substring(i+1);
            String hostname = NetUtils.getHostName(address);
            if (! address.equals(hostname)) {
                return hostname + ":" + port;
            }
        }
        return "";
    }
    
    public static String getPath(String url) {
        try {
            return URL.valueOf(url).getPath();
        } catch (Throwable t) {
            return url;
        }
    }
    
    public static String getAddress(String url) {
        try {
            return URL.valueOf(url).getAddress();
        } catch (Throwable t) {
            return url;
        }
    }
    
    public static String getIP(String address) {
    	if (address != null && address.length() > 0) {
	    	int i = address.indexOf("://");
	    	if (i >= 0) {
	    	    address = address.substring(i + 3);
	    	}
	    	i = address.indexOf('/');
            if (i >= 0) {
                address = address.substring(0, i);
            }
            i = address.indexOf('@');
            if (i >= 0) {
                address = address.substring(i + 1);
            }
            i = address.indexOf(':');
            if (i >= 0) {
                address = address.substring(0, i);
            }
            if (address.matches("[a-zA-Z]+")) {
                try {
                    address = InetAddress.getByName(address).getHostAddress();
                } catch (UnknownHostException e) {
                }
            }
    	}
    	return address;
    }
    
    public static String encode(String url) {
        try {
            return URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return url;
        }
    }
    
    public static String escape(String html) {
        return StringEscapeUtils.escapeHtml(html);
    }

    public static String unescape(String html) {
        return StringEscapeUtils.unescapeHtml(html);
    }
    
    public static String encodeUrl(String url) {
    	return URL.encode(url);
    }

    public static String decodeUrl(String url) {
        return URL.decode(url);
    }
    
    public static String encodeHtml(String html) {
    	return StringEscapeUtils.escapeHtml(html);
    }
    
    public static int countMapValues(Map<?, ?> map) {
    	int total = 0;
    	if (map != null && map.size() > 0) {
	    	for (Object value : map.values()) {
	    		if (value != null) {
		    		if (value instanceof Number) {
		    			total += ((Number)value).intValue();
		    		} else if (value.getClass().isArray()) {
		    			total += Array.getLength(value);
		    		} else if (value instanceof Collection) {
		    			total += ((Collection<?>)value).size();
		    		} else if (value instanceof Map) {
		    			total += ((Map<?, ?>)value).size();
		    		} else {
		    			total += 1;
		    		}
	    		}
	    	}
    	}
    	return total;
    }
	
	private static final Comparator<String> SIMPLE_NAME_COMPARATOR = new Comparator<String>() {
		public int compare(String s1, String s2) {
			if (s1 == null && s2 == null) {
				return 0;
			}
			if (s1 == null) {
				return -1;
			}
			if (s2 == null) {
				return 1;
			}
			s1 = getSimpleName(s1);
			s2 = getSimpleName(s2);
			return s1.compareToIgnoreCase(s2);
		}
	};
	
	public static List<String> sortSimpleName(List<String> list) {
		if (list != null && list.size() > 0) {
			Collections.sort(list, SIMPLE_NAME_COMPARATOR);
		}
		return list;
	}
	
	public static String getSimpleName(String name) {
        if (name != null && name.length() > 0) {
            final int ip = name.indexOf('/');
            String v = ip != -1 ? name.substring(0, ip + 1) : "";
            
            int i = name.lastIndexOf(':');
            int j = (i >= 0 ? name.lastIndexOf('.', i) : name.lastIndexOf('.'));
            if (j >= 0) {
                name = name.substring(j + 1);
            }
            name = v + name;
        }
        return name;
    }
	
	public static String getParameter(String parameters, String key){
        String value = "";
        if (parameters != null && parameters.length() > 0) {
            String[] pairs = parameters.split("&");
            for(String pair : pairs){
                String[] kv = pair.split("=");
                if(key.equals(kv[0])){
                    value = kv[1];
                    break;
                }
            }
        }
        return value;
    }
	

	/**
	 * 从provider的paramters参数中获取版本值
	 * @param parameters 
	 * @return
	 */
	public static String getVersionFromPara(String parameters){
		String version = "";
		if (parameters != null && parameters.length() > 0) {
			String[] params = parameters.split("&");
			for(String o : params){
				String[] kv = o.split("=");
				if("version".equals(kv[0])){
					version = kv[1];
					break;
				}
			}
		}
		return version;
	}
	
	 //时间格式化
    public String formatDate(Date date){
    	if(date==null){
    		return "";
    	}
    	 return DateFormatUtil.getDateFormat().format(date);
    }
    
    public String formatDate(Date date, String template){
    	if(date==null || template==null){
    		return "";
    	}
   	 	return DateFormatUtil.getDateFormat(template).format(date);
    }
    
    public boolean beforeNow(Date date){
        Date now = new Date();
        if(now.after(date)){
            return true;  
        }
        return false;
    }
    
    //时间相减
    public long dateMinus(Date date1, Date date2){
   	  return (date1.getTime() - date1.getTime())/ 1000;
    }
    
    public boolean isProviderEnabled(Provider provider){
    	List<Override> oList = overrideService.findByServiceAndAddress(provider.getService(), provider.getAddress());
    	for(Override o : oList){
    		Map<String, String> params = StringUtils.parseQueryString(o.getParams());
        	if(params.containsKey(Constants.DISABLED_KEY)){
        		if(params.get(Constants.DISABLED_KEY) .equals("true")){
        			return false;
        		}else{
        			return true;
        		}
        	}
    	}
    	if(provider.isEnabled()){
			return true;
		}else{
			return false;
		}
    	
    	
    }
    
    public boolean isInBlackList(Consumer consumer){
    	 Map<String, Route> findUsedRoute = RouteUtils.findUsedRoute(consumer.getService(), consumer.getAddress(), consumer.getParameters(),
    			 routeService.findByService(consumer.getService()), null);
    	 for(Entry<String, Route> entry : findUsedRoute.entrySet()){
    		 Route route = entry.getValue();
    		 if(route.isForce()){
    			 return true;
    		 }
    	 }
    	return false;
    }
    
    public boolean checkUrl(User user,String uri){
        return true;
        /*if(!User.ROOT.equals(user.getRole())){
            List<String> disabledSysinfo = new ArrayList<String>();
            List<String> disabledSysmanage = new ArrayList<String>();
            Map<String, Boolean> features = daoCache.getFeatures();
            if (features.size() > 0){
                for(Entry<String,Boolean> feature : features.entrySet()){
                    if(feature.getKey().startsWith("Sysinfo") && !feature.getValue()){
                        disabledSysinfo.add(feature.getKey().replace(".", "/").toLowerCase());
                    }else if(feature.getKey().startsWith("Sysmanage") && !feature.getValue()){
                        disabledSysmanage.add(feature.getKey().replace(".", "/").toLowerCase());
                    }
                }
                if(uri.startsWith("/sysinfo")){
                    for(String disabled : disabledSysinfo){
                        if (uri.contains(disabled)){
                            return false;
                        }
                    }
                }
                if(uri.startsWith("/sysmanage")){
                    for(String disabled : disabledSysmanage){
                        if (uri.contains(disabled)){
                            return false;
                        }
                    }
                }
            }else{
                return true;
            }
        }
        return true;*/
    }
}

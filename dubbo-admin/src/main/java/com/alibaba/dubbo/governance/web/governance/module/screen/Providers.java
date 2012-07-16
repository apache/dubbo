/*
 * Copyright 2011 Alibaba.com All right reserved. This software is the
 * confidential and proprietary information of Alibaba.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.governance.module.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.governance.service.OverrideService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.Override;
import com.alibaba.dubbo.registry.common.domain.Provider;
import com.alibaba.dubbo.registry.common.route.OverrideUtils;
import com.alibaba.dubbo.registry.common.util.Tool;

/**
 * <p>Providers.</p>
 * URI: <br>
 * GET /providers 全部提供者列表<br>
 * GET /providers/add 新增提供者表单<br>
 * POST /providers 创建提供者<br>
 * GET /providers/$id 查看提供者详细<br>
 * GET /providers/$id/edit 编辑提供者表单<br>
 * POST /providers/$id 更新提供者<br>
 * GET /providers/$id/delete 删除提供者<br>
 * GET /providers/$id/tostatic 转为静态<br>
 * GET /providers/$id/todynamic 转为动态<br>
 * GET /providers/$id/enable 启用<br>
 * GET /providers/$id/disable 禁用<br>
 * GET /providers/$id/reconnect 重连<br>
 * GET /providers/$id/recover 恢复<br>
 * <br>
 * GET /services/$service/providers 指定服务的提供者列表<br>
 * GET /services/$service/providers/add 新增提供者表单<br>
 * POST /services/$service/providers 创建提供者<br>
 * GET /services/$service/providers/$id 查看提供者详细<br>
 * GET /services/$service/providers/$id/edit 编辑提供者表单<br>
 * POST /services/$service/providers/$id 更新提供者<br>
 * GET /services/$service/providers/$id/delete 删除提供者<br>
 * GET /services/$service/providers/$id/tostatic 转为静态<br>
 * GET /services/$service/providers/$id/todynamic 转为动态<br>
 * GET /services/$service/providers/$id/enable 启用<br>
 * GET /services/$service/providers/$id/disable 禁用<br>
 * GET /services/$service/providers/$id/reconnect 重连<br>
 * GET /services/$service/providers/$id/recover 恢复<br>
 * 
 * @author william.liangf
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
        String address = (String)context.get("address");
        
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
        
        // 设置搜索结果到cookie中
        setSearchHistroy(context, value);
    }
    
    /**
     * 设置search记录到cookie中，操作步骤：
     * 检查加入的记录是否已经存在cookie中，如果存在，则更新列表次序；如果不存在，则插入到最前面
     * @param context
     * @param value
     */
    private void setSearchHistroy(Map<String, Object> context, String value) {
    	//分析已有的cookie
    	String separatorsB = "\\.\\.\\.\\.\\.\\.";
        String newCookiev = value;
        Cookie[] cookies = request.getCookies();
    	for(Cookie c:cookies){
    		if(c.getName().equals("HISTORY")){
    			String cookiev = c.getValue();
    			String[] values = cookiev.split(separatorsB);
    			int count = 1;
    			for(String v : values){
    				if(count<=10){
    					if(!value.equals(v)){
    						newCookiev = newCookiev + separatorsB + v;
    					}
    				}
    				count ++;
    			}
    			break;
    		}
    	}
    	
        Cookie _cookie=new Cookie("HISTORY", newCookiev);
        _cookie.setMaxAge(60*60*24*7); // 设置Cookie的存活时间为30分钟
        _cookie.setPath("/"); 
        response.addCookie(_cookie); // 写入客户端硬盘
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
     * 装载新增服务页面，获取所有的服务名称
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
        provider.setDynamic(false); // 页面上添加的一定是静态的Provider
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
			} else if (! super.currentUser.hasServicePrivilege(provider.getService())) {
				context.put( "message", getMessage("HaveNoServicePrivilege", provider.getService()));
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
			} else if (! super.currentUser.hasServicePrivilege(provider.getService())) {
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
			} else if (! super.currentUser.hasServicePrivilege(provider.getService())) {
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
			} else if (! super.currentUser.hasServicePrivilege(provider.getService())) {
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
			} else if (! super.currentUser.hasServicePrivilege(provider.getService())) {
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

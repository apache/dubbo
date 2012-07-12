package com.alibaba.dubbo.governance.web.util;

import java.util.Map;

/**
 * 包含在web层用到的常量
 * 
 * @author guanghui.shigh
 */
public class WebConstants {
	
    Map<String, Object> context;
    
    /** 
     * 在session中保存当前用户对象的key。
     */
    public static final String CURRENT_USER_KEY = "currentUser";
    
    /**
     * 当前的挂号服务器地址
     */
	public static final String REGISTRY_ADDRESS = "registryAddress";
	
	/**
     * 服务暴露地址
     */
    public static final String SERVICE_URL = "serviceUrl";
    
    /**
     * 服务名称
     */
    public static final String SERVICE_NAME = "serviceName";
    
    /**
     * 服务名称
     */
    public static final String ENTRY = "entry";
    
    /**
     * buc sso 登出地址
     */
    public static final String SSO_LOGOUT_URL = "SSO_LOGOUT_URL";
    
    /**
     * buc sso 用户名
     */
    public static final String BUC_SSO_USERNAME = "buc_sso_username";

    /**
	 * 操作记录页面默认页面记录显示条数
	 */
	public static final Integer OPRATION_RECORDS_PAGE_SIZE = 100;
	
}

package com.alibaba.dubbo.governance.web.common.pulltool;

import com.alibaba.citrus.service.pull.ToolFactory;
import com.alibaba.dubbo.governance.service.OverrideService;
import com.alibaba.dubbo.governance.service.RouteService;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * PullTool for accessing message bundle.
 *
 * @author gerry
 */
public class ToolUtil implements ToolFactory {
    @Autowired
    OverrideService overrideService;

    @Autowired
    RouteService routeService;
    private boolean singleton = false; //应该是global范围的对象！！

    public Object createTool() throws Exception {
        Tool tool = new Tool();
        tool.setOverrideService(overrideService);
        tool.setRouteService(routeService);
        return tool;
    }

    public boolean isSingleton() {
        return this.singleton;
    }

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

}

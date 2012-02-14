/**
 * File Created at 2012-02-13
 * $Id$
 *
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.rpc.cluster.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class Menu {
    
    private Map<String, List<String>> menus = new HashMap<String, List<String>>();
    
    public Menu() {}
    
    public Menu( Map<String, List<String>> menus ) {
        this.menus.putAll( menus );
    }
    
    public void putMenuItem( String menu, String item ) {
        List<String> items = menus.get( menu );
        if ( item == null ) {
            items = new ArrayList<String>();
            menus.put( menu, items );
        }
        items.add( item );
    }
    
    public void addMenu( String menu, List<String> items ) {
        List<String> menuItems = menus.get( menu );
        if ( menuItems == null ) {
            menus.put( menu, new ArrayList<String>( items ) );
        } else {
            menuItems.addAll( new ArrayList<String>( items ) );
        }
    }
    
    public Map<String, List<String>> getMenus() {
        return Collections.unmodifiableMap( menus );
    }
    
    public void merge( Menu menu ) {
        for( Map.Entry<String, List<String>> entry : menu.menus.entrySet() ) {
            addMenu( entry.getKey(), entry.getValue() );
        }
    }

}

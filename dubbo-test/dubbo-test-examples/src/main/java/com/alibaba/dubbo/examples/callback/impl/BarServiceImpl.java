package com.alibaba.dubbo.examples.callback.impl;

import com.alibaba.dubbo.examples.callback.api.BarService;
import com.alibaba.dubbo.examples.callback.api.CallbackListener;

/**
 * Created by tanhua on 16/4/15.
 */
public class BarServiceImpl implements BarService {

    @Override
    public void asyncCallInBar(final String key, final CallbackListener listener) {
        new Thread(new Runnable(){
        	@Override
        	public void run() {
        		try {
					Thread.sleep(500);//delay
				} catch (Exception e) {}
        		
        		listener.changed(key + ", this is bar!");
        	}
        }).start();
    }
}

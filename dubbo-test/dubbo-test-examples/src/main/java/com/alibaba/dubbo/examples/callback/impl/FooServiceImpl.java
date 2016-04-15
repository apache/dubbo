package com.alibaba.dubbo.examples.callback.impl;

import com.alibaba.dubbo.examples.callback.api.BarService;
import com.alibaba.dubbo.examples.callback.api.CallbackListener;
import com.alibaba.dubbo.examples.callback.api.FooService;

/**
 * Created by tanhua on 16/4/15.
 */
// @Service("fooService")
public class FooServiceImpl implements FooService {

	// @Resource(name = "barService")
	BarService barService;

	public FooServiceImpl(BarService barService) {
		super();
		this.barService = barService;
	}

	@Override
	public void asyncCallInFoo(final String key, final CallbackListener listener) {
		barService.asyncCallInBar("foo", new CallbackListener() {
			@Override
			public void changed(String msg) {
				listener.changed(key + ", this is foo! msg:" +msg);
			}
		});

	}
}

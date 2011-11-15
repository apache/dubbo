/*
 * Copyright 1999-2011 Alibaba Group.
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
package com.alibaba.dubbo.rpc.support;

import com.alibaba.dubbo.rpc.CustomArgument;


/**
 * <code>TestService</code>
 */

public interface DemoService
{
	void sayHello(String name);

	String echo(String text);

	long timestamp();

	String getThreadName();

	int getSize(String[] strs);

	int getSize(Object[] os);

	Object invoke(String service, String method) throws Exception;

	int stringLength(String str);

	Type enumlength(Type... types);
	
//	Type enumlength(Type type);
	
	String get(CustomArgument arg1);
	
	byte getbyte(byte arg);
	
}
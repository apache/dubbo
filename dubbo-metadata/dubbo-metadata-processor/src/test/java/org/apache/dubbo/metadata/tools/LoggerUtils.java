/*
 * Copyright (C) 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.metadata.tools;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.RpcContext;

/**
 * Logger Utilities
 */
public abstract class LoggerUtils {

	private static final Logger logger = LoggerFactory.getLogger(LoggerUtils.class);

	public static void log(String url, Object result) {
		String message = String
				.format("The client[%s] uses '%s' protocol to call %s : %s",
						RpcContext.getContext().getRemoteHostName(),
						RpcContext.getContext().getUrl() == null ? "N/A"
								: RpcContext.getContext().getUrl().getProtocol(),
						url, result);
		if (logger.isInfoEnabled()) {
			logger.info(message);
		}
	}
}

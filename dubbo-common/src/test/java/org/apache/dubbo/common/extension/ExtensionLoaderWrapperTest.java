/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.extension;

import java.io.IOException;
import java.util.List;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.wrapper.HelloSPI;
import org.junit.jupiter.api.Test;

public class ExtensionLoaderWrapperTest {

	@Test
	public  void test_wrapper() throws IOException {
		ExtensionLoader<HelloSPI> spi = ExtensionLoader.getExtensionLoader(HelloSPI.class);

		URL url = URL.valueOf("duboo://yzh.com/org.apache.dubbo.common.extension.wrapper.HelloSPI?names=dog");
		List<HelloSPI> activateExtension = spi.getActivateExtension(url, "names", "comsumer");
		activateExtension.forEach((s) -> s.say("Hello"));

	}

}

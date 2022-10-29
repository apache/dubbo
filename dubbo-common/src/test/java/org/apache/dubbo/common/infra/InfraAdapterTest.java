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
package org.apache.dubbo.common.infra;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.EnvironmentConfigurationTest;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.infra.support.EnvironmentAdapter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_ENV_KEYS;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_LABELS;


public class InfraAdapterTest extends EnvironmentConfigurationTest {

    @Test
    public void test() throws Exception {
        try {
            // spi test
            List<InfraAdapter> infraAdapters = ExtensionLoader.getExtensionLoader(InfraAdapter.class).getActivateExtension(URL.valueOf("test://0.0.0.0/"), "");
            Assertions.assertEquals(infraAdapters.size(), 1);
            InfraAdapter infraAdapter = infraAdapters.get(0);
            Assertions.assertTrue(infraAdapter instanceof EnvironmentAdapter);

            // add system env
            HashMap<String, String> map = new HashMap<>();
            map.put(DUBBO_LABELS, "k1=v1;k2=v2");
            setEnv(map);

            // add system properties
            System.setProperty(DUBBO_ENV_KEYS, "k3,k4");
            System.setProperty("k3", "v3");
            System.setProperty("k4", "v4");

            // get key & value
            Map<String, String> extraAttributes = infraAdapter.getExtraAttributes(null);
            Assertions.assertEquals(extraAttributes.size(), 4);
            for (Map.Entry<String, String> entry : extraAttributes.entrySet()) {
                System.out.println(entry.getKey() + "," + entry.getValue());
            }
        } finally {

            System.getProperties().remove(DUBBO_ENV_KEYS);
        }
    }

}

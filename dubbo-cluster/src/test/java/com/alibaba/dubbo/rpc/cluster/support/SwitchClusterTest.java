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
package com.alibaba.dubbo.rpc.cluster.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Directory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * @author ding.lid
 */
public class SwitchClusterTest {
    static Invoker<String> createInvoker(final Boolean available, final Boolean connected, final Integer invokerCount) {
        return new Invoker<String>() {
            public Class<String> getInterface() {
                return null;
            }

            public Result invoke(Invocation invocation) throws RpcException {
                return null;
            }

            public URL getUrl() {
                StringBuilder sb = new StringBuilder("dubbo://1.2.3.4:20880?k1=v1");
                if(connected != null) {
                    sb.append("&connected=" + connected);
                }
                if(invokerCount != null) {
                    sb.append("&" + Constants.INVOKER_INSIDE_INVOKER_COUNT_KEY +
                            "=" + invokerCount);
                }
                return URL.valueOf(sb.toString());
            }

            public boolean isAvailable() {
                return available;
            }

            public void destroy() {
            }
        };
    }

    static List<Invoker<String>> createInvokerList(Object[][] paramters) {
        List<Invoker<String>> ret = new ArrayList<Invoker<String>>();

        for(Object[] paramter : paramters) {
            ret.add(createInvoker((Boolean)(paramter[0]), (Boolean)(paramter[1]), (Integer)(paramter[2])));
        }

        return ret;
    }

    @Test
    public void test_getEffectiveInvokers() throws Exception {
        {
            // 全部OK
            List<Invoker<String>>  data = createInvokerList(new Object[][]{
                    {true, true, 1},
                    {true, true, 1},
                    {true, true, 1},
                    {true, true, 1},
            });

            List<Invoker<String>> result = SwitchCluster.getEffectiveInvokers(data);
            assertEquals(data, result);
        }
        {
            // 有connected=false
            List<Invoker<String>>  data = createInvokerList(new Object[][]{
                    {true, true, 1},
                    {true, false, 1},
                    {true, false, 1},
                    {true, true, 1},
            });

            List<Invoker<String>> result = SwitchCluster.getEffectiveInvokers(data);
            assertEquals(2, result.size());
            assertSame(data.get(0), result.get(0));
            assertSame(data.get(3), result.get(1));
        }
        {
            // 有available = false
            List<Invoker<String>>  data = createInvokerList(new Object[][]{
                    {true, true, 1},
                    {false, true, 1},
                    {false, true, 1},
                    {true, true, 1},
            });

            List<Invoker<String>> result = SwitchCluster.getEffectiveInvokers(data);
            assertEquals(2, result.size());
            assertSame(data.get(0), result.get(0));
            assertSame(data.get(3), result.get(1));
        }
        {
            // 有connected=false & available = false
            List<Invoker<String>>  data = createInvokerList(new Object[][]{
                    {true, true, 1},
                    {true, false, 1},
                    {false, true, 1},
                    {true, true, 1},
            });

            List<Invoker<String>> result = SwitchCluster.getEffectiveInvokers(data);
            assertEquals(2, result.size());
            assertSame(data.get(0), result.get(0));
            assertSame(data.get(3), result.get(1));
        }
        {
            // 有connected全 false
            List<Invoker<String>>  data = createInvokerList(new Object[][]{
                    {true, false, 1},
                    {false, false, 1},
                    {false, false, 1},
                    {true, false, 1},
            });

            List<Invoker<String>> result = SwitchCluster.getEffectiveInvokers(data);
            assertEquals(2, result.size());
            assertSame(data.get(0), result.get(0));
            assertSame(data.get(3), result.get(1));
        }
    }

    Directory directory = new Directory<String>() {

        public Class<String> getInterface() {
            return null;
        }

        public List<Invoker<String>> list(Invocation invocation) throws RpcException {
            return null;
        }

        public URL getUrl() {
            return URL.valueOf("registry://1.2.2.3");
        }

        public boolean isAvailable() {
            return true;
        }

        public void destroy() {
        }
    };

    @Test
    public void test_getSuitableInvoker() throws Exception {
        {
            List<Invoker<String>>  data = createInvokerList(new Object[][]{
                    {true, true, 100},
                    {true, true, 1},
                    {true, true, 1},
                    {true, true, 1},
            });

            Invoker<String> result = SwitchCluster.getSuitableInvoker(data, directory);
            assertSame(data.get(0), result);
        }
        {
            List<Invoker<String>>  data = createInvokerList(new Object[][]{
                    {true, true, 1},
                    {true, true, 2},
                    {true, true, 3},
                    {true, true, 100},
            });

            Invoker<String> result = SwitchCluster.getSuitableInvoker(data, directory);
            assertSame(data.get(3), result);
        }
        {
            List<Invoker<String>>  data = createInvokerList(new Object[][]{
                    {true, true, 1},
                    {true, true, 2},
                    {true, true, 3},
                    {true, true, 4},
            });

            Invoker<String> result = SwitchCluster.getSuitableInvoker(data, directory);
            assertSame(data.get(2), result);
        }
    }

    @Test
    public void test_getSuitableInvoker_noInput() throws Exception {
        try {
            SwitchCluster.getSuitableInvoker(new ArrayList<Invoker<String>>(), directory);
            fail();
        }
        catch (RpcException expected) {
            assertThat(expected.getMessage(),
                    containsString("No provider available in"));
        }
    }
}

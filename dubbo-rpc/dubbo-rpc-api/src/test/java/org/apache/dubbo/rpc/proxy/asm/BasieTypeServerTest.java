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
package org.apache.dubbo.rpc.proxy.asm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class BasieTypeServerTest {

    Wrapper reflect = new Wrapper();
    BasieTypeServer remote, proxyRemote;
    AbstractAsmProxy asm;
    Random random = new Random();

    @BeforeEach
    public void before() throws Exception {
        Invoker<?> mockInvoker = Mockito.mock(Invoker.class);
        Result mockResult = Mockito.mock(Result.class);
        Mockito.when(mockInvoker.invoke(Mockito.any())).thenReturn(mockResult);
        Class<?>[] clazzArray = new Class<?>[]{BasieTypeServer.class};
        proxyRemote = reflect.getProxy(clazzArray, mockInvoker);
        asm = (AbstractAsmProxy) Mockito.spy(proxyRemote);
        remote = (BasieTypeServer) asm;
    }

    @Test
    public void test() throws Throwable {
        Map<String, MethodExecute<?>> map = reflect.getInvoke(remote, proxyRemote.getClass());
        Iterator<Entry<String, MethodExecute<?>>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, MethodExecute<?>> entry = it.next();
            MethodStatement methodStatement = reflect.getMethodStatement(entry.getKey());
            Object[] objcet = TestCommonUtils.getParmameter(methodStatement.getParameterClass());
            Object value = TestCommonUtils.getValue((Class<?>) methodStatement.getReturnType());
            Mockito.doAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    try {
                        Object[] args = invocation.getArgument(1);
                        if (args != null) {
                            for (int i = 0; i < args.length; i++) {
                                assertEquals(args[i], objcet[i]);
                            }
                        } else {
                            assertEquals(args, objcet);
                        }
                        invocation.getMock();
                        return value;
                    } catch (Throwable e) {
                        throw e;
                    }
                }
            }).when(asm).doInvoke(Mockito.any(), Mockito.any());
            if (methodStatement.getReturnType() == null || void.class.equals(methodStatement.getReturnType())) {
                entry.getValue().execute(objcet);
            } else {
                assertEquals(entry.getValue().execute(objcet), value);
            }
        }
    }

    @Test
    public void returnVoid() {
        String str = "returnVoid";
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                MethodStatement ms = invocation.getArgument(0);
                assertEquals(ms.getMethod(), str);
                assertNull(invocation.getArgument(1));
                invocation.getMock();
                return null;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        remote.returnVoid();
    }

    @Test
    public void returnBoolean() {
        String str = "returnBoolean";
        Mockito.doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                MethodStatement ms = invocation.getArgument(0);
                assertEquals(ms.getMethod(), str);
                assertNull(invocation.getArgument(1));
                invocation.getMock();
                return true;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertTrue(remote.returnBoolean());
    }

    @Test
    public void returnChar() {
        String str = "returnChar";
        Mockito.doAnswer(new Answer<Character>() {
            @Override
            public Character answer(InvocationOnMock invocation) throws Throwable {
                MethodStatement ms = invocation.getArgument(0);
                assertEquals(ms.getMethod(), str);
                assertNull(invocation.getArgument(1));
                invocation.getMock();
                return 'a';
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.returnChar(), 'a');
    }

    @Test
    public void returnByte() {
        String str = "returnByte";
        Mockito.doAnswer(new Answer<Byte>() {
            @Override
            public Byte answer(InvocationOnMock invocation) throws Throwable {
                MethodStatement ms = invocation.getArgument(0);
                assertEquals(ms.getMethod(), str);
                assertNull(invocation.getArgument(1));
                invocation.getMock();
                return 127;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.returnByte(), 127);
    }

    @Test
    public void returnShort() {
        String str = "returnShort";
        Mockito.doAnswer(new Answer<Short>() {
            @Override
            public Short answer(InvocationOnMock invocation) throws Throwable {
                MethodStatement ms = invocation.getArgument(0);
                assertEquals(ms.getMethod(), str);
                assertNull(invocation.getArgument(1));
                invocation.getMock();
                return 1271;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.returnShort(), 1271);
    }

    @Test
    public void returnInt() {
        String str = "returnInt";
        Mockito.doAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                MethodStatement ms = invocation.getArgument(0);
                assertEquals(ms.getMethod(), str);
                assertNull(invocation.getArgument(1));
                invocation.getMock();
                return 127112;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.returnInt(), 127112);
    }

    @Test
    public void returnLong() {
        String str = "returnLong";
        Mockito.doAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                MethodStatement ms = invocation.getArgument(0);
                assertEquals(ms.getMethod(), str);
                assertNull(invocation.getArgument(1));
                invocation.getMock();
                return 127112L;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.returnLong(), 127112L);
    }

    @Test
    public void returnFloat() {
        String str = "returnFloat";
        Mockito.doAnswer(new Answer<Float>() {
            @Override
            public Float answer(InvocationOnMock invocation) throws Throwable {
                MethodStatement ms = invocation.getArgument(0);
                assertEquals(ms.getMethod(), str);
                assertNull(invocation.getArgument(1));
                invocation.getMock();
                return 127.127F;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.returnFloat(), 127.127F);
    }

    @Test
    public void returnDouble() {
        String str = "returnDouble";
        Mockito.doAnswer(new Answer<Double>() {
            @Override
            public Double answer(InvocationOnMock invocation) throws Throwable {
                MethodStatement ms = invocation.getArgument(0);
                assertEquals(ms.getMethod(), str);
                assertNull(invocation.getArgument(1));
                invocation.getMock();
                return 127.127D;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.returnDouble(), 127.127D);
    }

    @Test
    public void returnIntArray() {
        String str = "returnIntArray";
        int[] value = new int[]{random.nextInt()};
        Mockito.doAnswer(new Answer<int[]>() {
            @Override
            public int[] answer(InvocationOnMock invocation) throws Throwable {
                MethodStatement ms = invocation.getArgument(0);
                assertEquals(ms.getMethod(), str);
                assertNull(invocation.getArgument(1));
                invocation.getMock();
                return value;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.returnIntArray(), value);
    }

    @Test
    public void returnLongArray() {
        String str = "returnLongArray";
        long[] value = new long[]{random.nextLong()};
        Mockito.doAnswer(new Answer<long[]>() {
            @Override
            public long[] answer(InvocationOnMock invocation) throws Throwable {
                MethodStatement ms = invocation.getArgument(0);
                assertEquals(ms.getMethod(), str);
                assertNull(invocation.getArgument(1));
                invocation.getMock();
                return value;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.returnLongArray(), value);
    }

    @Test
    public void returnObject() {
        String str = "returnObject";
        Object value = new Object();
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MethodStatement ms = invocation.getArgument(0);
                assertEquals(ms.getMethod(), str);
                assertNull(invocation.getArgument(1));
                invocation.getMock();
                return value;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.returnObject(), value);
    }

    @Test
    public void returnObjectArray() {
        String str = "returnObjectArray";
        Object[] value = new Object[]{new Object()};
        Mockito.doAnswer(new Answer<Object[]>() {
            @Override
            public Object[] answer(InvocationOnMock invocation) throws Throwable {
                MethodStatement ms = invocation.getArgument(0);
                assertEquals(ms.getMethod(), str);
                assertNull(invocation.getArgument(1));
                invocation.getMock();
                return value;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.returnObjectArray(), value);
    }

    @Test
    public void parameterBoolean() {
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], true);
                invocation.getMock();
                return null;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        remote.parameterBoolean(true);
    }

    @Test
    public void parameterChar() {
        char value = 'a';
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return null;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        remote.parameterChar(value);
    }

    @Test
    public void parameterByte() {
        byte value = 'a';
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return null;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        remote.parameterByte(value);
    }

    @Test
    public void parameterShort() {
        short value = (short) random.nextInt();
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return null;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        remote.parameterShort(value);
    }

    @Test
    public void parameterInt() {
        int value = random.nextInt();
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return null;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        remote.parameterInt(value);
    }

    @Test
    public void parameterLong() {
        long value = random.nextLong();
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return null;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        remote.parameterLong(value);
    }

    @Test
    public void parameterFloat() {
        float value = random.nextFloat();
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return null;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        remote.parameterFloat(value);
    }

    @Test
    public void parameterDouble() {
        double value = random.nextDouble();
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return null;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        remote.parameterDouble(value);
    }

    @Test
    public void parameterIntArray() {
        int[] value = new int[]{random.nextInt(), random.nextInt()};
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return null;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        remote.parameterIntArray(value);
    }

    @Test
    public void parameterLongArray() {
        long[] value = new long[]{random.nextInt(), random.nextInt()};
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return null;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        remote.parameterLongArray(value);
    }

    @Test
    public void parameterObject() {
        float value = random.nextFloat();
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return null;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        remote.parameterObject(value);
    }

    @Test
    public void parameterObjectArray() {
        Object[] value = new Object[]{random.nextInt(), random.nextInt()};
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return null;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        remote.parameterObjectArray(value);
    }

    @Test
    public void parameterAndReturnBoolean() {
        boolean value = true;
        Mockito.doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return true;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertTrue(remote.parameterAndReturnBoolean(value));
    }

    @Test
    public void parameterAndReturnChar() {
        char value = 'a';
        Mockito.doAnswer(new Answer<Character>() {
            @Override
            public Character answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return value;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.parameterAndReturnChar(value), value);
    }

    @Test
    public void parameterAndReturnByte() {
        byte value = (byte) random.nextInt();
        Mockito.doAnswer(new Answer<Byte>() {
            @Override
            public Byte answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return value;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.parameterAndReturnByte(value), value);
    }

    @Test
    public void parameterAndReturnShort() {
        short value = (short) random.nextInt();
        Mockito.doAnswer(new Answer<Short>() {
            @Override
            public Short answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return value;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.parameterAndReturnShort(value), value);
    }

    @Test
    public void parameterAndReturnInt() {
        int value = random.nextInt();
        Mockito.doAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return value;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.parameterAndReturnInt(value), value);
    }

    @Test
    public void parameterAndReturnLong() {
        long value = random.nextLong();
        Mockito.doAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return value;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.parameterAndReturnLong(value), value);
    }

    @Test
    public void parameterAndReturnFloat() {
        float value = random.nextFloat();
        Mockito.doAnswer(new Answer<Float>() {
            @Override
            public Float answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return value;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.parameterAndReturnFloat(value), value);
    }

    @Test
    public void parameterAndReturnDouble() {
        double value = random.nextDouble();
        Mockito.doAnswer(new Answer<Double>() {
            @Override
            public Double answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return value;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.parameterAndReturnDouble(value), value);
    }

    @Test
    public void parameterAndReturnIntArray() {
        int[] value = new int[]{random.nextInt(), random.nextInt()};
        Mockito.doAnswer(new Answer<int[]>() {
            @Override
            public int[] answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return value;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.parameterAndReturnIntArray(value), value);
    }

    @Test
    public void parameterAndReturnLongArray() {
        long[] value = new long[]{random.nextInt(), random.nextInt()};
        Mockito.doAnswer(new Answer<long[]>() {
            @Override
            public long[] answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return value;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.parameterAndReturnLongArray(value), value);
    }

    @Test
    public void parameterAndReturnObject() {
        double value = random.nextDouble();
        Mockito.doAnswer(new Answer<Double>() {
            @Override
            public Double answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return value;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.parameterAndReturnObject(value), value);
    }

    @Test
    public void parameterAndReturnObjectArray() {
        Object[] value = new Object[]{random.nextInt(), random.nextInt()};
        Mockito.doAnswer(new Answer<Object[]>() {
            @Override
            public Object[] answer(InvocationOnMock invocation) throws Throwable {
                Object[] object = invocation.getArgument(1);
                assertEquals(object[0], value);
                invocation.getMock();
                return value;
            }
        }).when(asm).doInvoke(Mockito.any(), Mockito.any());
        assertEquals(remote.parameterAndReturnObjectArray(value), value);
    }
}

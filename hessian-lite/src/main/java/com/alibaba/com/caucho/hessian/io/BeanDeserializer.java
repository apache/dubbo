/*
 * Copyright (c) 2001-2008 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Burlap", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson
 */

package com.alibaba.com.caucho.hessian.io;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

/**
 * Serializing an object for known object types.
 */
public class BeanDeserializer extends AbstractMapDeserializer {
    private Class _type;
    private HashMap _methodMap;
    private Method _readResolve;
    private Constructor _constructor;
    private Object[] _constructorArgs;

    public BeanDeserializer(Class cl) {
        _type = cl;
        _methodMap = getMethodMap(cl);

        _readResolve = getReadResolve(cl);

        Constructor[] constructors = cl.getConstructors();
        int bestLength = Integer.MAX_VALUE;

        for (int i = 0; i < constructors.length; i++) {
            if (constructors[i].getParameterTypes().length < bestLength) {
                _constructor = constructors[i];
                bestLength = _constructor.getParameterTypes().length;
            }
        }

        if (_constructor != null) {
            _constructor.setAccessible(true);
            Class[] params = _constructor.getParameterTypes();
            _constructorArgs = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                _constructorArgs[i] = getParamArg(params[i]);
            }
        }
    }

    /**
     * Creates a map of the classes fields.
     */
    protected static Object getParamArg(Class cl) {
        if (!cl.isPrimitive())
            return null;
        else if (boolean.class.equals(cl))
            return Boolean.FALSE;
        else if (byte.class.equals(cl))
            return Byte.valueOf((byte) 0);
        else if (short.class.equals(cl))
            return Short.valueOf((short) 0);
        else if (char.class.equals(cl))
            return Character.valueOf((char) 0);
        else if (int.class.equals(cl))
            return Integer.valueOf(0);
        else if (long.class.equals(cl))
            return Long.valueOf(0);
        else if (float.class.equals(cl))
            return Double.valueOf(0);
        else if (double.class.equals(cl))
            return Double.valueOf(0);
        else
            throw new UnsupportedOperationException();
    }

    public Class getType() {
        return _type;
    }

    public Object readMap(AbstractHessianInput in)
            throws IOException {
        try {
            Object obj = instantiate();

            return readMap(in, obj);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOExceptionWrapper(e);
        }
    }

    public Object readMap(AbstractHessianInput in, Object obj)
            throws IOException {
        try {
            int ref = in.addRef(obj);

            while (!in.isEnd()) {
                Object key = in.readObject();

                Method method = (Method) _methodMap.get(key);

                if (method != null) {
                    Object value = in.readObject(method.getParameterTypes()[0]);

                    method.invoke(obj, new Object[]{value});
                } else {
                    Object value = in.readObject();
                }
            }

            in.readMapEnd();

            Object resolve = resolve(obj);

            if (obj != resolve)
                in.setRef(ref, resolve);

            return resolve;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOExceptionWrapper(e);
        }
    }

    private Object resolve(Object obj) {
        // if there's a readResolve method, call it
        try {
            if (_readResolve != null)
                return _readResolve.invoke(obj, new Object[0]);
        } catch (Exception e) {
        }

        return obj;
    }

    protected Object instantiate()
            throws Exception {
        return _constructor.newInstance(_constructorArgs);
    }

    /**
     * Returns the readResolve method
     */
    protected Method getReadResolve(Class cl) {
        for (; cl != null; cl = cl.getSuperclass()) {
            Method[] methods = cl.getDeclaredMethods();

            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];

                if (method.getName().equals("readResolve") &&
                        method.getParameterTypes().length == 0)
                    return method;
            }
        }

        return null;
    }

    /**
     * Creates a map of the classes fields.
     */
    protected HashMap getMethodMap(Class cl) {
        HashMap methodMap = new HashMap();

        for (; cl != null; cl = cl.getSuperclass()) {
            Method[] methods = cl.getDeclaredMethods();

            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];

                if (Modifier.isStatic(method.getModifiers()))
                    continue;

                String name = method.getName();

                if (!name.startsWith("set"))
                    continue;

                Class[] paramTypes = method.getParameterTypes();
                if (paramTypes.length != 1)
                    continue;

                if (!method.getReturnType().equals(void.class))
                    continue;

                if (findGetter(methods, name, paramTypes[0]) == null)
                    continue;

                // XXX: could parameterize the handler to only deal with public
                try {
                    method.setAccessible(true);
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                name = name.substring(3);

                int j = 0;
                for (; j < name.length() && Character.isUpperCase(name.charAt(j)); j++) {
                }

                if (j == 1)
                    name = name.substring(0, j).toLowerCase() + name.substring(j);
                else if (j > 1)
                    name = name.substring(0, j - 1).toLowerCase() + name.substring(j - 1);


                methodMap.put(name, method);
            }
        }

        return methodMap;
    }

    /**
     * Finds any matching setter.
     */
    private Method findGetter(Method[] methods, String setterName, Class arg) {
        String getterName = "get" + setterName.substring(3);

        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];

            if (!method.getName().equals(getterName))
                continue;

            if (!method.getReturnType().equals(arg))
                continue;

            Class[] params = method.getParameterTypes();

            if (params.length == 0)
                return method;
        }

        return null;
    }
}

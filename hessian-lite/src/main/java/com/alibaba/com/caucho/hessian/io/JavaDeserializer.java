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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Serializing an object for known object types.
 */
public class JavaDeserializer extends AbstractMapDeserializer {
    private static final Logger log
        = Logger.getLogger(JavaDeserializer.class.getName());

    private Class _type;
    private HashMap _fieldMap;
    private Method _readResolve;
    private Constructor _constructor;
    private Object[] _constructorArgs;

    public JavaDeserializer(Class cl) {
        _type = cl;
        _fieldMap = getFieldMap(cl);

        _readResolve = getReadResolve(cl);

        if (_readResolve != null) {
            _readResolve.setAccessible(true);
        }

        Constructor[] constructors = cl.getDeclaredConstructors();
        long bestCost = Long.MAX_VALUE;

        for (int i = 0; i < constructors.length; i++) {
            Class[] param = constructors[i].getParameterTypes();
            long cost = 0;

            for (int j = 0; j < param.length; j++) {
                cost = 4 * cost;

                if (Object.class.equals(param[j]))
                    cost += 1;
                else if (String.class.equals(param[j]))
                    cost += 2;
                else if (int.class.equals(param[j]))
                    cost += 3;
                else if (long.class.equals(param[j]))
                    cost += 4;
                else if (param[j].isPrimitive())
                    cost += 5;
                else
                    cost += 6;
            }

            if (cost < 0 || cost > (1 << 48))
                cost = 1 << 48;

            cost += (long) param.length << 48;

            if (cost < bestCost) {
                _constructor = constructors[i];
                bestCost = cost;
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
            return new Byte((byte) 0);
        else if (short.class.equals(cl))
            return new Short((short) 0);
        else if (char.class.equals(cl))
            return new Character((char) 0);
        else if (int.class.equals(cl))
            return Integer.valueOf(0);
        else if (long.class.equals(cl))
            return Long.valueOf(0);
        else if (float.class.equals(cl))
            return Float.valueOf(0);
        else if (double.class.equals(cl))
            return Double.valueOf(0);
        else
            throw new UnsupportedOperationException();
    }

    static void logDeserializeError(Field field, Object obj, Object value,
        Throwable e)
        throws IOException {
        String fieldName = (field.getDeclaringClass().getName()
            + "." + field.getName());

        if (e instanceof HessianFieldException)
            throw (HessianFieldException) e;
        else if (e instanceof IOException)
            throw new HessianFieldException(fieldName + ": " + e.getMessage(), e);

        if (value != null)
            throw new HessianFieldException(fieldName + ": " + value.getClass().getName() + " (" + value + ")"
                + " cannot be assigned to '" + field.getType().getName() + "'", e);
        else
            throw new HessianFieldException(fieldName + ": " + field.getType().getName() + " cannot be assigned from null", e);
    }

    @Override
    public Class getType() {
        return _type;
    }

    @Override
    public Object readMap(AbstractHessianInput in)
        throws IOException {
        try {
            Object obj = instantiate();

            return readMap(in, obj);
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOExceptionWrapper(_type.getName() + ":" + e.getMessage(), e);
        }
    }

    @Override
    public Object readObject(AbstractHessianInput in, String[] fieldNames)
        throws IOException {
        try {
            Object obj = instantiate();

            return readObject(in, obj, fieldNames);
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOExceptionWrapper(_type.getName() + ":" + e.getMessage(), e);
        }
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

    public Object readMap(AbstractHessianInput in, Object obj)
        throws IOException {
        try {
            int ref = in.addRef(obj);

            while (!in.isEnd()) {
                Object key = in.readObject();

                FieldDeserializer deser = (FieldDeserializer) _fieldMap.get(key);

                if (deser != null)
                    deser.deserialize(in, obj);
                else
                    in.readObject();
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

    public Object readObject(AbstractHessianInput in,
        Object obj,
        String[] fieldNames)
        throws IOException {
        try {
            int ref = in.addRef(obj);

            for (int i = 0; i < fieldNames.length; i++) {
                String name = fieldNames[i];

                FieldDeserializer deser = (FieldDeserializer) _fieldMap.get(name);

                if (deser != null)
                    deser.deserialize(in, obj);
                else
                    in.readObject();
            }

            Object resolve = resolve(obj);

            if (obj != resolve)
                in.setRef(ref, resolve);

            return resolve;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOExceptionWrapper(obj.getClass().getName() + ":" + e, e);
        }
    }

    private Object resolve(Object obj)
        throws Exception {
        // if there's a readResolve method, call it
        try {
            if (_readResolve != null)
                return _readResolve.invoke(obj, new Object[0]);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() != null)
                throw e;
        }

        return obj;
    }

    protected Object instantiate()
        throws Exception {
        try {
            if (_constructor != null)
                return _constructor.newInstance(_constructorArgs);
            else
                return _type.newInstance();
        } catch (Exception e) {
            throw new HessianProtocolException("'" + _type.getName() + "' could not be instantiated", e);
        }
    }

    /**
     * Creates a map of the classes fields.
     */
    protected HashMap getFieldMap(Class cl) {
        HashMap fieldMap = new HashMap();

        for (; cl != null; cl = cl.getSuperclass()) {
            Field[] fields = cl.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];

                if (Modifier.isTransient(field.getModifiers())
                    || Modifier.isStatic(field.getModifiers()))
                    continue;
                else if (fieldMap.get(field.getName()) != null)
                    continue;

                // XXX: could parameterize the handler to only deal with public
                try {
                    field.setAccessible(true);
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                Class type = field.getType();
                FieldDeserializer deser;

                if (String.class.equals(type))
                    deser = new StringFieldDeserializer(field);
                else if (byte.class.equals(type)) {
                    deser = new ByteFieldDeserializer(field);
                } else if (short.class.equals(type)) {
                    deser = new ShortFieldDeserializer(field);
                } else if (int.class.equals(type)) {
                    deser = new IntFieldDeserializer(field);
                } else if (long.class.equals(type)) {
                    deser = new LongFieldDeserializer(field);
                } else if (float.class.equals(type)) {
                    deser = new FloatFieldDeserializer(field);
                } else if (double.class.equals(type)) {
                    deser = new DoubleFieldDeserializer(field);
                } else if (boolean.class.equals(type)) {
                    deser = new BooleanFieldDeserializer(field);
                } else if (java.sql.Date.class.equals(type)) {
                    deser = new SqlDateFieldDeserializer(field);
                } else if (java.sql.Timestamp.class.equals(type)) {
                    deser = new SqlTimestampFieldDeserializer(field);
                } else if (java.sql.Time.class.equals(type)) {
                    deser = new SqlTimeFieldDeserializer(field);
                }
                // support generic type of map
                else if(Map.class.equals(type)
                    && field.getGenericType() != field.getType()){
                    deser = new ObjectMapFieldDeserializer(field);
                }
                else if(List.class.equals(type)
                    && field.getGenericType() != field.getType()){
                    deser = new ObjectListFieldDeserializer(field);
                }
                else {
                    deser = new ObjectFieldDeserializer(field);
                }

                fieldMap.put(field.getName(), deser);
            }
        }

        return fieldMap;
    }

    abstract static class FieldDeserializer {
        abstract void deserialize(AbstractHessianInput in, Object obj)
            throws IOException;
    }

    static class ObjectFieldDeserializer extends FieldDeserializer {
        private final Field _field;

        ObjectFieldDeserializer(Field field) {
            _field = field;
        }

        @Override
        void deserialize(AbstractHessianInput in, Object obj)
            throws IOException {
            Object value = null;

            try {
                value = in.readObject(_field.getType());

                _field.set(obj, value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class BooleanFieldDeserializer extends FieldDeserializer {
        private final Field _field;

        BooleanFieldDeserializer(Field field) {
            _field = field;
        }

        @Override
        void deserialize(AbstractHessianInput in, Object obj)
            throws IOException {
            boolean value = false;

            try {
                value = in.readBoolean();

                _field.setBoolean(obj, value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class ByteFieldDeserializer extends FieldDeserializer {
        private final Field _field;

        ByteFieldDeserializer(Field field) {
            _field = field;
        }

        @Override
        void deserialize(AbstractHessianInput in, Object obj)
            throws IOException {
            int value = 0;

            try {
                value = in.readInt();

                _field.setByte(obj, (byte) value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class ShortFieldDeserializer extends FieldDeserializer {
        private final Field _field;

        ShortFieldDeserializer(Field field) {
            _field = field;
        }

        @Override
        void deserialize(AbstractHessianInput in, Object obj)
            throws IOException {
            int value = 0;

            try {
                value = in.readInt();

                _field.setShort(obj, (short) value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class ObjectMapFieldDeserializer extends FieldDeserializer {
        private final Field _field;

        ObjectMapFieldDeserializer(Field field) {
            _field = field;
        }

        @Override
        void deserialize(AbstractHessianInput in, Object obj)
            throws IOException {
            Object value = null;

            try {

                Type[] types = ((ParameterizedType)_field.getGenericType()).getActualTypeArguments();
                value = in.readObject(_field.getType(),
                        isPrimitive(types[0]) ? (Class<?>)types[0] : null,
                        isPrimitive(types[1]) ? (Class<?>)types[1] : null
                    );

                _field.set(obj, value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class ObjectListFieldDeserializer extends FieldDeserializer {
        private final Field _field;

        ObjectListFieldDeserializer(Field field) {
            _field = field;
        }

        @Override
        void deserialize(AbstractHessianInput in, Object obj)
            throws IOException {
            Object value = null;

            try {

                Type[] types = ((ParameterizedType)_field.getGenericType()).getActualTypeArguments();
                value = in.readObject(_field.getType(),
                    isPrimitive(types[0]) ? (Class<?>)types[0] : null
                );

                _field.set(obj, value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class IntFieldDeserializer extends FieldDeserializer {
        private final Field _field;

        IntFieldDeserializer(Field field) {
            _field = field;
        }

        @Override
        void deserialize(AbstractHessianInput in, Object obj)
            throws IOException {
            int value = 0;

            try {
                value = in.readInt();

                _field.setInt(obj, value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class LongFieldDeserializer extends FieldDeserializer {
        private final Field _field;

        LongFieldDeserializer(Field field) {
            _field = field;
        }

        @Override
        void deserialize(AbstractHessianInput in, Object obj)
            throws IOException {
            long value = 0;

            try {
                value = in.readLong();

                _field.setLong(obj, value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class FloatFieldDeserializer extends FieldDeserializer {
        private final Field _field;

        FloatFieldDeserializer(Field field) {
            _field = field;
        }

        @Override
        void deserialize(AbstractHessianInput in, Object obj)
            throws IOException {
            double value = 0;

            try {
                value = in.readDouble();

                _field.setFloat(obj, (float) value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class DoubleFieldDeserializer extends FieldDeserializer {
        private final Field _field;

        DoubleFieldDeserializer(Field field) {
            _field = field;
        }

        @Override
        void deserialize(AbstractHessianInput in, Object obj)
            throws IOException {
            double value = 0;

            try {
                value = in.readDouble();

                _field.setDouble(obj, value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class StringFieldDeserializer extends FieldDeserializer {
        private final Field _field;

        StringFieldDeserializer(Field field) {
            _field = field;
        }

        @Override
        void deserialize(AbstractHessianInput in, Object obj)
            throws IOException {
            String value = null;

            try {
                value = in.readString();

                _field.set(obj, value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class SqlDateFieldDeserializer extends FieldDeserializer {
        private final Field _field;

        SqlDateFieldDeserializer(Field field) {
            _field = field;
        }

        @Override
        void deserialize(AbstractHessianInput in, Object obj)
            throws IOException {
            java.sql.Date value = null;

            try {
                java.util.Date date = (java.util.Date) in.readObject();
                if (date != null)
                    value = new java.sql.Date(date.getTime());

                _field.set(obj, value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class SqlTimestampFieldDeserializer extends FieldDeserializer {
        private final Field _field;

        SqlTimestampFieldDeserializer(Field field) {
            _field = field;
        }

        @Override
        void deserialize(AbstractHessianInput in, Object obj)
            throws IOException {
            java.sql.Timestamp value = null;

            try {
                java.util.Date date = (java.util.Date) in.readObject();
                if (date != null)
                    value = new java.sql.Timestamp(date.getTime());

                _field.set(obj, value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class SqlTimeFieldDeserializer extends FieldDeserializer {
        private final Field _field;

        SqlTimeFieldDeserializer(Field field) {
            _field = field;
        }

        @Override
        void deserialize(AbstractHessianInput in, Object obj)
            throws IOException {
            java.sql.Time value = null;

            try {
                java.util.Date date = (java.util.Date) in.readObject();
                if (date != null) value = new java.sql.Time(date.getTime());

                _field.set(obj, value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    /**
     * @see     java.lang.Boolean#TYPE
     * @see     java.lang.Character#TYPE
     * @see     java.lang.Byte#TYPE
     * @see     java.lang.Short#TYPE
     * @see     java.lang.Integer#TYPE
     * @see     java.lang.Long#TYPE
     * @see     java.lang.Float#TYPE
     * @see     java.lang.Double#TYPE
     * @see     java.lang.Void#TYPE
     */
    private static boolean isPrimitive(Type type) {
        try{
            if(type != null) {
                if(type instanceof Class<?>){
                    Class<?> clazz = (Class<?>)type;
                    return clazz.isPrimitive()
                        || PRIMITIVE_TYPE.containsKey(clazz.getName());
                }
            }
        }
        catch (Exception e){
            // ignore exception
        }
        return false;
    }

    static final Map<String, Boolean> PRIMITIVE_TYPE = new HashMap<String, Boolean>(){
        {
            put(Boolean.class.getName()    , true );
            put(Character.class.getName()  , true );
            put(Byte.class.getName()       , true );
            put(Short.class.getName()      , true );
            put(Integer.class.getName()    , true );
            put(Long.class.getName()       , true );
            put(Float.class.getName()      , true );
            put(Double.class.getName()     , true );
            put(Void.class.getName()       , true );
        }
    };
}

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
package org.apache.dubbo.common.timer;

import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.SystemPropertyConfigUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import static org.awaitility.Awaitility.await;

class HashedWheelTimerTest {
    private CountDownLatch tryStopTaskCountDownLatch = new CountDownLatch(1);
    private CountDownLatch errorTaskCountDownLatch = new CountDownLatch(1);

    private static class EmptyTask implements TimerTask {
        @Override
        public void run(Timeout timeout) {}
    }

    private static class BlockTask implements TimerTask {
        @Override
        public void run(Timeout timeout) throws InterruptedException {
            this.wait();
        }
    }

    private class ErrorTask implements TimerTask {
        @Override
        public void run(Timeout timeout) {
            errorTaskCountDownLatch.countDown();
            throw new RuntimeException("Test");
        }
    }

    private class TryStopTask implements TimerTask {
        private Timer timer;

        public TryStopTask(Timer timer) {
            this.timer = timer;
        }

        @Override
        public void run(Timeout timeout) {
            Assertions.assertThrows(RuntimeException.class, () -> timer.stop());
            tryStopTaskCountDownLatch.countDown();
        }
    }

    @Test
    void constructorTest() {
        // use weak reference to let gc work every time
        // which can check finalize method and reduce memory usage in time
        WeakReference<Timer> timer = new WeakReference<>(new HashedWheelTimer());
        timer = new WeakReference<>(new HashedWheelTimer(100, TimeUnit.MILLISECONDS));
        timer = new WeakReference<>(new HashedWheelTimer(100, TimeUnit.MILLISECONDS, 8));

        // to cover arg check branches
        Assertions.assertThrows(RuntimeException.class, () -> {
            new HashedWheelTimer(null, 100, TimeUnit.MILLISECONDS, 8, -1);
        });

        Assertions.assertThrows(RuntimeException.class, () -> {
            new HashedWheelTimer(new NamedThreadFactory("dubbo-future-timeout", true), 0, TimeUnit.MILLISECONDS, 8, -1);
        });

        Assertions.assertThrows(RuntimeException.class, () -> {
            new HashedWheelTimer(new NamedThreadFactory("dubbo-future-timeout", true), 100, null, 8, -1);
        });

        Assertions.assertThrows(RuntimeException.class, () -> {
            new HashedWheelTimer(
                    new NamedThreadFactory("dubbo-future-timeout", true), 100, TimeUnit.MILLISECONDS, 0, -1);
        });

        Assertions.assertThrows(RuntimeException.class, () -> {
            new HashedWheelTimer(
                    new NamedThreadFactory("dubbo-future-timeout", true), Long.MAX_VALUE, TimeUnit.MILLISECONDS, 8, -1);
        });

        Assertions.assertThrows(RuntimeException.class, () -> {
            new HashedWheelTimer(
                    new NamedThreadFactory("dubbo-future-timeout", true),
                    100,
                    TimeUnit.MILLISECONDS,
                    Integer.MAX_VALUE,
                    -1);
        });

        for (int i = 0; i < 128; i++) {
            // to trigger INSTANCE_COUNT_LIMIT
            timer = new WeakReference<>(new HashedWheelTimer());
        }

        System.gc();
    }

    @Test
    void createTaskTest() throws InterruptedException {
        HashedWheelTimer timer = new HashedWheelTimer(
                new NamedThreadFactory("dubbo-future-timeout", true), 10, TimeUnit.MILLISECONDS, 8, 8);

        EmptyTask emptyTask = new EmptyTask();
        Assertions.assertThrows(RuntimeException.class, () -> timer.newTimeout(null, 5, TimeUnit.SECONDS));
        Assertions.assertThrows(RuntimeException.class, () -> timer.newTimeout(emptyTask, 5, null));

        Timeout timeout = timer.newTimeout(new ErrorTask(), 10, TimeUnit.MILLISECONDS);
        errorTaskCountDownLatch.await();
        Assertions.assertFalse(timeout.cancel());
        Assertions.assertFalse(timeout.isCancelled());
        Assertions.assertNotNull(timeout.toString());
        Assertions.assertEquals(timeout.timer(), timer);

        timeout = timer.newTimeout(emptyTask, 1000, TimeUnit.SECONDS);
        timeout.cancel();
        Assertions.assertTrue(timeout.isCancelled());

        List<Timeout> timeouts = new LinkedList<>();
        BlockTask blockTask = new BlockTask();
        while (timer.pendingTimeouts() < 8) {
            // to trigger maxPendingTimeouts
            timeout = timer.newTimeout(blockTask, -1, TimeUnit.MILLISECONDS);
            timeouts.add(timeout);
            Assertions.assertNotNull(timeout.toString());
        }
        Assertions.assertEquals(8, timer.pendingTimeouts());

        // this will throw an exception because of maxPendingTimeouts
        Assertions.assertThrows(RuntimeException.class, () -> timer.newTimeout(blockTask, 1, TimeUnit.MILLISECONDS));

        Timeout secondTimeout = timeouts.get(2);
        // wait until the task expired
        await().until(secondTimeout::isExpired);

        timer.stop();
    }

    @Test
    void stopTaskTest() throws InterruptedException {
        Timer timer = new HashedWheelTimer(new NamedThreadFactory("dubbo-future-timeout", true));
        timer.newTimeout(new TryStopTask(timer), 10, TimeUnit.MILLISECONDS);
        tryStopTaskCountDownLatch.await();

        for (int i = 0; i < 8; i++) {
            timer.newTimeout(new EmptyTask(), 0, TimeUnit.SECONDS);
        }
        // stop timer
        timer.stop();
        Assertions.assertTrue(timer.isStop());

        // this will throw an exception
        Assertions.assertThrows(RuntimeException.class, () -> timer.newTimeout(new EmptyTask(), 5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Check whether sun.misc.Unsafe (used by latest Netty HashedWheelTimer) could be compiled or not.")
    void unsafeTest() {
        // attempt to access field Unsafe#theUnsafe
        final Object maybeUnsafe = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                    // We always want to try using Unsafe as the access still works on java9 as well and
                    // we need it for out native-transports and many optimizations.
                    Throwable cause = ReflectionUtil.trySetAccessible(unsafeField, false);
                    if (cause != null) {
                        return cause;
                    }
                    // the unsafe instance
                    return unsafeField.get(null);
                } catch (NoSuchFieldException | IllegalAccessException | SecurityException e) {
                    return e;
                } catch (NoClassDefFoundError e) {
                    // Also catch NoClassDefFoundError in case someone uses for example OSGI and it made
                    // Unsafe unloadable.
                    return e;
                }
            }
        });

        Assertions.assertInstanceOf(Unsafe.class, maybeUnsafe);
    }

    private static final class ReflectionUtil {

        private ReflectionUtil() {}

        /**
         * Try to call {@link AccessibleObject#setAccessible(boolean)} but will catch any {@link SecurityException} and
         * {@link java.lang.reflect.InaccessibleObjectException} and return it.
         * The caller must check if it returns {@code null} and if not handle the returned exception.
         */
        public static Throwable trySetAccessible(AccessibleObject object, boolean checkAccessible) {
            if (checkAccessible && !explicitTryReflectionSetAccessible0()) {
                return new UnsupportedOperationException("Reflective setAccessible(true) disabled");
            }
            try {
                object.setAccessible(true);
                return null;
            } catch (SecurityException e) {
                return e;
            } catch (RuntimeException e) {
                return handleInaccessibleObjectException(e);
            }
        }

        private static RuntimeException handleInaccessibleObjectException(RuntimeException e) {
            // JDK 9 can throw an inaccessible object exception here; since Netty compiles
            // against JDK 7 and this exception was only added in JDK 9, we have to weakly
            // check the type
            if ("java.lang.reflect.InaccessibleObjectException"
                    .equals(e.getClass().getName())) {
                return e;
            }
            throw e;
        }

        private static Class<?> fail(Class<?> type, String typeParamName) {
            throw new IllegalStateException(
                    "cannot determine the type of the type parameter '" + typeParamName + "': " + type);
        }

        /**
         * Resolve a type parameter of a class that is a subclass of the given parametrized superclass.
         *
         * @param object                 The object to resolve the type parameter for
         * @param parametrizedSuperclass The parametrized superclass
         * @param typeParamName          The name of the type parameter to resolve
         * @return The resolved type parameter
         * @throws IllegalStateException if the type parameter could not be resolved
         */
        public static Class<?> resolveTypeParameter(
                final Object object, Class<?> parametrizedSuperclass, String typeParamName) {
            final Class<?> thisClass = object.getClass();
            Class<?> currentClass = thisClass;
            for (; ; ) {
                if (currentClass.getSuperclass() == parametrizedSuperclass) {
                    int typeParamIndex = -1;
                    TypeVariable<?>[] typeParams = currentClass.getSuperclass().getTypeParameters();
                    for (int i = 0; i < typeParams.length; i++) {
                        if (typeParamName.equals(typeParams[i].getName())) {
                            typeParamIndex = i;
                            break;
                        }
                    }

                    if (typeParamIndex < 0) {
                        throw new IllegalStateException(
                                "unknown type parameter '" + typeParamName + "': " + parametrizedSuperclass);
                    }

                    Type genericSuperType = currentClass.getGenericSuperclass();
                    if (!(genericSuperType instanceof ParameterizedType)) {
                        return Object.class;
                    }

                    Type[] actualTypeParams = ((ParameterizedType) genericSuperType).getActualTypeArguments();

                    Type actualTypeParam = actualTypeParams[typeParamIndex];
                    if (actualTypeParam instanceof ParameterizedType) {
                        actualTypeParam = ((ParameterizedType) actualTypeParam).getRawType();
                    }
                    if (actualTypeParam instanceof Class) {
                        return (Class<?>) actualTypeParam;
                    }
                    if (actualTypeParam instanceof GenericArrayType) {
                        Type componentType = ((GenericArrayType) actualTypeParam).getGenericComponentType();
                        if (componentType instanceof ParameterizedType) {
                            componentType = ((ParameterizedType) componentType).getRawType();
                        }
                        if (componentType instanceof Class) {
                            return Array.newInstance((Class<?>) componentType, 0)
                                    .getClass();
                        }
                    }
                    if (actualTypeParam instanceof TypeVariable) {
                        // Resolved type parameter points to another type parameter.
                        TypeVariable<?> v = (TypeVariable<?>) actualTypeParam;
                        if (!(v.getGenericDeclaration() instanceof Class)) {
                            return Object.class;
                        }

                        currentClass = thisClass;
                        parametrizedSuperclass = (Class<?>) v.getGenericDeclaration();
                        typeParamName = v.getName();
                        if (parametrizedSuperclass.isAssignableFrom(thisClass)) {
                            continue;
                        }
                        return Object.class;
                    }

                    return fail(thisClass, typeParamName);
                }
                currentClass = currentClass.getSuperclass();
                if (currentClass == null) {
                    return fail(thisClass, typeParamName);
                }
            }
        }

        private static boolean explicitTryReflectionSetAccessible0() {
            // we disable reflective access
            return Boolean.parseBoolean(SystemPropertyConfigUtils.getSystemProperty(
                    "io.netty.tryReflectionSetAccessible",
                    String.valueOf(javaVersion() < 9 || RUNNING_IN_NATIVE_IMAGE)));
        }

        private static final boolean RUNNING_IN_NATIVE_IMAGE =
                SystemPropertyConfigUtils.getSystemProperty("org.graalvm.nativeimage.imagecode") != null;

        private static int javaVersion() {
            return majorVersion(SystemPropertyConfigUtils.getSystemProperty("java.specification.version", "1.6"));
        }

        private static int majorVersion(final String javaSpecVersion) {
            final String[] components = javaSpecVersion.split("\\.");
            final int[] version = new int[components.length];
            for (int i = 0; i < components.length; i++) {
                version[i] = Integer.parseInt(components[i]);
            }

            if (version[0] == 1) {
                assert version[1] >= 6;
                return version[1];
            } else {
                return version[0];
            }
        }
    }
}

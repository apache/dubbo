/*
 * Authored by Project Lombok and licensed by MIT License, which is attached below:
 *
 * Copyright (C) 2018-2021 The Project Lombok Authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.apache.dubbo.annotation.permit;

import org.apache.dubbo.annotation.permit.dummy.Parent;

import sun.misc.Unsafe;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

// sunapi suppresses javac's warning about using Unsafe; 'all' suppresses eclipse's warning about the unspecified 'sunapi' key. Leave them both.
// Yes, javac's definition of the word 'all' is quite contrary to what the dictionary says it means. 'all' does NOT include 'sunapi' according to javac.
@SuppressWarnings({"sunapi", "all"})
public class Permit {
    private Permit() {
    }


    private static final long ACCESSIBLE_OVERRIDE_FIELD_OFFSET;
    private static final IllegalAccessException INIT_ERROR;
    private static final sun.misc.Unsafe UNSAFE = (sun.misc.Unsafe) reflectiveStaticFieldAccess(sun.misc.Unsafe.class, "theUnsafe");

    static {
        Field f;
        long g;
        Throwable ex;

        try {
            g = getOverrideFieldOffset();
            ex = null;
        } catch (Throwable t) {
            f = null;
            g = -1L;
            ex = t;
        }

        ACCESSIBLE_OVERRIDE_FIELD_OFFSET = g;
        if (ex == null) INIT_ERROR = null;
        else if (ex instanceof IllegalAccessException) INIT_ERROR = (IllegalAccessException) ex;
        else {
            INIT_ERROR = new IllegalAccessException("Cannot initialize Unsafe-based permit");
            INIT_ERROR.initCause(ex);
        }
    }

    public static <T extends AccessibleObject> T setAccessible(T accessor) {
        if (INIT_ERROR == null) {
            UNSAFE.putBoolean(accessor, ACCESSIBLE_OVERRIDE_FIELD_OFFSET, true);
        } else {
            accessor.setAccessible(true);
        }

        return accessor;
    }

    private static long getOverrideFieldOffset() throws Throwable {
        Field f = null;
        Throwable saved = null;
        try {
            f = AccessibleObject.class.getDeclaredField("override");
        } catch (Throwable t) {
            saved = t;
        }

        if (f != null) {
            return UNSAFE.objectFieldOffset(f);
        }
        // The below seems very risky, but for all AccessibleObjects in java today it does work, and starting with JDK12, making the field accessible is no longer possible.
        try {
            return UNSAFE.objectFieldOffset(Fake.class.getDeclaredField("override"));
        } catch (Throwable t) {
            throw saved;
        }
    }

    static class Fake {
        boolean override;
        Object accessCheckCache;
    }

    public static Method getMethod(Class<?> c, String mName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method m = null;
        Class<?> oc = c;
        while (c != null) {
            try {
                m = c.getDeclaredMethod(mName, parameterTypes);
                break;
            } catch (NoSuchMethodException e) {
            }
            c = c.getSuperclass();
        }

        if (m == null) throw new NoSuchMethodException(oc.getName() + " :: " + mName + "(args)");
        return setAccessible(m);
    }

    public static Method permissiveGetMethod(Class<?> c, String mName, Class<?>... parameterTypes) {
        try {
            return getMethod(c, mName, parameterTypes);
        } catch (Exception ignore) {
            return null;
        }
    }

    public static Field getField(Class<?> c, String fName) throws NoSuchFieldException {
        Field f = null;
        Class<?> oc = c;
        while (c != null) {
            try {
                f = c.getDeclaredField(fName);
                break;
            } catch (NoSuchFieldException e) {
            }
            c = c.getSuperclass();
        }

        if (f == null) throw new NoSuchFieldException(oc.getName() + " :: " + fName);

        return setAccessible(f);
    }

    public static Field permissiveGetField(Class<?> c, String fName) {
        try {
            return getField(c, fName);
        } catch (Exception ignore) {
            return null;
        }
    }

    public static <T> T permissiveReadField(Class<T> type, Field f, Object instance) {
        try {
            return type.cast(f.get(instance));
        } catch (Exception ignore) {
            return null;
        }
    }

    public static <T> Constructor<T> getConstructor(Class<T> c, Class<?>... parameterTypes) throws NoSuchMethodException {
        return setAccessible(c.getDeclaredConstructor(parameterTypes));
    }

    private static Object reflectiveStaticFieldAccess(Class<?> c, String fName) {
        try {
            Field f = c.getDeclaredField(fName);
            f.setAccessible(true);
            return f.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isDebugReflection() {
        return !"false".equals(System.getProperty("lombok.debug.reflection", "false"));
    }

    public static void handleReflectionDebug(Throwable t, Throwable initError) {
        if (!isDebugReflection()) return;

        System.err.println("** LOMBOK REFLECTION exception: " + t.getClass() + ": " + (t.getMessage() == null ? "(no message)" : t.getMessage()));
        t.printStackTrace(System.err);
        if (initError != null) {
            System.err.println("*** ADDITIONALLY, exception occurred setting up reflection: ");
            initError.printStackTrace(System.err);
        }
    }

    public static Object invoke(Method m, Object receiver, Object... args) throws IllegalAccessException, InvocationTargetException {
        return invoke(null, m, receiver, args);
    }

    public static Object invoke(Throwable initError, Method m, Object receiver, Object... args) throws IllegalAccessException, InvocationTargetException {
        try {
            return m.invoke(receiver, args);
        } catch (IllegalAccessException e) {
            handleReflectionDebug(e, initError);
            throw e;
        } catch (RuntimeException e) {
            handleReflectionDebug(e, initError);
            throw e;
        } catch (Error e) {
            handleReflectionDebug(e, initError);
            throw e;
        }
    }

    public static Object invokeSneaky(Method m, Object receiver, Object... args) {
        return invokeSneaky(null, m, receiver, args);
    }

    public static Object invokeSneaky(Throwable initError, Method m, Object receiver, Object... args) {
        try {
            return m.invoke(receiver, args);
        } catch (NoClassDefFoundError e) {
            handleReflectionDebug(e, initError);
            //ignore, we don't have access to the correct ECJ classes, so lombok can't possibly
            //do anything useful here.
            return null;
        } catch (NullPointerException e) {
            handleReflectionDebug(e, initError);
            //ignore, we don't have access to the correct ECJ classes, so lombok can't possibly
            //do anything useful here.
            return null;
        } catch (IllegalAccessException e) {
            handleReflectionDebug(e, initError);
            throw sneakyThrow(e);
        } catch (InvocationTargetException e) {
            throw sneakyThrow(e.getCause());
        } catch (RuntimeException e) {
            handleReflectionDebug(e, initError);
            throw e;
        } catch (Error e) {
            handleReflectionDebug(e, initError);
            throw e;
        }
    }

    public static <T> T newInstance(Constructor<T> c, Object... args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        return newInstance(null, c, args);
    }

    public static <T> T newInstance(Throwable initError, Constructor<T> c, Object... args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        try {
            return c.newInstance(args);
        } catch (IllegalAccessException e) {
            handleReflectionDebug(e, initError);
            throw e;
        } catch (InstantiationException e) {
            handleReflectionDebug(e, initError);
            throw e;
        } catch (RuntimeException e) {
            handleReflectionDebug(e, initError);
            throw e;
        } catch (Error e) {
            handleReflectionDebug(e, initError);
            throw e;
        }
    }

    public static <T> T newInstanceSneaky(Constructor<T> c, Object... args) {
        return newInstanceSneaky(null, c, args);
    }

    public static <T> T newInstanceSneaky(Throwable initError, Constructor<T> c, Object... args) {
        try {
            return c.newInstance(args);
        } catch (NoClassDefFoundError e) {
            handleReflectionDebug(e, initError);
            //ignore, we don't have access to the correct ECJ classes, so lombok can't possibly
            //do anything useful here.
            return null;
        } catch (NullPointerException e) {
            handleReflectionDebug(e, initError);
            //ignore, we don't have access to the correct ECJ classes, so lombok can't possibly
            //do anything useful here.
            return null;
        } catch (IllegalAccessException e) {
            handleReflectionDebug(e, initError);
            throw sneakyThrow(e);
        } catch (InstantiationException e) {
            handleReflectionDebug(e, initError);
            throw sneakyThrow(e);
        } catch (InvocationTargetException e) {
            throw sneakyThrow(e.getCause());
        } catch (RuntimeException e) {
            handleReflectionDebug(e, initError);
            throw e;
        } catch (Error e) {
            handleReflectionDebug(e, initError);
            throw e;
        }
    }

    public static Object get(Field f, Object receiver) throws IllegalAccessException {
        try {
            return f.get(receiver);
        } catch (IllegalAccessException e) {
            handleReflectionDebug(e, null);
            throw e;
        } catch (RuntimeException e) {
            handleReflectionDebug(e, null);
            throw e;
        } catch (Error e) {
            handleReflectionDebug(e, null);
            throw e;
        }
    }

    public static void set(Field f, Object receiver, Object newValue) throws IllegalAccessException {
        try {
            f.set(receiver, newValue);
        } catch (IllegalAccessException e) {
            handleReflectionDebug(e, null);
            throw e;
        } catch (RuntimeException e) {
            handleReflectionDebug(e, null);
            throw e;
        } catch (Error e) {
            handleReflectionDebug(e, null);
            throw e;
        }
    }

    public static void reportReflectionProblem(Throwable initError, String msg) {
        if (!isDebugReflection()) return;
        System.err.println("** LOMBOK REFLECTION issue: " + msg);
        if (initError != null) {
            System.err.println("*** ADDITIONALLY, exception occurred setting up reflection: ");
            initError.printStackTrace(System.err);
        }
    }

    public static RuntimeException sneakyThrow(Throwable t) {
        if (t == null) throw new NullPointerException("t");
        return Permit.<RuntimeException>sneakyThrow0(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T sneakyThrow0(Throwable t) throws T {
        throw (T) t;
    }

    private static Object getJdkCompilerModule() {
		/* call public api: ModuleLayer.boot().findModule("jdk.compiler").get();
		   but use reflection because we don't want this code to crash on jdk1.7 and below.
		   In that case, none of this stuff was needed in the first place, so we just exit via
		   the catch block and do nothing.
		 */

        try {
            Class<?> cModuleLayer = Class.forName("java.lang.ModuleLayer");
            Method mBoot = cModuleLayer.getDeclaredMethod("boot");
            Object bootLayer = mBoot.invoke(null);
            Class<?> cOptional = Class.forName("java.util.Optional");
            Method mFindModule = cModuleLayer.getDeclaredMethod("findModule", String.class);
            Object oCompilerO = mFindModule.invoke(bootLayer, "jdk.compiler");
            return cOptional.getDeclaredMethod("get").invoke(oCompilerO);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object getOwnModule() {
        try {
            Method m = Permit.getMethod(Class.class, "getModule");
            return m.invoke(Permit.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static long getFirstFieldOffset(Unsafe unsafe) {
        try {
            return unsafe.objectFieldOffset(Parent.class.getDeclaredField("first"));
        } catch (NoSuchFieldException e) {
            // can't happen.
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            // can't happen
            throw new RuntimeException(e);
        }
    }

    public static void addOpens() {
        Class<?> cModule;
        try {
            cModule = Class.forName("java.lang.Module");
        } catch (ClassNotFoundException e) {
            return; //jdk8-; this is not needed.
        }

        Unsafe unsafe = UNSAFE;

        Object jdkCompilerModule = getJdkCompilerModule();
        Object ownModule = getOwnModule();
        String[] allPkgs = {
            "com.sun.tools.javac.code",
            "com.sun.tools.javac.comp",
            "com.sun.tools.javac.file",
            "com.sun.tools.javac.main",
            "com.sun.tools.javac.model",
            "com.sun.tools.javac.parser",
            "com.sun.tools.javac.processing",
            "com.sun.tools.javac.tree",
            "com.sun.tools.javac.util",
            "com.sun.tools.javac.jvm",
            "com.sun.tools.javac.api"
        };

        try {
            Method m = cModule.getDeclaredMethod("implAddOpens", String.class, cModule);
            long firstFieldOffset = getFirstFieldOffset(unsafe);
            unsafe.putBooleanVolatile(m, firstFieldOffset, true);
            for (String p : allPkgs) m.invoke(jdkCompilerModule, p, ownModule);
        } catch (Exception ignore) {}

        try {
            Method m = cModule.getDeclaredMethod("implAddExports", String.class, cModule);
            long firstFieldOffset = getFirstFieldOffset(unsafe);
            unsafe.putBooleanVolatile(m, firstFieldOffset, true);
            for (String p : allPkgs) m.invoke(jdkCompilerModule, p, ownModule);
        } catch (Exception ignore) {}
    }
}

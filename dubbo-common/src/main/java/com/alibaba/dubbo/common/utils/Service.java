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
package com.alibaba.dubbo.common.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

/**
 * Copy form openjdk sun.misc.Service.
 * http://java.sun.com/j2se/1.4.2/docs/guide/jar/jar.html#Service%20Provider
 * 
 * @author william.liangf
 * @author qian.lei
 * @author ding.lid
 */
public final class Service {

    private static final String prefix = "META-INF/services/";

    private Service() {
    }

    private static void fail(Class<?> service, String msg, Throwable cause) {
        IllegalStateException sce = new IllegalStateException(service.getName() + ": "
                + msg);
        sce.initCause(cause);
        throw sce;
    }

    private static void fail(Class<?> service, String msg) {
        throw new IllegalStateException(service.getName() + ": " + msg);
    }

    private static void fail(Class<?> service, URL u, int line, String msg) {
        fail(service, u + ":" + line + ": " + msg);
    }

    /**
     * Parse a single line from the given configuration file, adding the name on
     * the line to both the names list and the returned set iff the name is not
     * already a member of the returned set.
     */
    private static int parseLine(Class<?> service, URL u, BufferedReader r, int lc,
                                 List<String> names, Set<String> returned) throws IOException {
        String ln = r.readLine();
        if (ln == null) {
            return -1;
        }
        int ci = ln.indexOf('#');
        if (ci >= 0)
            ln = ln.substring(0, ci);
        ln = ln.trim();
        int n = ln.length();
        if (n != 0) {
            if ((ln.indexOf(' ') >= 0) || (ln.indexOf('\t') >= 0))
                fail(service, u, lc, "Illegal configuration-file syntax");
            int cp = ln.codePointAt(0);
            if (!Character.isJavaIdentifierStart(cp))
                fail(service, u, lc, "Illegal provider-class name: " + ln);
            for (int i = Character.charCount(cp); i < n; i += Character.charCount(cp)) {
                cp = ln.codePointAt(i);
                if (!Character.isJavaIdentifierPart(cp) && (cp != '.'))
                    fail(service, u, lc, "Illegal provider-class name: " + ln);
            }
            if (!returned.contains(ln)) {
                names.add(ln);
                returned.add(ln);
            }
        }
        return lc + 1;
    }

    /**
     * Parse the content of the given URL as a provider-configuration file.
     * 
     * @param service The service class for which providers are being sought;
     *            used to construct error detail strings
     * @param url The URL naming the configuration file to be parsed
     * @param returned A Set containing the names of provider classes that have
     *            already been returned. This set will be updated to contain the
     *            names that will be yielded from the returned <tt>Iterator</tt>
     *            .
     * @return A (possibly empty) <tt>Iterator</tt> that will yield the
     *         provider-class names in the given configuration file that are not
     *         yet members of the returned set
     * @throws ServiceConfigurationError If an I/O error occurs while reading
     *             from the given URL, or if a configuration-file format error
     *             is detected
     */
    private static Iterator<String> parse(Class<?> service, URL u, Set<String> returned) {
        InputStream in = null;
        BufferedReader r = null;
        ArrayList<String> names = new ArrayList<String>();
        try {
            in = u.openStream();
            r = new BufferedReader(new InputStreamReader(in, "utf-8"));
            int lc = 1;
            while ((lc = parseLine(service, u, r, lc, names, returned)) >= 0)
                ;
        } catch (IOException x) {
            fail(service, ": " + x);
        } finally {
            try {
                if (r != null)
                    r.close();
                if (in != null)
                    in.close();
            } catch (IOException y) {
                fail(service, ": " + y);
            }
        }
        return names.iterator();
    }

    /**
     * Private inner class implementing fully-lazy provider lookup
     */
    private static class LazyIterator<T> implements Iterator<Class<? extends T>> {
        
        Class<T>         service;
        ClassLoader      loader;
        Enumeration<URL> configs  = null;
        Iterator<String> pending  = null;
        Set<String>      returned = new TreeSet<String>();
        String           nextName = null;
        
        private LazyIterator(Class<T> service, ClassLoader loader) {
            this.service = service;
            this.loader = loader;
        }
        
        public boolean hasNext() {
            if (nextName != null) {
                return true;
            }
            if (configs == null) {
                try {
                    String fullName = prefix + service.getName();
                    if (loader == null)
                        configs = ClassLoader.getSystemResources(fullName);
                    else
                        configs = loader.getResources(fullName);
                } catch (IOException x) {
                    fail(service, ": " + x);
                }
            }
            while ((pending == null) || !pending.hasNext()) {
                if (!configs.hasMoreElements()) {
                    return false;
                }
                pending = parse(service, (URL) configs.nextElement(), returned);
            }
            nextName = (String) pending.next();
            return true;
        }
		        
        @SuppressWarnings("unchecked")
        public Class<? extends T> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String cn = nextName;
            nextName = null;
            try {
                return (Class<? extends T>) Class.forName(cn, true, loader);
            } catch (ClassNotFoundException x) {
                fail(service, "Provider " + cn + " not found");
            } catch (Exception x) {
                fail(service, "Provider " + cn + " could not be instantiated: " + x, x);
            }
            return null; /* This cannot happen */
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Locates and incrementally instantiates the available providers of a given
     * service using the given class loader.
     * <p>
     * This method transforms the name of the given service class into a
     * provider-configuration filename as described above and then uses the
     * <tt>getResources</tt> method of the given class loader to find all
     * available files with that name. These files are then read and parsed to
     * produce a list of provider-class names. The iterator that is returned
     * uses the given class loader to lookup and then instantiate each element
     * of the list.
     * <p>
     * Because it is possible for extensions to be installed into a running Java
     * virtual machine, this method may return different results each time it is
     * invoked.
     * <p>
     * 
     * @param service The service's abstract service class
     * @param loader The class loader to be used to load provider-configuration
     *            files and instantiate provider classes, or <tt>null</tt> if
     *            the system class loader (or, failing that the bootstrap class
     *            loader) is to be used
     * @return An <tt>Iterator</tt> that yields provider objects for the given
     *         service, in some arbitrary order. The iterator will throw a
     *         <tt>ServiceConfigurationError</tt> if a provider-configuration
     *         file violates the specified format or if a provider class cannot
     *         be found and instantiated.
     * @throws ServiceConfigurationError If a provider-configuration file
     *             violates the specified format or names a provider class that
     *             cannot be found and instantiated
     * @see #providers(java.lang.Class<?>)
     * @see #installedProviders(java.lang.Class<?>)
     */
    public static <T> Iterator<Class<? extends T>> providers(Class<T> service, ClassLoader loader) {
        return new LazyIterator<T>(service, loader);
    }

    /**
     * Locates and incrementally instantiates the available providers of a given
     * service using the context class loader. This convenience method is
     * equivalent to
     * 
     * <pre>
     * ClassLoader cl = Thread.currentThread().getContextClassLoader();
     * return Service.providers(service, cl);
     * </pre>
     * 
     * @param service The service's abstract service class
     * @return An <tt>Iterator</tt> that yields provider objects for the given
     *         service, in some arbitrary order. The iterator will throw a
     *         <tt>ServiceConfigurationError</tt> if a provider-configuration
     *         file violates the specified format or if a provider class cannot
     *         be found and instantiated.
     * @throws ServiceConfigurationError If a provider-configuration file
     *             violates the specified format or names a provider class that
     *             cannot be found and instantiated
     * @see #providers(java.lang.Class<?>, java.lang.ClassLoader)
     */
    public static <T> Iterator<Class<? extends T>> providers(Class<T> service) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return Service.providers(service, cl);
    }

    /**
     * Locates and incrementally instantiates the available providers of a given
     * service using the extension class loader. This convenience method simply
     * locates the extension class loader, call it <tt>extClassLoader</tt>, and
     * then does
     * 
     * <pre>
     * return Service.providers(service, extClassLoader);
     * </pre>
     * 
     * If the extension class loader cannot be found then the system class
     * loader is used; if there is no system class loader then the bootstrap
     * class loader is used.
     * 
     * @param service The service's abstract service class
     * @return An <tt>Iterator</tt> that yields provider objects for the given
     *         service, in some arbitrary order. The iterator will throw a
     *         <tt>ServiceConfigurationError</tt> if a provider-configuration
     *         file violates the specified format or if a provider class cannot
     *         be found and instantiated.
     * @throws ServiceConfigurationError If a provider-configuration file
     *             violates the specified format or names a provider class that
     *             cannot be found and instantiated
     * @see #providers(java.lang.Class<?>, java.lang.ClassLoader)
     */
    public static <T> Iterator<Class<? extends T>> installedProviders(Class<T> service) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        ClassLoader prev = null;
        while (cl != null) {
            prev = cl;
            cl = cl.getParent();
        }
        return Service.providers(service, prev);
    }

}
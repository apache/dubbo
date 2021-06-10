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
package org.apache.dubbo.config.spring.beans.factory.annotation;

import java.util.HashSet;
import java.util.Set;

/**
 * A temp holder for scanned packages of service.
 */
public class ServicePackagesHolder {

    public static final String BEAN_NAME = "dubboServicePackagesHolder";

    private final Set<String> scannedPackages = new HashSet<>();

    private final Set<String> scannedClasses = new HashSet<>();


    public void addScannedPackage(String apackage) {
        apackage = normalizePackage(apackage);
        synchronized (scannedPackages) {
            scannedPackages.add(apackage);
        }
    }

    public boolean isPackageScanned(String packageName) {
        packageName = normalizePackage(packageName);
        synchronized (scannedPackages) {
            if (scannedPackages.contains(packageName)) {
                return true;
            }
            for (String scannedPackage : scannedPackages) {
                if (isSubPackage(packageName, scannedPackage)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addScannedClass(String className) {
        synchronized (scannedClasses) {
            scannedClasses.add(className);
        }
    }

    public boolean isClassScanned(String className) {
        synchronized (scannedClasses) {
            return scannedClasses.contains(className);
        }
    }

    /**
     * Whether test package is sub package of parent package
     * @param testPkg
     * @param parent
     * @return
     */
    private boolean isSubPackage(String testPkg, String parent) {
        // child pkg startsWith parent pkg
        return testPkg.startsWith(parent);
    }

    private String normalizePackage(String apackage) {
        if (!apackage.endsWith(".")) {
            apackage += ".";
        }
        return apackage;
    }

}

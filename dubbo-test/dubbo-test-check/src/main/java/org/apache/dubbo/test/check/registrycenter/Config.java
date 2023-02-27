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
package org.apache.dubbo.test.check.registrycenter;

/**
 * Define the config to obtain the
 */
public interface Config {

    /**
     * Returns the default connection address in single registry center.
     */
    default String getConnectionAddress(){
        return getConnectionAddress1();
    }

    /**
     * Returns the first connection address in multiple registry center.
     */
    String getConnectionAddress1();

    /**
     * Returns the second connection address in multiple registry center.
     */
    String getConnectionAddress2();

    /**
     * Returns the default connection address key in single registry center.
     * <h3>How to use</h3>
     * <pre>
     * System.getProperty({@link #getConnectionAddressKey()})
     * </pre>
     */
    String getConnectionAddressKey();

    /**
     * Returns the first connection address key in multiple registry center.
     * <h3>How to use</h3>
     * <pre>
     * System.getProperty({@link #getConnectionAddressKey1()})
     * </pre>
     */
    String getConnectionAddressKey1();

    /**
     * Returns the second connection address key in multiple registry center.
     * <h3>How to use</h3>
     * <pre>
     * System.getProperty({@link #getConnectionAddressKey2()})
     * </pre>
     */
    String getConnectionAddressKey2();
}

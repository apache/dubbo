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
package org.apache.dubbo.common.json;

/**
 * JSONVisitor.
 */
@Deprecated
public interface JSONVisitor {
    public static final String CLASS_PROPERTY = "class";

    /**
     * parse begin .
     */
    void begin();

    /**
     * parse end.
     *
     * @param obj     root obj.
     * @param isValue is json value.
     * @return parse result.
     * @throws ParseException
     */
    Object end(Object obj, boolean isValue) throws ParseException;

    /**
     * object begin.
     *
     * @throws ParseException
     */
    void objectBegin() throws ParseException;

    /**
     * object end, return object value.
     *
     * @param count property count.
     * @return object value.
     * @throws ParseException
     */
    Object objectEnd(int count) throws ParseException;

    /**
     * object property name.
     *
     * @param name name.
     * @throws ParseException
     */
    void objectItem(String name) throws ParseException;

    /**
     * object property value.
     *
     * @param obj     obj.
     * @param isValue is json value.
     * @throws ParseException
     */
    void objectItemValue(Object obj, boolean isValue) throws ParseException;

    /**
     * array begin.
     *
     * @throws ParseException
     */
    void arrayBegin() throws ParseException;

    /**
     * array end, return array value.
     *
     * @param count count.
     * @return array value.
     * @throws ParseException
     */
    Object arrayEnd(int count) throws ParseException;

    /**
     * array item.
     *
     * @param index index.
     * @throws ParseException
     */
    void arrayItem(int index) throws ParseException;

    /**
     * array item.
     *
     * @param index   index.
     * @param obj     item.
     * @param isValue is json value.
     * @throws ParseException
     */
    void arrayItemValue(int index, Object obj, boolean isValue) throws ParseException;
}
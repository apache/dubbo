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
package com.alibaba.dubbo.container.page;

import java.io.Serializable;
import java.util.Comparator;

/**
 * MenuComparator
 */
public class MenuComparator implements Comparator<PageHandler>, Serializable {

    private static final long serialVersionUID = -3161526932904414029L;

    public int compare(PageHandler o1, PageHandler o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return 1;
        }
        return o1.equals(o2) ? 0 : (o1.getClass().getAnnotation(Menu.class).order()
                > o2.getClass().getAnnotation(Menu.class).order() ? 1 : -1);
    }

}
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
package com.alibaba.dubbo.demo.api;

import java.io.Serializable;
import java.util.HashMap;

/**
 * IndexQueryParameter
 * 
 * @author william.liangf
 */
public class IndexQueryParameter implements Serializable {

    private static final long       serialVersionUID = 5035064961368369616L;

    /**

     * 开始的查询页码

     */

    private int                     pageNo           = 1;

    /**

     * 本次操作的最大返回记录数

     */

    private int                     pageSize         = 24;

    /**

     * 用户查询参数

     */

    private HashMap<String, Object> parameters;

    public static IndexQueryParameter create() {

        return new IndexQueryParameter();

    }

    public IndexQueryParameter() {

        this(new HashMap<String, Object>());

    }

    public IndexQueryParameter(HashMap<String, Object> parameters) {

        this.parameters = parameters;

    }

    public int getPageNo() {

        return pageNo;

    }

    public IndexQueryParameter setPageNo(int pageNo) {

        this.pageNo = pageNo;

        if (this.pageNo < 1) {

            this.pageNo = 1;

        }

        return this;

    }

    public int getPageSize() {

        return pageSize;

    }

    public IndexQueryParameter setPageSize(int pageSize) {

        this.pageSize = pageSize;

        if (this.pageSize < 0) {

            this.pageSize = 0;

        }

        return this;

    }

    public HashMap<String, Object> getParameters() {

        return parameters;

    }

    public void setParameters(HashMap<String, Object> parameters) {

        this.parameters = parameters;

    }

    public IndexQueryParameter setParameter(String name, String value) {

        this.parameters.put(name, value);

        return this;

    }

    public int getRecordCount() {

        return this.pageNo * this.pageSize;

    }

}
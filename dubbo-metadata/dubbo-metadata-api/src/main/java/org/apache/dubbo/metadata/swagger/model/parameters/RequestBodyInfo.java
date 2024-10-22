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
package org.apache.dubbo.metadata.swagger.model.parameters;

import org.apache.dubbo.metadata.swagger.model.media.Schema;

public class RequestBodyInfo {

    /**
     * The Request body.
     */
    private RequestBody requestBody;

    /**
     * The Merged schema.
     */
    private Schema mergedSchema;

    /**
     * Gets request body.
     *
     * @return the request body
     */
    public RequestBody getRequestBody() {
        return requestBody;
    }

    /**
     * Sets request body.
     *
     * @param requestBody the request body
     */
    public void setRequestBody(RequestBody requestBody) {
        this.requestBody = requestBody;
    }

    /**
     * Gets merged schema.
     *
     * @return the merged schema
     */
    public Schema getMergedSchema() {
        return mergedSchema;
    }

    /**
     * Sets merged schema.
     *
     * @param mergedSchema the merged schema
     */
    public void setMergedSchema(Schema mergedSchema) {
        this.mergedSchema = mergedSchema;
    }

    public void addProperties(String paramName, Schema schemaN) {
        if (mergedSchema == null) {
            mergedSchema = new Schema();
        }
        mergedSchema.addProperty(paramName, schemaN);
    }
}

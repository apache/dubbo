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
package org.apache.dubbo.validation.support.jvalidation;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.validation.Validator;
import org.apache.dubbo.validation.support.AbstractValidation;

/**
 * Creates a new instance of {@link Validator} using input argument url.
 * @see AbstractValidation
 * @see Validator
 */
public class JValidation extends AbstractValidation {

    /**
     * Return new instance of {@link JValidator}
     * @param url Valid URL instance
     * @return Instance of JValidator
     */
    @Override
    protected Validator createValidator(URL url) {
        return new JValidator(url);
    }

}
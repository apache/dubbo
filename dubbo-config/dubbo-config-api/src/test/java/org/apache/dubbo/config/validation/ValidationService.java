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
package org.apache.dubbo.config.validation;

import org.apache.dubbo.validation.MethodValidated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;


/**
 * ValidationService
 * <p>
 * Use service interface to distinguish validation scenario, for example: @NotNull(groups = ValidationService.class)
 */
public interface ValidationService {

    /**
     * The current logic will not verify 'groups = ValidationService.Save.class' if
     * '@MethodValidated(ValidationService.Save.class)' is not present
     *
     * @param parameter
     */
    @MethodValidated(Save.class)
    void save(ValidationParameter parameter);

    void update(ValidationParameter parameter);

    void delete(@Min(1) long id, @NotNull @Size(min = 2, max = 16) @Pattern(regexp = "^[a-zA-Z]+$") String operator);

    /**
     * Assume both id and email are needed to pass in, need to verify Save group and Update group.
     *
     * @param parameter
     */
    @MethodValidated({Save.class, Update.class})
    void relatedQuery(ValidationParameter parameter);

    /**
     * annotation which has the same name with the method but has the first letter in capital
     * used for distinguish validation scenario, for example: @NotNull(groups = ValidationService.Save.class)
     * optional
     */
    @interface Save {
    }

    /**
     * annotation which has the same name with the method but has the first letter in capital
     * used for distinguish validation scenario, for example: @NotNull(groups = ValidationService.Update.class)
     * optional
     */
    @interface Update {
    }
}

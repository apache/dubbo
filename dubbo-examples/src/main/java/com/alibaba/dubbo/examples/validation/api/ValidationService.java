/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.examples.validation.api;

/**
 * ValidationService
 * 
 * @author william.liangf
 */
public interface ValidationService { // 缺省可换服务接口区分验证场景，如：@NotNull(groups = ValidationService.class)
    
    interface Save{} // 与方法同名接口，首字母大写，用于分组区分验证场景，如：@NotNull(groups = ValidationService.Save.class)
    void save(ValidationParameter parameter);

    interface Update{} // 与方法同名接口，首字母大写，用于分组区分验证场景，如：@NotNull(groups = ValidationService.Update.class)
    void update(ValidationParameter parameter);

}

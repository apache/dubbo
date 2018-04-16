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
package com.alibaba.dubbo.demo;

import com.alibaba.dubbo.demo.entity.User;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Collection;

public interface DemoService {

    String sayHello(@NotNull(message = "医院编号不能为空")String name);

    void bye(Object o);

    void callbackParam(String msg, ParamCallback callback);

    String say01(String msg);

    String[] say02();

    void say03();

    Void say04();

    void save(@Valid User user);

    void update(@Valid User user);

    void delete(@Valid User user, @NotNull(message = "不允许为空") Boolean vip);

    void saves(@Valid Collection<User> users);

    void saves(@NotNull(message = "至少需要保存一个用户") User[] users);

    void demo(@NotNull(message = "名字不能为空") @Min(value = 6, message = "昵称不能太短") String name,
              String password, // 不校验
              @NotNull(message = "至少需要保存一个用户") User user);

    interface Save{}

}
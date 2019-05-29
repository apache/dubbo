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
package org.apache.dubbo.config.builders;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.AbstractReferenceConfig;

/**
 * AbstractBuilder
 *
 * @since 2.7
 */
public abstract class AbstractReferenceBuilder<T extends AbstractReferenceConfig, B extends AbstractReferenceBuilder<T, B>>
        extends AbstractInterfaceBuilder<T, B> {

    /**
     * Check if service provider exists, if not exists, it will be fast fail
     */
    protected Boolean check;

    /**
     * Whether to eagle-init
     */
    protected Boolean init;

    /**
     * Whether to use generic interface
     */
    protected String generic;

    /**
     * Whether to find reference's instance from the current JVM
     */
    protected Boolean injvm;

    /**
     * Lazy create connection
     */
    protected Boolean lazy;

    protected String reconnect;

    protected Boolean sticky;

    /**
     * The remote service version the customer side will reference
     */
    protected String version;

    /**
     * The remote service group the customer side will reference
     */
    protected String group;

    public B check(Boolean check) {
        this.check = check;
        return getThis();
    }

    public B init(Boolean init) {
        this.init = init;
        return getThis();
    }

    public B generic(String generic) {
        this.generic = generic;
        return getThis();
    }

    public B generic(Boolean generic) {
        if (generic != null) {
            this.generic = generic.toString();
        } else {
            this.generic = null;
        }
        return getThis();
    }

    /**
     * @param injvm
     * @see org.apache.dubbo.config.builders.AbstractInterfaceBuilder#scope(String)
     * @deprecated instead, use the parameter <b>scope</b> to judge if it's in jvm, scope=local
     */
    @Deprecated
    public B injvm(Boolean injvm) {
        this.injvm = injvm;
        return getThis();
    }

    public B lazy(Boolean lazy) {
        this.lazy = lazy;
        return getThis();
    }

    public B reconnect(String reconnect) {
        this.reconnect = reconnect;
        return getThis();
    }

    public B sticky(Boolean sticky) {
        this.sticky = sticky;
        return getThis();
    }

    public B version(String version) {
        this.version = version;
        return getThis();
    }

    public B group(String group) {
        this.group = group;
        return getThis();
    }

    @Override
    public void build(T instance) {
        super.build(instance);

        if (check != null) {
            instance.setCheck(check);
        }
        if (init != null) {
            instance.setInit(init);
        }
        if (!StringUtils.isEmpty(generic)) {
            instance.setGeneric(generic);
        }
        if (injvm != null) {
            instance.setInjvm(injvm);
        }
        if (lazy != null) {
            instance.setLazy(lazy);
        }
        if (!StringUtils.isEmpty(reconnect)) {
            instance.setReconnect(reconnect);
        }
        if (sticky != null) {
            instance.setSticky(sticky);
        }
        if (!StringUtils.isEmpty(version)) {
            instance.setVersion(version);
        }
        if (!StringUtils.isEmpty(group)) {
            instance.setGroup(group);
        }
    }
}

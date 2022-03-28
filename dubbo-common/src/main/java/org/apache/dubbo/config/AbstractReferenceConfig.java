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
package org.apache.dubbo.config;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import static org.apache.dubbo.common.constants.CommonConstants.INVOKER_LISTENER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REFERENCE_FILTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REFER_ASYNC_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ROUTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.STUB_EVENT_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDED_BY;

/**
 * AbstractConsumerConfig
 *
 * @export
 * @see ReferenceConfigBase
 */
public abstract class AbstractReferenceConfig extends AbstractInterfaceConfig {

    private static final long serialVersionUID = -2786526984373031126L;

    // ======== Reference config default values, will take effect if reference's attribute is not set  ========

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
     * Whether to support event in stub.
     */
    //TODO solve merge problem
    protected Boolean stubevent;//= Constants.DEFAULT_STUB_EVENT;



    /**
     * declares which app or service this interface belongs to
     */
    protected String providedBy;

    protected String router;

    /**
     * Weather the reference is referred asynchronously
     * @deprecated
     * @see ModuleConfig#referAsync
     */
    @Deprecated
    private Boolean referAsync;

    public AbstractReferenceConfig() {
    }

    public AbstractReferenceConfig(ModuleModel moduleModel) {
        super(moduleModel);
    }

    @Override
    protected void checkDefault() {
        super.checkDefault();
        if (sticky == null) {
            sticky = false;
        }
    }

    public Boolean isCheck() {
        return check;
    }

    public void setCheck(Boolean check) {
        this.check = check;
    }

    public Boolean isInit() {
        return init;
    }

    public void setInit(Boolean init) {
        this.init = init;
    }

    /**
     * @deprecated Replace to {@link AbstractReferenceConfig#getGeneric()}
     */
    @Deprecated
    @Parameter(excluded = true, attribute = false)
    public Boolean isGeneric() {
        return this.generic != null ? ProtocolUtils.isGeneric(generic) : null;
    }

    /**
     * @deprecated Replace to {@link AbstractReferenceConfig#setGeneric(String)}
     */
    @Deprecated
    public void setGeneric(Boolean generic) {
        if (generic != null) {
            this.generic = generic.toString();
        }
    }

    public String getGeneric() {
        return generic;
    }

    public void setGeneric(String generic) {
        if (StringUtils.isEmpty(generic)) {
            return;
        }
        if (ProtocolUtils.isValidGenericValue(generic)) {
            this.generic = generic;
        } else {
            throw new IllegalArgumentException("Unsupported generic type " + generic);
        }
    }

    /**
     * @return
     * @deprecated instead, use the parameter <b>scope</> to judge if it's in jvm, scope=local
     */
    @Deprecated
    public Boolean isInjvm() {
        return injvm;
    }

    /**
     * @param injvm
     * @deprecated instead, use the parameter <b>scope</b> to judge if it's in jvm, scope=local
     */
    @Deprecated
    public void setInjvm(Boolean injvm) {
        this.injvm = injvm;
    }

    @Override
    @Parameter(key = REFERENCE_FILTER_KEY, append = true)
    public String getFilter() {
        return super.getFilter();
    }

    @Override
    @Parameter(key = INVOKER_LISTENER_KEY, append = true)
    public String getListener() {
        return super.getListener();
    }

    @Override
    public void setListener(String listener) {
        super.setListener(listener);
    }

    public Boolean getLazy() {
        return lazy;
    }

    public void setLazy(Boolean lazy) {
        this.lazy = lazy;
    }

    @Override
    public void setOnconnect(String onconnect) {
        if (onconnect != null && onconnect.length() > 0) {
            this.stubevent = true;
        }
        super.setOnconnect(onconnect);
    }

    @Override
    public void setOndisconnect(String ondisconnect) {
        if (ondisconnect != null && ondisconnect.length() > 0) {
            this.stubevent = true;
        }
        super.setOndisconnect(ondisconnect);
    }

    @Parameter(key = STUB_EVENT_KEY)
    public Boolean getStubevent() {
        return stubevent;
    }

    public String getReconnect() {
        return reconnect;
    }

    public void setReconnect(String reconnect) {
        this.reconnect = reconnect;
    }

    public Boolean getSticky() {
        return sticky;
    }

    public void setSticky(Boolean sticky) {
        this.sticky = sticky;
    }



    @Parameter(key = PROVIDED_BY)
    public String getProvidedBy() {
        return providedBy;
    }

    public void setProvidedBy(String providedBy) {
        this.providedBy = providedBy;
    }

    @Parameter(key = ROUTER_KEY, append = true)
    public String getRouter() {
        return router;
    }

    public void setRouter(String router) {
        this.router = router;
    }

    @Deprecated
    @Parameter(key = REFER_ASYNC_KEY)
    public Boolean getReferAsync() {
        return referAsync;
    }

    @Deprecated
    public void setReferAsync(Boolean referAsync) {
        this.referAsync = referAsync;
    }
}

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
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDER_NAMESPACE;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDER_PORT;

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

    /**
     * By VirtualService and DestinationRule, envoy will generate a new route rule,such as 'demo.default.svc.cluster.local:80',the default port is 80.
     * When you want to specify the provider port,you can use this config.
     *
     * @since 3.1.0
     */
    protected Integer providerPort;

    /**
     * assign the namespace that provider belong to
     * @since 3.1.1
     */
    protected String providerNamespace;

    protected String router;

    /**
     * Weather the reference is referred asynchronously
     *
     * @see ModuleConfig#referAsync
     * @deprecated
     */
    @Deprecated
    private Boolean referAsync;

    /**
     * client type
     */
    protected String client;

    /**
     * Only the service provider of the specified protocol is invoked, and other protocols are ignored.
     */
    protected String protocol;

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

    @Override
    protected boolean isNeedCheckMethod() {
        return StringUtils.isEmpty(getGeneric());
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

    @Parameter(key = PROVIDER_PORT)
    public Integer getProviderPort() {
        return providerPort;
    }

    public void setProviderPort(Integer providerPort) {
        this.providerPort = providerPort;
    }

    @Parameter(key = PROVIDER_NAMESPACE)
    public String getProviderNamespace() {
        return providerNamespace;
    }

    public void setProviderNamespace(String providerNamespace) {
        this.providerNamespace = providerNamespace;
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

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}

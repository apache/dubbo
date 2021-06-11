package org.apache.dubbo.common.url.component.param;

import org.apache.dubbo.common.constants.CommonConstants;

import java.util.List;

public class DefaultDynamicParamSource implements DynamicParamSource {
    @Override
    public void init(List<String> keys, List<ParamValue> values) {
        keys.add(CommonConstants.VERSION_KEY);
        values.add(new DynamicValues(null));

        keys.add(CommonConstants.SIDE_KEY);
        values.add(new FixedParamValue(CommonConstants.CONSUMER_SIDE, CommonConstants.PROVIDER_SIDE));

        keys.add(CommonConstants.INTERFACE_KEY);
        values.add(new DynamicValues(null));

        keys.add(CommonConstants.PID_KEY);
        values.add(new DynamicValues(null));

        keys.add(CommonConstants.THREADPOOL_KEY);
        values.add(new DynamicValues(null));

        keys.add(CommonConstants.GROUP_KEY);
        values.add(new DynamicValues(null));

        keys.add(CommonConstants.VERSION_KEY);
        values.add(new DynamicValues(null));

        keys.add(CommonConstants.METADATA_KEY);
        values.add(new DynamicValues(null));

        keys.add(CommonConstants.APPLICATION_KEY);
        values.add(new DynamicValues(null));

        keys.add(CommonConstants.DUBBO_VERSION_KEY);
        values.add(new DynamicValues(null));

        keys.add(CommonConstants.RELEASE_KEY);
        values.add(new DynamicValues(null));

        keys.add(CommonConstants.PATH_KEY);
        values.add(new DynamicValues(null));

        keys.add(CommonConstants.ANYHOST_KEY);
        values.add(new DynamicValues(null));
    }
}

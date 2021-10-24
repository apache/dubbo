package org.apache.dubbo.rpc;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * {@link PenetrateAttachmentSelector}
 */
public class PenetrateAttachmentSelectorTest {

    @Test
    public void test() {
        ExtensionLoader<PenetrateAttachmentSelector> selectorExtensionLoader = ApplicationModel.defaultModel().getExtensionLoader(PenetrateAttachmentSelector.class);
        Set<String> supportedSelectors = selectorExtensionLoader.getSupportedExtensions();
        Map<String, Object> allSelected = new HashMap<>();
        if (CollectionUtils.isNotEmpty(supportedSelectors)) {
            for (String supportedSelector : supportedSelectors) {
                Map<String, Object> selected = selectorExtensionLoader.getExtension(supportedSelector).select();
                allSelected.putAll(selected);
            }
        }
        Assertions.assertEquals(allSelected.get("testKey"), "testVal");
    }
}

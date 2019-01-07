package org.apache.dubbo.metadata.identifier;

import org.apache.dubbo.common.Constants;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author cvictory ON 2019/1/7
 */
public class MetadataIdentifierTest {

    @Test
    public void testGetUniqueKey() {
        String interfaceName = "org.apache.dubbo.metadata.integration.InterfaceNameTestService";
        String version = "1.0.0.zk.md";
        String group = null;
        String application = "vic.zk.md";
        MetadataIdentifier providerMetadataIdentifier = new MetadataIdentifier(interfaceName, version, group, Constants.PROVIDER_SIDE, application);
        System.out.println(providerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.PATH));
        Assert.assertEquals(providerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.PATH), "metadata" + Constants.PATH_SEPARATOR + interfaceName + Constants.PATH_SEPARATOR + (version == null ? "" : (version + Constants.PATH_SEPARATOR))
                + (group == null ? "" : (group + Constants.PATH_SEPARATOR)) + Constants.PROVIDER_SIDE + Constants.PATH_SEPARATOR + application);
        System.out.println(providerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY));
        Assert.assertEquals(providerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY),
                interfaceName + MetadataIdentifier.SEPARATOR + (version == null ? "" : version + MetadataIdentifier.SEPARATOR) + (group == null ? "" : group + MetadataIdentifier.SEPARATOR) + Constants.PROVIDER_SIDE + MetadataIdentifier.SEPARATOR + application);
    }
}

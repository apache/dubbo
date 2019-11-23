package org.apache.dubbo.metadata.report.identifier;

/**
 * @author cvictory ON 2019-08-15
 */
public interface BaseMetadataIdentifier {

    String getUniqueKey(KeyTypeEnum keyType);

    String getIdentifierKey();

}

package org.apache.dubbo.metadata.store.failover;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.support.AbstractMetadataReportFactory;

public class MockMetadataReportFactory extends AbstractMetadataReportFactory {

    @Override
    protected MetadataReport createMetadataReport(URL url) {
        return new MockMetadataReport(url);
    }

}
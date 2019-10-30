package org.apache.dubbo.metadata.report;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.extension.ExtensionLoader;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_DIRECTORY;
import static org.apache.dubbo.metadata.report.support.Constants.METADATA_REPORT_KEY;

/**
 * 2019-08-09
 */
public class MetadataReportInstance {

    private static AtomicBoolean init = new AtomicBoolean(false);

    private static MetadataReport metadataReport;

    public static void init(URL metadataReportURL) {
        if (init.get()) {
            return;
        }
        MetadataReportFactory metadataReportFactory = ExtensionLoader.getExtensionLoader(MetadataReportFactory.class).getAdaptiveExtension();
        if (METADATA_REPORT_KEY.equals(metadataReportURL.getProtocol())) {
            String protocol = metadataReportURL.getParameter(METADATA_REPORT_KEY, DEFAULT_DIRECTORY);
            metadataReportURL = URLBuilder.from(metadataReportURL)
                    .setProtocol(protocol)
                    .removeParameter(METADATA_REPORT_KEY)
                    .build();
        }
        metadataReport = metadataReportFactory.getMetadataReport(metadataReportURL);
        init.set(true);
    }

    public static MetadataReport getMetadataReport() {
        return getMetadataReport(false);
    }

    public static MetadataReport getMetadataReport(boolean checked) {
        if (checked) {
            checkInit();
        }
        return metadataReport;
    }

    private static void checkInit() {
        if (!init.get()) {
            throw new IllegalStateException("the metadata report was not inited.");
        }
    }
}

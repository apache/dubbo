package org.apache.dubbo.common.constants;

public interface MetadataReportConstants {
    String METADATA_REPORT_KEY = "metadata";

    String RETRY_TIMES_KEY = "retry.times";

    Integer DEFAULT_METADATA_REPORT_RETRY_TIMES = 100;

    String RETRY_PERIOD_KEY = "retry.period";

    Integer DEFAULT_METADATA_REPORT_RETRY_PERIOD = 3000;

    String SYNC_REPORT_KEY = "sync.report";

    String CYCLE_REPORT_KEY = "cycle.report";

    Boolean DEFAULT_METADATA_REPORT_CYCLE_REPORT = true;
}

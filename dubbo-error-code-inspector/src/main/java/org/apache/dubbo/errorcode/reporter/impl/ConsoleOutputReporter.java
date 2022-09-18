package org.apache.dubbo.errorcode.reporter.impl;

import org.apache.dubbo.errorcode.reporter.ReportResult;
import org.apache.dubbo.errorcode.reporter.Reporter;

/**
 * Info here.
 */
public class ConsoleOutputReporter implements Reporter {
    @Override
    public void report(ReportResult reportResult) {
        System.out.println("All error codes: " + reportResult.getAllErrorCodes());

        System.out.println("Error codes which document links are not reachable: " + reportResult.getLinkNotReachableErrorCodes());
    }
}

package org.apache.dubbo.errorcode.reporter.impl;

import org.apache.dubbo.errorcode.reporter.ReportResult;
import org.apache.dubbo.errorcode.reporter.Reporter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Info here.
 *
 * @author Andy Cheung
 */
public class FileOutputReporter implements Reporter {
    @Override
    public void report(ReportResult reportResult) {
        try (PrintStream printStream = new PrintStream(Files.newOutputStream(Paths.get(System.getProperty("user.dir"), "error-inspection-result.txt")))) {

            printStream.println("All error codes: " + reportResult.getAllErrorCodes());
            printStream.println();
            printStream.println("Error codes which document links are not reachable: " + reportResult.getLinkNotReachableErrorCodes());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

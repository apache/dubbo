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

package org.apache.dubbo.errorcode.reporter.impl;

import org.apache.dubbo.errorcode.reporter.ReportResult;
import org.apache.dubbo.errorcode.reporter.Reporter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * File outputting reporter.
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

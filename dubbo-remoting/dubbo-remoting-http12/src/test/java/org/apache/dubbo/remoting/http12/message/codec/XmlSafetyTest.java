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
package org.apache.dubbo.remoting.http12.message.codec;

import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.LINUX)
public class XmlSafetyTest {

    ProcessChecker checker = new ProcessChecker();

    @BeforeEach
    void setUp() throws Exception {
        checker.prepare();
    }

    @AfterEach
    void check() throws Exception {
        checker.check();
    }

    @Test
    void testSafe1() {
        try {
            InputStream in = new ByteArrayInputStream(("<xml>\n" + "  <dynamic-proxy>\n"
                            + "    <interface>java.util.List</interface>\n"
                            + "    <handler class=\"java.beans.EventHandler\">\n"
                            + "      <target class=\"java.lang.ProcessBuilder\">\n"
                            + "        <command>\n"
                            + "          <string>" + "sleep" + "</string>\n"
                            + "          <string>" + "60" + "</string>\n"
                            + "        </command>\n"
                            + "      </target>\n"
                            + "      <action>start</action>\n"
                            + "    </handler>\n"
                            + "  </dynamic-proxy>\n"
                            + "</xml>")
                    .getBytes());
            new XmlCodec().decode(in, Object.class);
        } catch (Exception e) {
        }
    }

    @Test
    void testSafe2() {
        try {
            InputStream in = new ByteArrayInputStream(("<java>\n" + "  <object class=\"java.lang.Runtime\">\n"
                            + "    <void method=\"exec\">\n"
                            + "      <string>" + "sleep" + "</string>\n"
                            + "      <string>" + "60" + "</string>\n"
                            + "    </void>\n"
                            + "  </object>\n"
                            + "</java>")
                    .getBytes());
            new XmlCodec().decode(in, Object.class);
        } catch (Exception e) {
        }
    }

    static class ProcessChecker {
        Set<String> processesBefore;
        String currentPid;

        public Set<String> getProcesses() {
            if (currentPid == null) {
                return Collections.emptySet();
            }
            try {
                Set<String> processes = new HashSet<>();
                Process process = Runtime.getRuntime().exec(new String[] {"ps", "-o", "pid,ppid,cmd"});
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    boolean headerSkipped = false;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) {
                            continue;
                        }

                        if (!headerSkipped) {
                            headerSkipped = true;
                            continue;
                        }

                        String[] parts = line.split("\\s+", 3);
                        if (parts.length < 3) {
                            continue;
                        }

                        String pid = parts[0];
                        String ppid = parts[1];
                        String cmd = parts[2];

                        if (ppid.equals(currentPid) && cmd.contains("sleep") && cmd.contains("60")) {
                            processes.add(pid + ":" + cmd);
                        }
                    }
                }
                return processes;
            } catch (Exception e) {
            }
            return Collections.emptySet();
        }

        public void prepare() {
            try {
                currentPid = new File("/proc/self").getCanonicalFile().getName();
            } catch (IOException e) {
                currentPid = null;
            }
            processesBefore = getProcesses();
        }

        public void check() throws Exception {
            Set<String> processesAfter = getProcesses();
            for (String msg : processesAfter) {
                if (!processesBefore.contains(msg)) {
                    throw new Exception("Command executed when XML deserialization.");
                }
            }
        }
    }
}

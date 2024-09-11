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
package org.apache.dubbo.spring.boot.actuate.endpoint.configuration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class DubboEndpointPropertiesGenerator {

    public static void main(String[] args) {
        String QOS_API_BASE_COMMAND =
                "dubbo-plugin/dubbo-qos/src/main/resources/META-INF/dubbo/internal/org.apache.dubbo.qos.api.BaseCommand";
        String ENDPOINTS_DEFAULT_PROPERTIES =
                "dubbo-spring-boot/dubbo-spring-boot-actuator/src/main/resources/META-INF/dubbo-endpoints-default.properties";
        Set<String> newKeywords = DubboEndpointPropertiesGenerator.parseBaseCommandFile(QOS_API_BASE_COMMAND);
        DubboEndpointPropertiesGenerator.updatePropertiesFile(ENDPOINTS_DEFAULT_PROPERTIES, newKeywords);
    }

    public static Set<String> parseBaseCommandFile(String inputFilePath) {
        Set<String> keywords = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.trim().startsWith("#")) {
                    String[] parts = line.split("=");
                    if (parts.length > 0) {
                        String keyword = parts[0].trim().toLowerCase();
                        keywords.add(keyword);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return keywords;
    }

    public static void updatePropertiesFile(String propertiesFilePath, Set<String> newKeywords) {
        HashMap<String, Boolean> fileContents = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(propertiesFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()
                        && !line.trim().startsWith("#")
                        && line.trim().matches(".*enabled\\s*=\\s*.*")) {
                    fileContents.put(line.split("=")[0].trim(), Boolean.valueOf(line.split("=")[1].trim()));
                } else {
                    fileContents.put(line.trim(), null);
                }
            }
            Set<String> newProperties = generateDubboProperties(newKeywords);

            for (String element : newProperties) {
                if (!fileContents.containsKey(element)) {
                    fileContents.put(element, true);
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(propertiesFilePath))) {
                for (Map.Entry<String, Boolean> entry : fileContents.entrySet()) {
                    String key = entry.getKey();
                    Boolean value = entry.getValue();
                    if (value == null) {
                        writer.write(key);
                    } else {
                        writer.write(key + " = " + value);
                    }
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 根据提取的关键字生成新的配置
    private static Set<String> generateDubboProperties(Set<String> keywords) {
        Set<String> properties = new LinkedHashSet<>();
        for (String keyword : keywords) {
            properties.add("management.endpoint.dubbo" + keyword + ".enabled");
        }
        return properties;
    }
}

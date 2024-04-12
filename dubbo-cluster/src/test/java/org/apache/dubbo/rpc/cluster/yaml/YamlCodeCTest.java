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
package org.apache.dubbo.rpc.cluster.yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * @author yuluo
 */
class YamlCodeCTest {

    @Test
    void testYamlLoadAs() {

        Yaml yaml = new Yaml();

        InputStream yamlResources = this.getClass().getClassLoader().getResourceAsStream("person.yaml");

        Person person = yaml.loadAs(yamlResources, Person.class);

        Assertions.assertNotNull(yamlResources);
        Assertions.assertNotNull(person);

        Assertions.assertEquals(20, person.getAge());
        Assertions.assertEquals("test", person.getName());
    }

    @Test
    void testInvalidYamlLoadAs() {

        Yaml yaml = new Yaml();

        InputStream invalidYamlStream = this.getClass().getClassLoader().getResourceAsStream("InvalidYamlFile.yaml");

        Assertions.assertThrows(YAMLException.class, () -> yaml.loadAs(invalidYamlStream, Person.class));
    }

    @Test
    void testStringLoadAs() throws IOException {

        Yaml yaml = new Yaml();

        InputStream resource = this.getClass().getClassLoader().getResourceAsStream("person.yaml");

        Assertions.assertNotNull(resource);

        Scanner scanner = new Scanner(resource).useDelimiter("\\A");
        String yamlString = scanner.hasNext() ? scanner.next() : "";
        resource.close();
        scanner.close();

        Person person = yaml.loadAs(yamlString, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals(person.getAge(), 20);
        Assertions.assertEquals(person.getName(), "test");

        Assertions.assertThrows(YAMLException.class, () -> yaml.loadAs("invalied", Person.class));
    }
}

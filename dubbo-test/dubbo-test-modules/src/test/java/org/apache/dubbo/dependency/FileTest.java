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
package org.apache.dubbo.dependency;

import org.apache.dubbo.common.constants.CommonConstants;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FileTest {
    private static final List<Pattern> ignoredModules = new LinkedList<>();
    private static final List<Pattern> ignoredArtifacts = new LinkedList<>();
    private static final List<Pattern> ignoredModulesInDubboAll = new LinkedList<>();

    static {
        ignoredModules.add(Pattern.compile("dubbo-apache-release"));
        ignoredModules.add(Pattern.compile("dubbo-build-tools"));
        ignoredModules.add(Pattern.compile("dubbo-dependencies-all"));
        ignoredModules.add(Pattern.compile("dubbo-parent"));
        ignoredModules.add(Pattern.compile("dubbo-core-spi"));
        ignoredModules.add(Pattern.compile("dubbo-demo.*"));

        ignoredArtifacts.add(Pattern.compile("dubbo-demo.*"));
        ignoredArtifacts.add(Pattern.compile("dubbo-test.*"));

        ignoredModulesInDubboAll.add(Pattern.compile("dubbo"));
        ignoredModulesInDubboAll.add(Pattern.compile("dubbo-bom"));
        ignoredModulesInDubboAll.add(Pattern.compile("dubbo-compiler"));
        ignoredModulesInDubboAll.add(Pattern.compile("dubbo-dependencies.*"));
        ignoredModulesInDubboAll.add(Pattern.compile("dubbo-distribution"));
        ignoredModulesInDubboAll.add(Pattern.compile("dubbo-metadata-processor"));
        ignoredModulesInDubboAll.add(Pattern.compile("dubbo-native.*"));
        ignoredModulesInDubboAll.add(Pattern.compile(".*spring-boot.*"));
        ignoredModulesInDubboAll.add(Pattern.compile("dubbo-maven-plugin"));
    }

    @Test
    void checkDubboBom() throws DocumentException {
        File baseFile = getBaseFile();

        List<File> poms = new LinkedList<>();
        readPoms(baseFile, poms);

        SAXReader reader = new SAXReader();

        List<String> artifactIds = poms.stream()
                .map(f -> {
                    try {
                        return reader.read(f);
                    } catch (DocumentException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(Document::getRootElement)
                .map(doc -> doc.elementText("artifactId"))
                .sorted()
                .collect(Collectors.toList());

        String dubboBomPath = "dubbo-distribution" + File.separator + "dubbo-bom" + File.separator + "pom.xml";
        Document dubboBom = reader.read(new File(getBaseFile(), dubboBomPath));
        List<String> artifactIdsInDubboBom = dubboBom
                .getRootElement()
                .element("dependencyManagement")
                .element("dependencies")
                .elements("dependency")
                .stream()
                .map(ele -> ele.elementText("artifactId"))
                .collect(Collectors.toList());

        List<String> expectedArtifactIds = new LinkedList<>(artifactIds);
        expectedArtifactIds.removeAll(artifactIdsInDubboBom);
        expectedArtifactIds.removeIf(artifactId -> ignoredModules.stream()
                .anyMatch(pattern -> pattern.matcher(artifactId).matches()));

        Assertions.assertTrue(
                expectedArtifactIds.isEmpty(),
                "Newly created modules must be added to dubbo-bom. Found modules: " + expectedArtifactIds);
    }

    @Test
    void checkArtifacts() throws DocumentException, IOException {
        File baseFile = getBaseFile();

        List<File> poms = new LinkedList<>();
        readPoms(baseFile, poms);

        SAXReader reader = new SAXReader();

        List<String> artifactIds = poms.stream()
                .map(f -> {
                    try {
                        return reader.read(f);
                    } catch (DocumentException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(Document::getRootElement)
                .map(doc -> doc.elementText("artifactId"))
                .sorted()
                .collect(Collectors.toList());

        List<String> artifactIdsInRoot = IOUtils.readLines(
                this.getClass()
                        .getClassLoader()
                        .getResource(CommonConstants.DUBBO_VERSIONS_KEY + "/.artifacts")
                        .openStream(),
                StandardCharsets.UTF_8);
        artifactIdsInRoot.removeIf(s -> s.startsWith("#"));

        List<String> expectedArtifactIds = new LinkedList<>(artifactIds);
        expectedArtifactIds.removeAll(artifactIdsInRoot);
        expectedArtifactIds.removeIf(artifactId -> ignoredArtifacts.stream()
                .anyMatch(pattern -> pattern.matcher(artifactId).matches()));

        Assertions.assertTrue(
                expectedArtifactIds.isEmpty(),
                "Newly created modules must be added to .artifacts (in project root). Found modules: "
                        + expectedArtifactIds);
    }

    @Test
    void checkDubboDependenciesAll() throws DocumentException {
        File baseFile = getBaseFile();

        List<File> poms = new LinkedList<>();
        readPoms(baseFile, poms);

        SAXReader reader = new SAXReader();

        List<String> artifactIds = poms.stream()
                .map(f -> {
                    try {
                        return reader.read(f);
                    } catch (DocumentException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(Document::getRootElement)
                .filter(doc -> !Objects.equals("pom", doc.elementText("packaging")))
                .map(doc -> doc.elementText("artifactId"))
                .sorted()
                .collect(Collectors.toList());

        String dubboDependenciesAllPath =
                "dubbo-test" + File.separator + "dubbo-dependencies-all" + File.separator + "pom.xml";
        Document dubboDependenciesAll = reader.read(new File(getBaseFile(), dubboDependenciesAllPath));
        List<String> artifactIdsInDubboDependenciesAll =
                dubboDependenciesAll.getRootElement().element("dependencies").elements("dependency").stream()
                        .map(ele -> ele.elementText("artifactId"))
                        .collect(Collectors.toList());

        List<String> expectedArtifactIds = new LinkedList<>(artifactIds);
        expectedArtifactIds.removeAll(artifactIdsInDubboDependenciesAll);
        expectedArtifactIds.removeIf(artifactId -> ignoredModules.stream()
                .anyMatch(pattern -> pattern.matcher(artifactId).matches()));

        Assertions.assertTrue(
                expectedArtifactIds.isEmpty(),
                "Newly created modules must be added to dubbo-dependencies-all. Found modules: " + expectedArtifactIds);
    }

    @Test
    void checkDubboAllDependencies() throws DocumentException {
        File baseFile = getBaseFile();

        List<File> poms = new LinkedList<>();
        readPoms(baseFile, poms);

        SAXReader reader = new SAXReader();

        List<String> artifactIds = poms.stream()
                .map(f -> {
                    try {
                        return reader.read(f);
                    } catch (DocumentException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(Document::getRootElement)
                .map(doc -> doc.elementText("artifactId"))
                .sorted()
                .collect(Collectors.toList());

        Assertions.assertEquals(poms.size(), artifactIds.size());

        List<String> deployedArtifactIds = poms.stream()
                .map(f -> {
                    try {
                        return reader.read(f);
                    } catch (DocumentException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(Document::getRootElement)
                .filter(doc -> !Objects.equals("pom", doc.elementText("packaging")))
                .filter(doc -> Objects.isNull(doc.element("properties"))
                        || (!Objects.equals("true", doc.element("properties").elementText("skip_maven_deploy"))
                                && !Objects.equals(
                                        "true", doc.element("properties").elementText("maven.deploy.skip"))))
                .map(doc -> doc.elementText("artifactId"))
                .sorted()
                .collect(Collectors.toList());

        String dubboAllPath = "dubbo-distribution" + File.separator + "dubbo-all" + File.separator + "pom.xml";
        Document dubboAll = reader.read(new File(getBaseFile(), dubboAllPath));
        List<String> artifactIdsInDubboAll =
                dubboAll.getRootElement().element("dependencies").elements("dependency").stream()
                        .map(ele -> ele.elementText("artifactId"))
                        .collect(Collectors.toList());

        List<String> expectedArtifactIds = new LinkedList<>(deployedArtifactIds);
        expectedArtifactIds.removeAll(artifactIdsInDubboAll);
        expectedArtifactIds.removeIf(artifactId -> ignoredModules.stream()
                .anyMatch(pattern -> pattern.matcher(artifactId).matches()));
        expectedArtifactIds.removeIf(artifactId -> ignoredModulesInDubboAll.stream()
                .anyMatch(pattern -> pattern.matcher(artifactId).matches()));

        Assertions.assertTrue(
                expectedArtifactIds.isEmpty(),
                "Newly created modules must be added to dubbo-all(dubbo-distribution" + File.separator + "dubbo-all"
                        + File.separator + "pom.xml). Found modules: " + expectedArtifactIds);

        List<String> unexpectedArtifactIds = new LinkedList<>(artifactIdsInDubboAll);
        unexpectedArtifactIds.removeIf(artifactId -> !artifactIds.contains(artifactId));
        unexpectedArtifactIds.removeAll(deployedArtifactIds);
        Assertions.assertTrue(
                unexpectedArtifactIds.isEmpty(),
                "Undeploy dependencies should not be added to dubbo-all(dubbo-distribution" + File.separator
                        + "dubbo-all" + File.separator + "pom.xml). Found modules: " + unexpectedArtifactIds);

        unexpectedArtifactIds = new LinkedList<>();
        for (String artifactId : artifactIdsInDubboAll) {
            if (!artifactIds.contains(artifactId)) {
                continue;
            }
            if (ignoredModules.stream()
                    .anyMatch(pattern -> pattern.matcher(artifactId).matches())) {
                unexpectedArtifactIds.add(artifactId);
            }
            if (ignoredModulesInDubboAll.stream()
                    .anyMatch(pattern -> pattern.matcher(artifactId).matches())) {
                unexpectedArtifactIds.add(artifactId);
            }
        }
        Assertions.assertTrue(
                unexpectedArtifactIds.isEmpty(),
                "Unexpected dependencies should not be added to dubbo-all(dubbo-distribution" + File.separator
                        + "dubbo-all" + File.separator + "pom.xml). Found modules: " + unexpectedArtifactIds);
    }

    @Test
    void checkDubboAllShade() throws DocumentException {
        File baseFile = getBaseFile();

        List<File> poms = new LinkedList<>();
        readPoms(baseFile, poms);

        SAXReader reader = new SAXReader();

        List<String> artifactIds = poms.stream()
                .map(f -> {
                    try {
                        return reader.read(f);
                    } catch (DocumentException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(Document::getRootElement)
                .map(doc -> doc.elementText("artifactId"))
                .sorted()
                .collect(Collectors.toList());

        Assertions.assertEquals(poms.size(), artifactIds.size());

        List<String> deployedArtifactIds = poms.stream()
                .map(f -> {
                    try {
                        return reader.read(f);
                    } catch (DocumentException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(Document::getRootElement)
                .filter(doc -> Objects.isNull(doc.element("properties"))
                        || (!Objects.equals("true", doc.element("properties").elementText("skip_maven_deploy"))
                                && !Objects.equals(
                                        "true", doc.element("properties").elementText("maven.deploy.skip"))))
                .filter(doc -> !Objects.equals("pom", doc.elementText("packaging")))
                .map(doc -> doc.elementText("artifactId"))
                .sorted()
                .collect(Collectors.toList());

        String dubboAllPath = "dubbo-distribution" + File.separator + "dubbo-all" + File.separator + "pom.xml";
        Document dubboAll = reader.read(new File(getBaseFile(), dubboAllPath));
        List<String> artifactIdsInDubboAll =
                dubboAll.getRootElement().element("build").element("plugins").elements("plugin").stream()
                        .filter(ele -> ele.elementText("artifactId").equals("maven-shade-plugin"))
                        .map(ele -> ele.element("executions"))
                        .map(ele -> ele.elements("execution"))
                        .flatMap(Collection::stream)
                        .filter(ele -> ele.elementText("phase").equals("package"))
                        .map(ele -> ele.element("configuration"))
                        .map(ele -> ele.element("artifactSet"))
                        .map(ele -> ele.element("includes"))
                        .map(ele -> ele.elements("include"))
                        .flatMap(Collection::stream)
                        .map(Element::getText)
                        .filter(artifactId -> artifactId.startsWith("org.apache.dubbo:"))
                        .map(artifactId -> artifactId.substring("org.apache.dubbo:".length()))
                        .collect(Collectors.toList());

        List<String> expectedArtifactIds = new LinkedList<>(deployedArtifactIds);
        expectedArtifactIds.removeAll(artifactIdsInDubboAll);
        expectedArtifactIds.removeIf(artifactId -> ignoredModules.stream()
                .anyMatch(pattern -> pattern.matcher(artifactId).matches()));
        expectedArtifactIds.removeIf(artifactId -> ignoredModulesInDubboAll.stream()
                .anyMatch(pattern -> pattern.matcher(artifactId).matches()));

        Assertions.assertTrue(
                expectedArtifactIds.isEmpty(),
                "Newly created modules must be added to dubbo-all (dubbo-distribution" + File.separator + "dubbo-all"
                        + File.separator + "pom.xml in shade plugin). Found modules: " + expectedArtifactIds);

        List<String> unexpectedArtifactIds = new LinkedList<>(artifactIdsInDubboAll);
        unexpectedArtifactIds.removeIf(artifactId -> !artifactIds.contains(artifactId));
        unexpectedArtifactIds.removeAll(deployedArtifactIds);
        Assertions.assertTrue(
                unexpectedArtifactIds.isEmpty(),
                "Undeploy dependencies should not be added to dubbo-all (dubbo-distribution" + File.separator
                        + "dubbo-all" + File.separator + "pom.xml in shade plugin). Found modules: "
                        + unexpectedArtifactIds);

        unexpectedArtifactIds = new LinkedList<>();
        for (String artifactId : artifactIdsInDubboAll) {
            if (!artifactIds.contains(artifactId)) {
                continue;
            }
            if (ignoredModules.stream()
                    .anyMatch(pattern -> pattern.matcher(artifactId).matches())) {
                unexpectedArtifactIds.add(artifactId);
            }
            if (ignoredModulesInDubboAll.stream()
                    .anyMatch(pattern -> pattern.matcher(artifactId).matches())) {
                unexpectedArtifactIds.add(artifactId);
            }
        }
        Assertions.assertTrue(
                unexpectedArtifactIds.isEmpty(),
                "Unexpected dependencies should not be added to dubbo-all (dubbo-distribution" + File.separator
                        + "dubbo-all" + File.separator + "pom.xml in shade plugin). Found modules: "
                        + unexpectedArtifactIds);
    }

    @Test
    void checkDubboAllTransform() throws DocumentException {
        File baseFile = getBaseFile();
        List<String> spis = new LinkedList<>();
        readSPI(baseFile, spis);

        String dubboAllPath = "dubbo-distribution" + File.separator + "dubbo-all" + File.separator + "pom.xml";

        SAXReader reader = new SAXReader();
        Document dubboAll = reader.read(new File(baseFile, dubboAllPath));

        List<String> transformsInDubboAll =
                dubboAll.getRootElement().element("build").element("plugins").elements("plugin").stream()
                        .filter(ele -> ele.elementText("artifactId").equals("maven-shade-plugin"))
                        .map(ele -> ele.element("executions"))
                        .map(ele -> ele.elements("execution"))
                        .flatMap(Collection::stream)
                        .filter(ele -> ele.elementText("phase").equals("package"))
                        .map(ele -> ele.element("configuration"))
                        .map(ele -> ele.element("transformers"))
                        .map(ele -> ele.elements("transformer"))
                        .flatMap(Collection::stream)
                        .map(ele -> ele.elementText("resource"))
                        .map(String::trim)
                        .map(resource -> resource.substring(resource.lastIndexOf("/") + 1))
                        .collect(Collectors.toList());

        List<String> expectedSpis = new LinkedList<>(spis);
        expectedSpis.removeAll(transformsInDubboAll);
        Assertions.assertTrue(
                expectedSpis.isEmpty(),
                "Newly created SPI interface must be added to dubbo-all(dubbo-distribution" + File.separator
                        + "dubbo-all" + File.separator + "pom.xml in shade plugin) to being transformed. Found spis: "
                        + expectedSpis);

        List<String> unexpectedSpis = new LinkedList<>(transformsInDubboAll);
        unexpectedSpis.removeAll(spis);
        Assertions.assertTrue(
                unexpectedSpis.isEmpty(),
                "Class without `@SPI` declaration should not be added to dubbo-all(dubbo-distribution" + File.separator
                        + "dubbo-all" + File.separator + "pom.xml in shade plugin) to being transformed. Found spis: "
                        + unexpectedSpis);
    }

    @Test
    void checkSpiFiles() {
        File baseFile = getBaseFile();
        List<String> spis = new LinkedList<>();
        readSPI(baseFile, spis);

        Map<File, String> spiResources = new HashMap<>();
        readSPIResource(baseFile, spiResources);
        Map<File, String> copyOfSpis = new HashMap<>(spiResources);
        copyOfSpis.entrySet().removeIf(entry -> spis.contains(entry.getValue()));
        Assertions.assertTrue(
                copyOfSpis.isEmpty(),
                "Newly created spi profiles must have a valid class declared with `@SPI`. Found spi profiles: "
                        + copyOfSpis.keySet());

        List<File> unexpectedSpis = new LinkedList<>();
        readSPIUnexpectedResource(baseFile, unexpectedSpis);
        unexpectedSpis.removeIf(file -> file.getAbsolutePath()
                .contains("dubbo-common" + File.separator + "src" + File.separator + "main" + File.separator
                        + "resources" + File.separator + "META-INF" + File.separator + "services" + File.separator
                        + "org.apache.dubbo.common.extension.LoadingStrategy"));
        Assertions.assertTrue(
                unexpectedSpis.isEmpty(),
                "Dubbo native provided spi profiles must filed in `META-INF" + File.separator + "dubbo" + File.separator
                        + "internal`. Please move to proper folder . Found spis: " + unexpectedSpis);
    }

    private static File getBaseFile() {
        File baseFile = new File(new File("").getAbsolutePath());
        while (baseFile != null) {
            if (new File(baseFile, ".asf.yaml").exists()) {
                break;
            }
            baseFile = baseFile.getParentFile();
        }
        Assertions.assertNotNull(baseFile, "Can not find base dir");

        System.out.println("Found Project Base Path: " + baseFile.getAbsolutePath());
        return baseFile;
    }

    public void readPoms(File path, List<File> poms) {
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    readPoms(file, poms);
                }
            }
        } else if (path.isFile()) {
            if (path.getAbsolutePath().contains("target")) {
                return;
            }
            if (path.getName().equals("pom.xml")) {
                poms.add(path);
            }
        }
    }

    public void readSPI(File path, List<String> spis) {
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    readSPI(file, spis);
                }
            }
        } else if (path.isFile()) {
            if (path.getAbsolutePath().contains("target")) {
                return;
            }
            if (path.getAbsolutePath().contains("src" + File.separator + "main" + File.separator + "java")) {
                String content;
                try {
                    content = FileUtils.readFileToString(path, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (content != null && content.contains("@SPI")) {
                    String absolutePath = path.getAbsolutePath();
                    absolutePath = absolutePath.substring(absolutePath.lastIndexOf(
                                    "src" + File.separator + "main" + File.separator + "java" + File.separator)
                            + ("src" + File.separator + "main" + File.separator + "java" + File.separator).length());
                    absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf(".java"));
                    absolutePath = absolutePath.replaceAll(Matcher.quoteReplacement(File.separator), ".");
                    spis.add(absolutePath);
                }
            }
        }
    }

    public void readSPIResource(File path, Map<File, String> spis) {
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    readSPIResource(file, spis);
                }
            }
        } else if (path.isFile()) {
            if (path.getAbsolutePath().contains("target")) {
                return;
            }
            if (path.getAbsolutePath()
                    .contains("src" + File.separator + "main" + File.separator + "resources" + File.separator
                            + "META-INF" + File.separator + "dubbo" + File.separator + "internal" + File.separator)) {
                String absolutePath = path.getAbsolutePath();
                absolutePath = absolutePath.substring(absolutePath.lastIndexOf("src" + File.separator + "main"
                                + File.separator + "resources" + File.separator + "META-INF" + File.separator + "dubbo"
                                + File.separator + "internal" + File.separator)
                        + ("src" + File.separator + "main" + File.separator + "resources" + File.separator + "META-INF"
                                        + File.separator + "dubbo" + File.separator + "internal" + File.separator)
                                .length());
                absolutePath = absolutePath.replaceAll(Matcher.quoteReplacement(File.separator), ".");
                spis.put(path, absolutePath);
            }
        }
    }

    public void readSPIUnexpectedResource(File path, List<File> spis) {
        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    readSPIUnexpectedResource(file, spis);
                }
            }
        } else if (path.isFile()) {
            if (path.getAbsolutePath().contains("target")) {
                return;
            }
            if (path.getAbsolutePath()
                    .contains("src" + File.separator + "main" + File.separator + "resources" + File.separator
                            + "META-INF" + File.separator + "dubbo" + File.separator + "org.apache.dubbo")) {
                spis.add(path);
            }
            if (path.getAbsolutePath()
                    .contains("src" + File.separator + "main" + File.separator + "resources" + File.separator
                            + "META-INF" + File.separator + "dubbo" + File.separator + "com.alibaba.dubbo")) {
                spis.add(path);
            }
            if (path.getAbsolutePath()
                    .contains("src" + File.separator + "main" + File.separator + "resources" + File.separator
                            + "META-INF" + File.separator + "services" + File.separator + "org.apache.dubbo")) {
                spis.add(path);
            }
            if (path.getAbsolutePath()
                    .contains("src" + File.separator + "main" + File.separator + "resources" + File.separator
                            + "META-INF" + File.separator + "services" + File.separator + "com.alibaba.dubbo")) {
                spis.add(path);
            }

            if (path.getAbsolutePath()
                    .contains("src" + File.separator + "main" + File.separator + "resources" + File.separator
                            + "META-INF.dubbo" + File.separator + "org.apache.dubbo")) {
                spis.add(path);
            }
            if (path.getAbsolutePath()
                    .contains("src" + File.separator + "main" + File.separator + "resources" + File.separator
                            + "META-INF.dubbo" + File.separator + "com.alibaba.dubbo")) {
                spis.add(path);
            }
            if (path.getAbsolutePath()
                    .contains("src" + File.separator + "main" + File.separator + "resources" + File.separator
                            + "META-INF.services" + File.separator + "org.apache.dubbo")) {
                spis.add(path);
            }
            if (path.getAbsolutePath()
                    .contains("src" + File.separator + "main" + File.separator + "resources" + File.separator
                            + "META-INF.services" + File.separator + "com.alibaba.dubbo")) {
                spis.add(path);
            }
            if (path.getAbsolutePath()
                    .contains("src" + File.separator + "main" + File.separator + "resources" + File.separator
                            + "META-INF.dubbo.internal" + File.separator + "org.apache.dubbo")) {
                spis.add(path);
            }
            if (path.getAbsolutePath()
                    .contains("src" + File.separator + "main" + File.separator + "resources" + File.separator
                            + "META-INF.dubbo.internal" + File.separator + "com.alibaba.dubbo")) {
                spis.add(path);
            }
        }
    }
}

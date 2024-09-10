/*
 * Copyright (c) 2019 Maven Protocol Buffers Plugin Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.maven.plugin.protoc;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.SystemPropertyConfigUtils;
import org.apache.dubbo.maven.plugin.protoc.command.DefaultProtocCommandBuilder;
import org.apache.dubbo.maven.plugin.protoc.enums.DubboGenerateTypeEnum;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.codehaus.plexus.util.FileUtils.cleanDirectory;
import static org.codehaus.plexus.util.FileUtils.copyStreamToFile;
import static org.codehaus.plexus.util.FileUtils.getFiles;

@Mojo(
    name = "compile",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    threadSafe = true
)
public class DubboProtocCompilerMojo extends AbstractMojo {
    @Parameter(property = "protoSourceDir", defaultValue = "${basedir}/src/main/proto")
    private File protoSourceDir;
    @Parameter(property = "outputDir", defaultValue = "${project.build.directory}/generated-sources/protobuf/java")
    private File outputDir;
    @Parameter(required = false, property = "dubboVersion")
    private String dubboVersion;
    @Parameter(required = true, readonly = true, defaultValue = "${project.remoteArtifactRepositories}")
    private List<ArtifactRepository> remoteRepositories;
    @Parameter(required = false, property = "protocExecutable")
    private String protocExecutable;
    @Parameter(required = false, property = "protocArtifact")
    private String protocArtifact;
    @Parameter(required = false, property = "protocVersion")
    private String protocVersion;
    @Parameter(required = false, defaultValue = "${project.build.directory}/protoc-plugins")
    private File protocPluginDirectory;
    @Parameter(required = true, defaultValue = "${project.build.directory}/protoc-dependencies")
    private File temporaryProtoFileDirectory;
    @Parameter(required = true, property = "dubboGenerateType", defaultValue = "tri")
    private String dubboGenerateType;
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;
    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;
    @Parameter(required = true, readonly = true, property = "localRepository")
    private ArtifactRepository localRepository;
    @Component
    private ArtifactFactory artifactFactory;
    @Component
    private RepositorySystem repositorySystem;
    @Component
    private ResolutionErrorHandler resolutionErrorHandler;
    @Component
    protected MavenProjectHelper projectHelper;
    @Component
    protected BuildContext buildContext;
    final CommandLineUtils.StringStreamConsumer output = new CommandLineUtils.StringStreamConsumer();
    final CommandLineUtils.StringStreamConsumer error = new CommandLineUtils.StringStreamConsumer();
    private final DefaultProtocCommandBuilder defaultProtocCommandBuilder = new DefaultProtocCommandBuilder();
    private final DubboProtocPluginWrapperFactory dubboProtocPluginWrapperFactory = new DubboProtocPluginWrapperFactory();

    public void execute() throws MojoExecutionException, MojoFailureException {
        Properties versionMatrix = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader.getResourceAsStream("version-matrix.properties");
        try {
            versionMatrix.load(stream);
        } catch (IOException e) {
            getLog().warn("Unable to load default version matrix", e);
        }

        if (dubboVersion == null) {
            dubboVersion = versionMatrix.getProperty("dubbo.version");
        }
        if (protocVersion == null) {
            protocVersion = versionMatrix.getProperty("protoc.version");
        }
        if (protocArtifact == null) {
            final String osName = SystemPropertyConfigUtils.getSystemProperty(CommonConstants.SystemProperty.SYSTEM_OS_NAME);
            final String osArch = SystemPropertyConfigUtils.getSystemProperty(CommonConstants.SystemProperty.OS_ARCH);

            final String detectedName = normalizeOs(osName);
            final String detectedArch = normalizeArch(osArch);

            protocArtifact = "com.google.protobuf:protoc:" + protocVersion + ":exe:" + detectedName + '-' + detectedArch;
        }

        if (protocExecutable == null && protocArtifact != null) {
            final Artifact artifact = createProtocArtifact(protocArtifact);
            final File file = resolveBinaryArtifact(artifact);
            protocExecutable = file.getAbsolutePath();
        }
        if (protocExecutable == null) {
            getLog().warn("No 'protocExecutable' parameter is configured, using the default: 'protoc'");
            protocExecutable = "protoc";
        }
        getLog().info("using protocExecutable: " + protocExecutable);
        DubboProtocPlugin dubboProtocPlugin = buildDubboProtocPlugin(dubboVersion, dubboGenerateType, protocPluginDirectory);
        getLog().info("build dubbo protoc plugin:" + dubboProtocPlugin + " success");
        List<String> commandArgs = defaultProtocCommandBuilder.buildProtocCommandArgs(new ProtocMetaData(protocExecutable, makeAllProtoPaths(), findAllProtoFiles(protoSourceDir), outputDir, dubboProtocPlugin
        ));
        if (!outputDir.exists()) {
            FileUtils.mkdir(outputDir.getAbsolutePath());
        }
        try {
            int exitStatus = executeCommandLine(commandArgs);
            getLog().info("execute commandLine finished with exit code: " + exitStatus);
            if (exitStatus != 0) {
                getLog().error("PROTOC FAILED: " + getError());
                throw new MojoFailureException(
                    "protoc did not exit cleanly. Review output for more information.");
            } else if (StringUtils.isNotBlank(getError())) {
                getLog().warn("PROTOC: " + getError());
            } else {
                linkProtoFilesToMaven();
            }
        } catch (CommandLineException e) {
            throw new MojoExecutionException(e);
        }
    }

    private static String normalizeOs(String value) {
        value = normalize(value);
        if (value.startsWith("linux")) {
            return "linux";
        }
        if (value.startsWith("mac") || value.startsWith("osx")) {
            return "osx";
        }
        if (value.startsWith("windows")) {
            return "windows";
        }

        return "unknown";
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }

    private static String normalizeArch(String value) {
        value = normalize(value);
        if (value.matches("^(x8664|amd64|ia32e|em64t|x64)$")) {
            return "x86_64";
        }
        if ("aarch64".equals(value)) {
            return "aarch_64";
        }
        return "unknown";
    }

    public void linkProtoFilesToMaven() {
        linkProtoSources();
        linkGeneratedFiles();
    }

    public void linkProtoSources() {
        projectHelper.addResource(project, protoSourceDir.getAbsolutePath(),
            Collections.singletonList("**/*.proto*"), Collections.singletonList(""));
    }

    public void linkGeneratedFiles() {
        project.addCompileSourceRoot(outputDir.getAbsolutePath());
        buildContext.refresh(outputDir);
    }

    public List<File> findAllProtoFiles(final File protoSourceDir) {
        if (protoSourceDir == null) {
            throw new RuntimeException("'protoSourceDir' is null");
        }
        if (!protoSourceDir.isDirectory()) {
            throw new RuntimeException(format("%s is not a directory", protoSourceDir));
        }
        final List<File> protoFilesInDirectory;
        try {
            protoFilesInDirectory = getFiles(protoSourceDir, "**/*.proto*", "");
        } catch (IOException e) {
            throw new RuntimeException("Unable to retrieve the list of files: " + e.getMessage(), e);
        }
        getLog().info("protoFilesInDirectory: " + protoFilesInDirectory);
        return protoFilesInDirectory;
    }

    public int executeCommandLine(List<String> commandArgs) throws CommandLineException {
        final Commandline cl = new Commandline();
        cl.setExecutable(protocExecutable);
        cl.addArguments(commandArgs.toArray(new String[]{}));
        int attemptsLeft = 3;
        while (true) {
            try {
                getLog().info("commandLine:" + cl.toString());
                return CommandLineUtils.executeCommandLine(cl, null, output, error);
            } catch (CommandLineException e) {
                if (--attemptsLeft == 0 || e.getCause() == null) {
                    throw e;
                }
                getLog().warn("[PROTOC] Unable to invoke protoc, will retry " + attemptsLeft + " time(s)", e);
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    private DubboProtocPlugin buildDubboProtocPlugin(String dubboVersion, String dubboGenerateType, File protocPluginDirectory) {
        DubboProtocPlugin dubboProtocPlugin = new DubboProtocPlugin();
        DubboGenerateTypeEnum dubboGenerateTypeEnum = DubboGenerateTypeEnum.getByType(dubboGenerateType);
        if (dubboGenerateTypeEnum == null) {
            throw new RuntimeException(" can not find the dubboGenerateType: " + dubboGenerateType + ",please check it !");
        }
        dubboProtocPlugin.setId(dubboGenerateType);
        dubboProtocPlugin.setMainClass(dubboGenerateTypeEnum.getMainClass());
        dubboProtocPlugin.setDubboVersion(dubboVersion);
        dubboProtocPlugin.setPluginDirectory(protocPluginDirectory);
        dubboProtocPlugin.setJavaHome(SystemPropertyConfigUtils.getSystemProperty(CommonConstants.SystemProperty.JAVA_HOME));
        DubboProtocPluginWrapper protocPluginWrapper = dubboProtocPluginWrapperFactory.findByOs();
        dubboProtocPlugin.setResolvedJars(resolvePluginDependencies());
        File protocPlugin = protocPluginWrapper.createProtocPlugin(dubboProtocPlugin, getLog());
        boolean debugEnabled = getLog().isDebugEnabled();
        if (debugEnabled) {
            getLog().debug("protocPlugin: " + protocPlugin.getAbsolutePath());
        }
        dubboProtocPlugin.setProtocPlugin(protocPlugin);
        return dubboProtocPlugin;
    }

    private List<File> resolvePluginDependencies() {
        List<File> resolvedJars = new ArrayList<>();
        final VersionRange versionSpec;
        try {
            versionSpec = VersionRange.createFromVersionSpec(dubboVersion);
        } catch (InvalidVersionSpecificationException e) {
            throw new RuntimeException("Invalid plugin version specification", e);
        }
        final Artifact protocPluginArtifact =
            artifactFactory.createDependencyArtifact(
                "org.apache.dubbo",
                "dubbo-compiler",
                versionSpec,
                "jar",
                "",
                Artifact.SCOPE_RUNTIME);
        final ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact(project.getArtifact())
            .setResolveRoot(false)
            .setArtifactDependencies(Collections.singleton(protocPluginArtifact))
            .setManagedVersionMap(emptyMap())
            .setLocalRepository(localRepository)
            .setRemoteRepositories(remoteRepositories)
            .setOffline(session.isOffline())
            .setForceUpdate(session.getRequest().isUpdateSnapshots())
            .setServers(session.getRequest().getServers())
            .setMirrors(session.getRequest().getMirrors())
            .setProxies(session.getRequest().getProxies());

        final ArtifactResolutionResult result = repositorySystem.resolve(request);

        try {
            resolutionErrorHandler.throwErrors(request, result);
        } catch (ArtifactResolutionException e) {
            throw new RuntimeException("Unable to resolve plugin artifact: " + e.getMessage(), e);
        }

        final Set<Artifact> artifacts = result.getArtifacts();

        if (artifacts == null || artifacts.isEmpty()) {
            throw new RuntimeException("Unable to resolve plugin artifact");
        }

        for (final Artifact artifact : artifacts) {
            resolvedJars.add(artifact.getFile());
        }

        if (getLog().isDebugEnabled()) {
            getLog().debug("Resolved jars: " + resolvedJars);
        }
        return resolvedJars;
    }

    protected Artifact createProtocArtifact(final String artifactSpec) {
        final String[] parts = artifactSpec.split(":");
        if (parts.length < 3 || parts.length > 5) {
            throw new RuntimeException(
                "Invalid artifact specification format"
                    + ", expected: groupId:artifactId:version[:type[:classifier]]"
                    + ", actual: " + artifactSpec);
        }
        final String type = parts.length >= 4 ? parts[3] : "exe";
        final String classifier = parts.length == 5 ? parts[4] : null;
        // parts: [com.google.protobuf, protoc, 3.6.0, exe, osx-x86_64]
        getLog().info("parts: " + Arrays.toString(parts));
        return createDependencyArtifact(parts[0], parts[1], parts[2], type, classifier);
    }

    protected Artifact createDependencyArtifact(
        final String groupId,
        final String artifactId,
        final String version,
        final String type,
        final String classifier
    ) {
        final VersionRange versionSpec;
        try {
            versionSpec = VersionRange.createFromVersionSpec(version);
        } catch (final InvalidVersionSpecificationException e) {
            throw new RuntimeException("Invalid version specification", e);
        }
        return artifactFactory.createDependencyArtifact(
            groupId,
            artifactId,
            versionSpec,
            type,
            classifier,
            Artifact.SCOPE_RUNTIME);
    }

    protected File resolveBinaryArtifact(final Artifact artifact) {
        final ArtifactResolutionResult result;
        final ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact(project.getArtifact())
            .setResolveRoot(false)
            .setResolveTransitively(false)
            .setArtifactDependencies(singleton(artifact))
            .setManagedVersionMap(emptyMap())
            .setLocalRepository(localRepository)
            .setRemoteRepositories(remoteRepositories)
            .setOffline(session.isOffline())
            .setForceUpdate(session.getRequest().isUpdateSnapshots())
            .setServers(session.getRequest().getServers())
            .setMirrors(session.getRequest().getMirrors())
            .setProxies(session.getRequest().getProxies());
        result = repositorySystem.resolve(request);
        try {
            resolutionErrorHandler.throwErrors(request, result);
        } catch (final ArtifactResolutionException e) {
            throw new RuntimeException("Unable to resolve artifact: " + e.getMessage(), e);
        }

        final Set<Artifact> artifacts = result.getArtifacts();

        if (artifacts == null || artifacts.isEmpty()) {
            throw new RuntimeException("Unable to resolve artifact");
        }

        final Artifact resolvedBinaryArtifact = artifacts.iterator().next();
        if (getLog().isDebugEnabled()) {
            getLog().debug("Resolved artifact: " + resolvedBinaryArtifact);
        }

        final File sourceFile = resolvedBinaryArtifact.getFile();
        final String sourceFileName = sourceFile.getName();
        final String targetFileName;
        if (Os.isFamily(Os.FAMILY_WINDOWS) && !sourceFileName.endsWith(".exe")) {
            targetFileName = sourceFileName + ".exe";
        } else {
            targetFileName = sourceFileName;
        }
        final File targetFile = new File(protocPluginDirectory, targetFileName);
        if (targetFile.exists()) {
            getLog().debug("Executable file already exists: " + targetFile.getAbsolutePath());
            return targetFile;
        }
        try {
            FileUtils.forceMkdir(protocPluginDirectory);
        } catch (final IOException e) {
            throw new RuntimeException("Unable to create directory " + protocPluginDirectory, e);
        }
        try {
            FileUtils.copyFile(sourceFile, targetFile);
        } catch (final IOException e) {
            throw new RuntimeException("Unable to copy the file to " + protocPluginDirectory, e);
        }
        if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
            boolean b = targetFile.setExecutable(true);
            if (!b) {
                throw new RuntimeException("Unable to make executable: " + targetFile.getAbsolutePath());
            }
        }

        if (getLog().isDebugEnabled()) {
            getLog().debug("Executable file: " + targetFile.getAbsolutePath());
        }
        return targetFile;
    }

    protected Set<File> makeAllProtoPaths() {
        File temp = temporaryProtoFileDirectory;
        if (temp.exists()) {
            try {
                cleanDirectory(temp);
            } catch (IOException e) {
                throw new RuntimeException("Unable to clean up temporary proto file directory", e);
            }
        }
        Set<File> protoDirectories = new LinkedHashSet<>();
        if (protoSourceDir.exists()) {
            protoDirectories.add(protoSourceDir);
        }
        //noinspection deprecation
        for (Artifact artifact : project.getCompileArtifacts()) {
            File file = artifact.getFile();
            if (file.isFile() && file.canRead() && !file.getName().endsWith(".xml")) {
                try (JarFile jar = new JarFile(file)) {
                    Enumeration<JarEntry> jarEntries = jar.entries();
                    while (jarEntries.hasMoreElements()) {
                        JarEntry jarEntry = jarEntries.nextElement();
                        String jarEntryName = jarEntry.getName();
                        if (jarEntryName.endsWith(".proto")) {
                            File targetDirectory;
                            try {
                                targetDirectory = new File(temp, hash(jar.getName()));
                                String canonicalTargetDirectoryPath = targetDirectory.getCanonicalPath();
                                File target = new File(targetDirectory, jarEntryName);
                                String canonicalTargetPath = target.getCanonicalPath();
                                if (!canonicalTargetPath.startsWith(canonicalTargetDirectoryPath + File.separator)) {
                                    throw new RuntimeException(
                                            "ZIP SLIP: Entry " + jarEntry.getName() + " in " + jar.getName()
                                                    + " is outside of the target dir");
                                }
                                FileUtils.mkdir(target.getParentFile().getAbsolutePath());
                                copyStreamToFile(new RawInputStreamFacade(jar.getInputStream(jarEntry)), target);
                            } catch (IOException e) {
                                throw new RuntimeException("Unable to unpack proto files", e);
                            }
                            protoDirectories.add(targetDirectory);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Not a readable JAR artifact: " + file.getAbsolutePath(), e);
                }
            } else if (file.isDirectory()) {
                List<File> protoFiles;
                try {
                    protoFiles = getFiles(file, "**/*.proto", null);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to scan for proto files in: " + file.getAbsolutePath(), e);
                }
                if (!protoFiles.isEmpty()) {
                    protoDirectories.add(file);
                }
            }
        }
        return protoDirectories;
    }

    private static String hash(String input) {
        try {
            byte[] bytes = MessageDigest.getInstance("MD5").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create MD5 digest", e);
        }
    }

    public String getError() {
        return fixUnicodeOutput(error.getOutput());
    }

    public String getOutput() {
        return fixUnicodeOutput(output.getOutput());
    }

    private static String fixUnicodeOutput(final String message) {
        return new String(message.getBytes(), StandardCharsets.UTF_8);
    }
}

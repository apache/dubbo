package org.apache.dubbo.xds.bootstrap;

import javax.annotation.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.grpc.Internal;
import io.grpc.InternalLogId;
import io.grpc.internal.JsonParser;

import org.apache.dubbo.xds.XdsInitializationException;
import org.apache.dubbo.xds.XdsLogger;
import org.apache.dubbo.xds.XdsLogger.XdsLogLevel;
import org.apache.dubbo.xds.bootstrap.EnvoyProtoData.Node;

import static com.google.common.base.Preconditions.checkArgument;

@Internal
public class Bootstrapper {

    public static final String XDSTP_SCHEME = "xdstp:";
    private static final String BOOTSTRAP_PATH_SYS_ENV_VAR = "GRPC_XDS_BOOTSTRAP";
    private static final String BOOTSTRAP_CONFIG_SYS_ENV_VAR = "GRPC_XDS_BOOTSTRAP_CONFIG";
    private static final String DEFAULT_BOOTSTRAP_PATH = "/etc/istio/proxy/grpc-bootstrap.json";
    public static final String CLIENT_FEATURE_DISABLE_OVERPROVISIONING = "envoy.lb.does_not_support_overprovisioning";
    public static final String CLIENT_FEATURE_RESOURCE_IN_SOTW = "xds.config.resource-in-sotw";
    private static final String SERVER_FEATURE_IGNORE_RESOURCE_DELETION = "ignore_resource_deletion";
    private static final String SERVER_FEATURE_XDS_V3 = "xds_v3";

    protected final XdsLogger logger;
    protected FileReader reader = LocalFileReader.INSTANCE;

    @VisibleForTesting
    public String bootstrapPathFromEnvVar = System.getenv(BOOTSTRAP_PATH_SYS_ENV_VAR);
    @VisibleForTesting
    public String bootstrapConfigFromEnvVar = System.getenv(BOOTSTRAP_CONFIG_SYS_ENV_VAR);

    public Bootstrapper() {
        logger = XdsLogger.withLogId(InternalLogId.allocate("bootstrapper", null));
    }

    public BootstrapInfo bootstrap() throws XdsInitializationException {
        String jsonContent;
        try {
            jsonContent = getJsonContent();
        } catch (IOException e) {
            throw new XdsInitializationException("Fail to read bootstrap file", e);
        }

        if (jsonContent == null) {
            //TODO:try loading from Dubbo control panel and user specified URL
        }

        Map<String, ?> rawBootstrap;
        try {
            rawBootstrap = (Map<String, ?>) JsonParser.parse(jsonContent);
        } catch (IOException e) {
            throw new XdsInitializationException("Failed to parse JSON", e);
        }

        logger.log(XdsLogLevel.DEBUG, "Bootstrap configuration:\n{0}", rawBootstrap);
        return null;
    }

    private String getJsonContent() throws IOException, XdsInitializationException {
        String jsonContent;
        String filePath = null;

        // Check the default path
        if (Files.exists(Paths.get(DEFAULT_BOOTSTRAP_PATH))) {
            filePath = DEFAULT_BOOTSTRAP_PATH;
        } else if (Files.exists(Paths.get(bootstrapPathFromEnvVar))) {
            // Check environment variable and system property
            filePath = bootstrapPathFromEnvVar;
        }

        if (filePath != null) {
            logger.log(XdsLogLevel.INFO, "Reading bootstrap file from {0}", filePath);
            jsonContent = reader.readFile(filePath);
            logger.log(XdsLogLevel.INFO, "Reading bootstrap from " + filePath);
        } else {
            jsonContent = null;
        }

        return jsonContent;
    }

    public class ServerInfo {
        private final String target;
        private final Object implSpecificConfig;
        private final boolean ignoreResourceDeletion;

        public ServerInfo(String target, Object implSpecificConfig, boolean ignoreResourceDeletion) {
            this.target = target;
            this.implSpecificConfig = implSpecificConfig;
            this.ignoreResourceDeletion = ignoreResourceDeletion;
        }

        public String getTarget() {
            return target;
        }

        public Object getImplSpecificConfig() {
            return implSpecificConfig;
        }

        public boolean isIgnoreResourceDeletion() {
            return ignoreResourceDeletion;
        }

        public ServerInfo create(String target, Object implSpecificConfig) {
            return new ServerInfo(target, implSpecificConfig, false);
        }

        public ServerInfo create(String target, Object implSpecificConfig, boolean ignoreResourceDeletion) {
            return new ServerInfo(target, implSpecificConfig, ignoreResourceDeletion);
        }

        @Override
        public String toString() {
            return "ServerInfo{" + "target='" + target + '\'' + ", implSpecificConfig=" + implSpecificConfig
                    + ", ignoreResourceDeletion=" + ignoreResourceDeletion + '}';
        }
    }

    @Internal
    public class CertificateProviderInfo {
        private final String pluginName;
        private final Map<String, ?> config;

        public CertificateProviderInfo(String pluginName, Map<String, ?> config) {
            this.pluginName = pluginName;
            this.config = Collections.unmodifiableMap(config);
        }

        public String getPluginName() {
            return pluginName;
        }

        public Map<String, ?> getConfig() {
            return config;
        }

        public CertificateProviderInfo create(String pluginName, Map<String, ?> config) {
            return new CertificateProviderInfo(pluginName, config);
        }

        @Override
        public String toString() {
            return "CertificateProviderInfo{" + "pluginName='" + pluginName + '\'' + ", config=" + config + '}';
        }
    }

    public class AuthorityInfo {
        private final String clientListenerResourceNameTemplate;
        private final ImmutableList<ServerInfo> xdsServers;

        public AuthorityInfo(String clientListenerResourceNameTemplate, List<ServerInfo> xdsServers) {
            checkArgument(!xdsServers.isEmpty(), "xdsServers must not be empty");
            this.clientListenerResourceNameTemplate = clientListenerResourceNameTemplate;
            this.xdsServers = ImmutableList.copyOf(xdsServers);
        }

        public String getClientListenerResourceNameTemplate() {
            return clientListenerResourceNameTemplate;
        }

        public ImmutableList<ServerInfo> getXdsServers() {
            return xdsServers;
        }

        public AuthorityInfo create(String clientListenerResourceNameTemplate, List<ServerInfo> xdsServers) {
            return new AuthorityInfo(clientListenerResourceNameTemplate, xdsServers);
        }

        @Override
        public String toString() {
            return "AuthorityInfo{" + "clientListenerResourceNameTemplate='" + clientListenerResourceNameTemplate + '\''
                    + ", xdsServers=" + xdsServers + '}';
        }
    }

    public class BootstrapInfo {
        private final ImmutableList<ServerInfo> servers;
        private final Node node;
        @Nullable
        private final ImmutableMap<String, CertificateProviderInfo> certProviders;
        @Nullable
        private final String serverListenerResourceNameTemplate;
        private final String clientDefaultListenerResourceNameTemplate;
        private final ImmutableMap<String, AuthorityInfo> authorities;

        private BootstrapInfo(Builder builder) {
            this.servers = ImmutableList.copyOf(builder.servers);
            this.node = builder.node;
            this.certProviders = builder.certProviders == null ? null : ImmutableMap.copyOf(builder.certProviders);
            this.serverListenerResourceNameTemplate = builder.serverListenerResourceNameTemplate;
            this.clientDefaultListenerResourceNameTemplate = builder.clientDefaultListenerResourceNameTemplate;
            this.authorities = ImmutableMap.copyOf(builder.authorities);
        }

        public ImmutableList<ServerInfo> getServers() {
            return servers;
        }

        public Node getNode() {
            return node;
        }

        @Nullable
        public ImmutableMap<String, CertificateProviderInfo> getCertProviders() {
            return certProviders;
        }

        @Nullable
        public String getServerListenerResourceNameTemplate() {
            return serverListenerResourceNameTemplate;
        }

        public String getClientDefaultListenerResourceNameTemplate() {
            return clientDefaultListenerResourceNameTemplate;
        }

        public ImmutableMap<String, AuthorityInfo> getAuthorities() {
            return authorities;
        }

        public Builder builder() {
            return new Builder().clientDefaultListenerResourceNameTemplate("%s")
                    .authorities(ImmutableMap.of());
        }

        public class Builder {
            private List<ServerInfo> servers;
            private Node node;
            private Map<String, CertificateProviderInfo> certProviders;
            private String serverListenerResourceNameTemplate;
            private String clientDefaultListenerResourceNameTemplate;
            private Map<String, AuthorityInfo> authorities;

            public Builder servers(List<ServerInfo> servers) {
                this.servers = servers;
                return this;
            }

            public Builder node(Node node) {
                this.node = node;
                return this;
            }

            public Builder certProviders(@Nullable Map<String, CertificateProviderInfo> certProviders) {
                this.certProviders = certProviders;
                return this;
            }

            public Builder serverListenerResourceNameTemplate(@Nullable String serverListenerResourceNameTemplate) {
                this.serverListenerResourceNameTemplate = serverListenerResourceNameTemplate;
                return this;
            }

            public Builder clientDefaultListenerResourceNameTemplate(String clientDefaultListenerResourceNameTemplate) {
                this.clientDefaultListenerResourceNameTemplate = clientDefaultListenerResourceNameTemplate;
                return this;
            }

            public Builder authorities(Map<String, AuthorityInfo> authorities) {
                this.authorities = authorities;
                return this;
            }

            public BootstrapInfo build() {
                return new BootstrapInfo(this);
            }
        }

        @Override
        public String toString() {
            return "BootstrapInfo{" + "servers=" + servers + ", node=" + node + ", certProviders=" + certProviders
                    + ", serverListenerResourceNameTemplate='" + serverListenerResourceNameTemplate + '\''
                    + ", clientDefaultListenerResourceNameTemplate='" + clientDefaultListenerResourceNameTemplate + '\''
                    + ", authorities=" + authorities + '}';
        }
    }

    @VisibleForTesting
    public void setFileReader(FileReader reader) {
        this.reader = reader;
    }

    public interface FileReader {
        String readFile(String path) throws IOException;
    }

    protected enum LocalFileReader implements FileReader {
        INSTANCE;

        @Override
        public String readFile(String path) throws IOException {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        }
    }

}


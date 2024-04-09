package org.apache.dubbo.xds.listener;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.security.authz.RuleSource;
import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;
import org.apache.dubbo.xds.security.authz.rule.matcher.IpMatcher;
import org.apache.dubbo.xds.security.authz.rule.matcher.KeyMatcher;
import org.apache.dubbo.xds.security.authz.rule.matcher.Matchers;
import org.apache.dubbo.xds.security.authz.rule.matcher.StringMatcher;
import org.apache.dubbo.xds.security.authz.rule.tree.CompositeRuleNode;
import org.apache.dubbo.xds.security.authz.rule.tree.LeafRuleNode;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleNode;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleNode.Relation;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleRoot;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleRoot.Action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.envoyproxy.envoy.config.listener.v3.Filter;
import io.envoyproxy.envoy.config.listener.v3.FilterChain;
import io.envoyproxy.envoy.config.listener.v3.Listener;
import io.envoyproxy.envoy.config.rbac.v3.Permission;
import io.envoyproxy.envoy.config.rbac.v3.Policy;
import io.envoyproxy.envoy.config.rbac.v3.Principal;
import io.envoyproxy.envoy.config.rbac.v3.Principal.IdentifierCase;
import io.envoyproxy.envoy.config.rbac.v3.RBAC;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter;
import io.envoyproxy.envoy.type.matcher.v3.MetadataMatcher.PathSegment;

import static org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty.AUTHENTICATED;
import static org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty.DIRECT_REMOTE_IP;
import static org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty.HEADER;
import static org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty.REMOTE_IP;
import static org.apache.dubbo.xds.security.authz.rule.tree.RuleNode.Relation.AND;

/**
 * Filter for authentication rules
 *
 * @author lwj
 * @since 2.0.0
 */
public class RbacLdsListener implements LdsListener, RuleSource {

    protected static final String LDS_RBAC_FILTER = "envoy.filters.http.rbac";

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RbacLdsListener.class);

    public RbacLdsListener(ApplicationModel applicationModel) {

    }

    public void resolveRbac(List<HttpFilter> httpFilters, Map<String, RuleRoot> roots) {
        //action (ALLOW/DENY/...) -> policies
        Map<RBAC.Action, RBAC> rbacMap = new HashMap<>();
        for (HttpFilter httpFilter : httpFilters) {
            if (!httpFilter.getName()
                    .equals(LDS_RBAC_FILTER)) {
                continue;
            }
            try {
                io.envoyproxy.envoy.extensions.filters.http.rbac.v3.RBAC rbac = httpFilter.getTypedConfig()
                        .unpack(io.envoyproxy.envoy.extensions.filters.http.rbac.v3.RBAC.class);
                if (rbac != null) {
                    /**There are multiple duplicates, and we only choose one of them */
                    if (!rbacMap.containsKey(rbac.getRules()
                            .getAction())) {
                        rbacMap.put(rbac.getRules()
                                .getAction(), rbac.getRules());
                    }

                }
            } catch (InvalidProtocolBufferException e) {
                logger.warn("", "", "", "[XdsDataSource] Parsing RbacRule error", e);
            }
        }

        for (Entry<RBAC.Action, RBAC> rbacEntry : rbacMap.entrySet()) {
            RBAC.Action action = rbacEntry.getKey();
            RBAC rbac = rbacEntry.getValue();
            /*
              单个RBAC rule根节点
             */
            RuleRoot ruleNode = new RuleRoot(AND, action.equals(RBAC.Action.ALLOW) ? Action.ALLOW : Action.DENY, "rules");

            //policies:  "service-admin"、"product-viewer"
            for (Entry<String, Policy> entry : rbac.getPoliciesMap()
                    .entrySet()) {

                //rule下的单个policy,包含一个principals Node和 permissions Node，两Node之间AND关系
                CompositeRuleNode policyNode = new CompositeRuleNode(entry.getKey(), AND);

                //每个policy下可以多个principal，之间OR关系
                CompositeRuleNode principalNode = new CompositeRuleNode("principals", Relation.OR);

                List<Principal> principals = entry.getValue()
                        .getPrincipalsList();

                for (Principal principal : principals) {
                    //解析单个Principal到node
                    RuleNode principalAnd = resolvePrincipal(principal);
                    if (principalAnd != null) {
                        principalNode.addChild(principalAnd);
                    }
                }

                if (!principals.isEmpty()) {
                    policyNode.addChild(principalNode);
                }

                CompositeRuleNode permissionNode = new CompositeRuleNode("permissions", Relation.OR);
                List<Permission> permissions = entry.getValue()
                        .getPermissionsList();
                for (Permission permission : permissions) {
                    RuleNode permissionRule = resolvePermission(permission);
                    if (permissionRule != null) {
                        permissionNode.addChild(permissionRule);
                    }
                }

                if (!permissions.isEmpty()) {
                    policyNode.addChild(permissionNode);
                }

                ruleNode.addChild(policyNode);
                roots.put(ruleNode.getNodeName(), ruleNode);
            }
        }
    }

    public static HttpConnectionManager unpackHttpConnectionManager(Any any) {
        try {
            if (!any.is(HttpConnectionManager.class)) {
                return null;
            }
            return any.unpack(HttpConnectionManager.class);
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    public static final String LDS_REQUEST_AUTH_PRINCIPAL = "request.auth.principal";

    public static final String LDS_REQUEST_AUTH_AUDIENCE = "request.auth.audiences";

    public static final String LDS_REQUEST_AUTH_PRESENTER = "request.auth.presenter";

    public static final String LDS_REQUEST_AUTH_CLAIMS = "request.auth.claims";

    private RuleNode resolvePrincipal(Principal principal) {

        switch (principal.getIdentifierCase()) {
            case AND_IDS:
                CompositeRuleNode andNode = new CompositeRuleNode("and_ids", Relation.AND);
                for (Principal subPrincipal : principal.getAndIds()
                        .getIdsList()) {
                    andNode.addChild(resolvePrincipal(subPrincipal));
                }
                return andNode;

            case OR_IDS:
                CompositeRuleNode orNode = new CompositeRuleNode("or_ids", Relation.OR);
                for (Principal subPrincipal : principal.getOrIds()
                        .getIdsList()) {
                    orNode.addChild(resolvePrincipal(subPrincipal));
                }
                return orNode;

            case NOT_ID:
                CompositeRuleNode notNode = new CompositeRuleNode("not_id", Relation.NOT);
                notNode.addChild(resolvePrincipal(principal.getNotId()));
                return notNode;

            default:
                return handleLeafPrincipal(principal);
        }
    }
    private LeafRuleNode handleLeafPrincipal(Principal orIdentity) {
        IdentifierCase principalCase = orIdentity.getIdentifierCase();

        LeafRuleNode valueNode = null;

        switch (principalCase) {
            case AUTHENTICATED:
                StringMatcher matcher = Matchers.stringMatcher(orIdentity.getAuthenticated().getPrincipalName(), AUTHENTICATED);
                if (matcher != null) {
                    valueNode = new LeafRuleNode(Collections.singletonList(matcher), AUTHENTICATED.name());
                }
                break;

            case HEADER:
                String headerName = orIdentity.getHeader().getName();
                KeyMatcher keyMatcher = Matchers.keyMatcher(headerName, Matchers.stringMatcher(orIdentity.getHeader(), HEADER));
                valueNode = new LeafRuleNode(Collections.singletonList(keyMatcher), HEADER.name());
                break;

            case REMOTE_IP:
                IpMatcher ipMatcher = Matchers.ipMatcher(orIdentity.getRemoteIp(), REMOTE_IP);
                valueNode = new LeafRuleNode(Collections.singletonList(ipMatcher), REMOTE_IP.name());
                break;

            case DIRECT_REMOTE_IP:
                IpMatcher directIpMatcher = Matchers.ipMatcher(orIdentity.getDirectRemoteIp(), DIRECT_REMOTE_IP);
                valueNode = new LeafRuleNode(Collections.singletonList(directIpMatcher), DIRECT_REMOTE_IP.name());
                break;

            case METADATA:
                List<PathSegment> segments = orIdentity.getMetadata().getPathList();
                String key = segments.get(0).getKey();

                switch (key) {
                    case LDS_REQUEST_AUTH_PRINCIPAL:
                        StringMatcher jwtPrincipalMatcher = Matchers.stringMatcher(
                                orIdentity.getMetadata().getValue().getStringMatch(), RequestAuthProperty.JWT_PRINCIPALS);
                        if (jwtPrincipalMatcher != null) {
                            valueNode = new LeafRuleNode(Collections.singletonList(jwtPrincipalMatcher), LDS_REQUEST_AUTH_PRINCIPAL);
                        }
                        break;
                    case LDS_REQUEST_AUTH_AUDIENCE:
                        StringMatcher jwtAudienceMatcher = Matchers.stringMatcher(
                                orIdentity.getMetadata().getValue().getStringMatch(), RequestAuthProperty.JWT_AUDIENCES);
                        if (jwtAudienceMatcher != null) {
                            valueNode = new LeafRuleNode(Collections.singletonList(jwtAudienceMatcher), LDS_REQUEST_AUTH_AUDIENCE);
                        }
                        break;
                    case LDS_REQUEST_AUTH_PRESENTER:
                        StringMatcher jwtPresenterMatcher = Matchers.stringMatcher(
                                orIdentity.getMetadata().getValue().getStringMatch(), RequestAuthProperty.JWT_PRESENTERS);
                        if (jwtPresenterMatcher != null) {
                            valueNode = new LeafRuleNode(Collections.singletonList(jwtPresenterMatcher), LDS_REQUEST_AUTH_PRESENTER);
                        }
                        break;
                    case LDS_REQUEST_AUTH_CLAIMS:
                        if (segments.size() >= 2) {
                            String claimKey = segments.get(1).getKey();
                            KeyMatcher jwtClaimsMatcher = Matchers.keyMatcher(claimKey, Matchers.stringMatcher(
                                    orIdentity.getMetadata().getValue().getListMatch().getOneOf().getStringMatch(), RequestAuthProperty.JWT_CLAIMS));
                            valueNode = new LeafRuleNode(Collections.singletonList(jwtClaimsMatcher), LDS_REQUEST_AUTH_CLAIMS);
                        }
                        break;
                    default:
                        logger.warn("[XdsDataSource] Unsupported metadata type=" + key);
                        break;
                }
                break;

            default:
                logger.warn("[XdsDataSource] Unsupported principalCase =" + principalCase);
                break;
        }
        return valueNode;
    }


    private RuleNode resolvePermission(Permission permission){

        switch (permission.getRuleCase()) {
            case AND_RULES:
                CompositeRuleNode andNode = new CompositeRuleNode("and_rules", Relation.AND);
                for (Permission subPermission : permission.getAndRules().getRulesList()) {
                    andNode.addChild(resolvePermission(subPermission));
                }
                return andNode;

            case OR_RULES:
                CompositeRuleNode orNode = new CompositeRuleNode("or_rules", Relation.OR);
                for (Permission subPermission : permission.getOrRules().getRulesList()) {
                    orNode.addChild(resolvePermission(subPermission));
                }
                return orNode;

            case NOT_RULE:
                CompositeRuleNode notNode = new CompositeRuleNode("not_rules", Relation.NOT);
                notNode.addChild(resolvePermission(permission.getNotRule()));
                return notNode;

            default:
                return handleLeafPermission(permission);
        }
    }

    protected static final String LDS_HEADER_NAME_AUTHORITY = ":authority";

    protected static final String LDS_HEADER_NAME_METHOD = ":method";

    private RuleNode handleLeafPermission(Permission permission){
        Permission.RuleCase ruleCase = permission.getRuleCase();

        LeafRuleNode leafRuleNode = null;

        switch (ruleCase) {
            case DESTINATION_PORT: {
                int port = permission.getDestinationPort();
                if (port != 0) {
                    StringMatcher matcher = Matchers.stringMatcher(String.valueOf(permission.getDestinationPort()), RequestAuthProperty.DESTINATION_PORT);
                    leafRuleNode = new LeafRuleNode(Collections.singletonList(matcher), RequestAuthProperty.DESTINATION_PORT.name());
                }
                break;
            }
            case REQUESTED_SERVER_NAME: {
                StringMatcher matcher = Matchers.stringMatcher(permission.getRequestedServerName(), RequestAuthProperty.REQUESTED_SERVER_NAME);
                leafRuleNode = new LeafRuleNode(Collections.singletonList(matcher), RequestAuthProperty.DESTINATION_PORT.name());
                break;
            }
            case DESTINATION_IP:{
                IpMatcher matcher = Matchers.ipMatcher(permission.getDestinationIp(),RequestAuthProperty.DESTINATION_IP);
                leafRuleNode = new LeafRuleNode(Collections.singletonList(matcher), RequestAuthProperty.DESTINATION_IP.name());
                break;
            }
            case URL_PATH: {
                StringMatcher matcher = Matchers.stringMatcher(permission.getUrlPath().getPath(),RequestAuthProperty.URL_PATH);
                leafRuleNode = new LeafRuleNode(Collections.singletonList(matcher),RequestAuthProperty.URL_PATH.name());
                break;
            }
            case HEADER: {
                String headerName = permission.getHeader()
                        .getName();

                KeyMatcher matcher = null;

                if (LDS_HEADER_NAME_AUTHORITY.equals(headerName)) {
                    matcher = Matchers.keyMatcher(headerName ,Matchers.stringMatcher(permission.getHeader(),RequestAuthProperty.HOSTS));
                    leafRuleNode = new LeafRuleNode(Collections.singletonList(matcher), RequestAuthProperty.HOSTS.name());
                } else if (LDS_HEADER_NAME_METHOD.equals(headerName)) {
                    matcher = Matchers.keyMatcher(headerName ,Matchers.stringMatcher(permission.getHeader(),RequestAuthProperty.METHODS));
                    leafRuleNode = new LeafRuleNode(Collections.singletonList(matcher), RequestAuthProperty.METHODS.name());
                }

                if (matcher == null) {
                    logger.warn("","","","[XdsDataSource] Unsupported headerName="+headerName);
                }

                break;
            }
            default:
                logger.warn("","","","[XdsDataSource] Unsupported ruleCase="+ ruleCase);
                break;
        }
        return leafRuleNode;
    }

    @Override
    public void onResourceUpdate(List<Listener> listeners) {

        if (CollectionUtils.isEmpty(listeners)) {
            return;
        }

        List<HttpFilter> httpFilters = resolveHttpFilter(listeners);
        Map<String, RuleRoot> ruleRoots = new HashMap<>();

        //读 rbac filter
        resolveRbac(httpFilters, ruleRoots);

        //这里读的是 envoy.filters.http.jwt_authn 验证filter
//        Map<String, JwtRule> jwtRules = resolveJWT(httpFilters);
//
//        logger.info("[XdsDataSource] Auth rules resolve finish, RBAC rules size: {}, Jwt rules size: {}",
//            ruleRoots.size() + denyAuthRules.size(), jwtRules.size());

        //转为RuleSourceProvider
//        Rules rules = new Rules(allowAuthRules, denyAuthRules, jwtRules);
//        authRepository.update(rules);
//        return true;
    }

    protected static final String LDS_VIRTUAL_INBOUND = "virtualInbound";

    protected static final String LDS_CONNECTION_MANAGER = "envoy.filters.network.http_connection_manager";

    public static List<HttpFilter> resolveHttpFilter(List<Listener> listeners) {
        List<HttpFilter> httpFilters = new ArrayList<>();
        for (Listener listener : listeners) {
            if (!listener.getName().equals(LDS_VIRTUAL_INBOUND)) {
                continue;
            }
            for (FilterChain filterChain : listener.getFilterChainsList()) {
                for (Filter filter : filterChain.getFiltersList()) {
                    if (!filter.getName().equals(LDS_CONNECTION_MANAGER)) {
                        continue;
                    }
                    HttpConnectionManager httpConnectionManager = unpackHttpConnectionManager(filter.getTypedConfig());
                    if (httpConnectionManager == null) {
                        continue;
                    }
                    for (HttpFilter httpFilter : httpConnectionManager.getHttpFiltersList()) {
                        if (httpFilter != null) {
                            httpFilters.add(httpFilter);
                        }
                    }
                }
            }
        }
        return httpFilters;
    }

//    /**
//     * Parsing JWT Rule
//     *
//     * @param httpFilters
//     * @return
//     */
//    public static Map<String, JwtRule> resolveJWT(List<HttpFilter> httpFilters) {
//        Map<String, JwtRule> jwtRules = new HashMap<>();
//        /**There are multiple duplicates, and we only choose one of them */
//        JwtAuthentication jwtAuthentication = null;
//        for (HttpFilter httpFilter : httpFilters) {
//            if (!httpFilter.getName().equals(LDS_JWT_FILTER)) {
//                continue;
//            }
//            try {
//                jwtAuthentication = httpFilter.getTypedConfig().unpack(JwtAuthentication.class);
//                if (null != jwtAuthentication) {
//                    break;
//                }
//            } catch (InvalidProtocolBufferException e) {
//                logger.warn("","","","[XdsDataSource] Parsing JwtRule error", e);
//            }
//        }
//        if (null == jwtAuthentication) {
//            return jwtRules;
//        }
//
//        Map<String, JwtProvider> jwtProviders = jwtAuthentication.getProvidersMap();
//        for (Entry<String, JwtProvider> entry : jwtProviders.entrySet()) {
//            JwtProvider provider = entry.getValue();
//            Map<String, String> fromHeaders = new HashMap<>();
//            for (JwtHeader header : provider.getFromHeadersList()) {
//                fromHeaders.put(header.getName(), header.getValuePrefix());
//            }
//            jwtRules.put(entry.getKey(),
//                    new JwtRule(entry.getKey(), fromHeaders, provider.getIssuer(),
//                            new ArrayList<>(provider.getAudiencesList()),
//                            provider.getLocalJwks().getInlineString(),
//                            new ArrayList<>(provider.getFromParamsList())));
//        }
//
//        return jwtRules;
//    }

    @Override
    public Map<String, Object> readAsMap() {
        return null;
    }
}

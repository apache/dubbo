package org.apache.dubbo.security.cert.rule.authorization;

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.security.cert.Endpoint;

import java.net.UnknownHostException;
import java.util.List;

public class AuthorizationTarget {
    private List<String> ipBlocks;
    private List<String> notIpBlocks;
    private List<String> principals;
    private List<String> notPrincipals;

    private List<AuthorizationExtend> sourceExtends;
    private List<AuthorizationExtend> sourceNotExtends;

    public List<String> getIpBlocks() {
        return ipBlocks;
    }

    public void setIpBlocks(List<String> ipBlocks) {
        this.ipBlocks = ipBlocks;
    }

    public List<String> getNotIpBlocks() {
        return notIpBlocks;
    }

    public void setNotIpBlocks(List<String> notIpBlocks) {
        this.notIpBlocks = notIpBlocks;
    }

    public List<String> getPrincipals() {
        return principals;
    }

    public void setPrincipals(List<String> principals) {
        this.principals = principals;
    }

    public List<String> getNotPrincipals() {
        return notPrincipals;
    }

    public void setNotPrincipals(List<String> notPrincipals) {
        this.notPrincipals = notPrincipals;
    }

    public List<AuthorizationExtend> getSourceExtends() {
        return sourceExtends;
    }

    public void setSourceExtends(List<AuthorizationExtend> sourceExtends) {
        this.sourceExtends = sourceExtends;
    }

    public List<AuthorizationExtend> getSourceNotExtends() {
        return sourceNotExtends;
    }

    public void setSourceNotExtends(List<AuthorizationExtend> sourceNotExtends) {
        this.sourceNotExtends = sourceNotExtends;
    }

    public boolean match(Endpoint endpoint) {
        String ip = endpoint.getIp();
        if (ipBlocks != null && !ipBlocks.isEmpty() &&
            (ip == null || ipBlocks.stream().noneMatch(ipBlock -> {
                try {
                    return NetUtils.matchIpExpression(ipBlock, ip);
                } catch (UnknownHostException e) {
                    return false;
                }
            }))) {
            return false;
        }

        if (notIpBlocks != null && !notIpBlocks.isEmpty() &&
            ip != null && notIpBlocks.stream().anyMatch(ipBlock -> {
            try {
                return NetUtils.matchIpExpression(ipBlock, ip);
            } catch (UnknownHostException e) {
                return false;
            }
        })) {
            return false;
        }

        String principal = endpoint.getPrincipal();
        if (principals != null && !principals.isEmpty() &&
            (principal == null || !principals.contains(principal))) {
            return false;
        }

        if (notPrincipals != null && !notPrincipals.isEmpty() &&
            principal != null && notPrincipals.contains(principal)) {
            return false;
        }

        if (sourceExtends != null && !sourceExtends.isEmpty()) {
            for (AuthorizationExtend extend : sourceExtends) {
                if (!extend.match(endpoint)) {
                    return false;
                }
            }
        }

        if (sourceNotExtends != null && !sourceNotExtends.isEmpty()) {
            for (AuthorizationExtend extend : sourceNotExtends) {
                if (extend.match(endpoint)) {
                    return false;
                }
            }
        }

        return true;
    }
}

package org.apache.dubbo.security.cert.rule;

import org.apache.dubbo.common.URL;

import java.util.List;

public class AuthorizationSource {
    private List<String> namespaces;
    private List<String> notNamespaces;
    private List<String> ipBlocks;
    private List<String> notIpBlocks;
    private List<String> principals;
    private List<String> notPrincipals;

    private List<AuthorizationExtend> sourceExtends;
    private List<AuthorizationExtend> sourceNotExtends;

    public List<String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(List<String> namespaces) {
        this.namespaces = namespaces;
    }

    public List<String> getNotNamespaces() {
        return notNamespaces;
    }

    public void setNotNamespaces(List<String> notNamespaces) {
        this.notNamespaces = notNamespaces;
    }

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

    public boolean match(URL url) {
        if (namespaces != null && !namespaces.isEmpty()) {
            String namespace = url.getParameter("kubernetesEnv.namespace");
            if (namespace == null || !namespaces.contains(namespace)) {
                return false;
            }
        }

        if (notNamespaces != null && !notNamespaces.isEmpty()) {
            String namespace = url.getParameter("kubernetesEnv.namespace");
            if (namespace != null && notNamespaces.contains(namespace)) {
                return false;
            }
        }

        if (ipBlocks != null && !ipBlocks.isEmpty()) {
            String ip = url.getHost();
            if (ip == null || !ipBlocks.contains(ip)) {
                return false;
            }
        }
        if (notIpBlocks != null && !notIpBlocks.isEmpty()) {
            String ip = url.getHost();
            if (ip != null && notIpBlocks.contains(ip)) {
                return false;
            }
        }

        // TODO principals

        if (sourceExtends != null && !sourceExtends.isEmpty()) {
            for (AuthorizationExtend extend : sourceExtends) {
                if (!extend.match(url)) {
                    return false;
                }
            }
        }

        if (sourceNotExtends != null && !sourceNotExtends.isEmpty()) {
            for (AuthorizationExtend extend : sourceNotExtends) {
                if (extend.match(url)) {
                    return false;
                }
            }
        }

        return true;
    }
}

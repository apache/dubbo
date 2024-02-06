package org.apache.dubbo.rpc.cluster.xds.resource;

public class XdsRouteMatch {
    private String prefix;

    private String path;

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    private String regex;

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    private boolean caseSensitive;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isMatch(String input) {
        if (getPath() != null && !getPath().equals("")) {
            return isCaseSensitive() ? getPath().equals(input) : getPath().equalsIgnoreCase(input);
        } else if (getPrefix() != null) {
            return isCaseSensitive()
                ? input.startsWith(getPrefix())
                : input.toLowerCase().startsWith(getPrefix());
        }
        return input.matches(getRegex());
    }
}

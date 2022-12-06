package org.apache.dubbo.rpc.protocol.mvc.annotation.provider.dispatcher;

import org.apache.dubbo.rpc.protocol.mvc.constans.RestConstant;

import java.util.Arrays;
import java.util.Objects;

public class PathMather {
    private static final String SEPARATOR = RestConstant.PATH_SEPARATOR;
    private String path;
    private String version;
    private String group;
    private String port;
    private String[] pathSplits;
    private boolean hasPathVariable;


    public PathMather(String path) {
        this(path, null, null, null);
    }

    public PathMather(String path, String version, String group, String port) {
        this.path = path;
        this.pathSplits = path.split(SEPARATOR);

        for (String pathSplit : pathSplits) {

            if (isPlaceHold(pathSplit)) {
                hasPathVariable = true;
                break;
            }
        }
        this.version = version;
        this.group = group;
        this.port = port;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setPort(int port) {
        this.port = String.valueOf(port);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathMather that = (PathMather) o;
        return pathEqual(that.path) && Objects.equals(version, that.version)
            && Objects.equals(group, that.group) && Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, group, port);
    }

    private boolean pathEqual(String path) {

        // no place hold
        if (!hasPathVariable) {
            return this.path.equals(path);
        }

        String[] split = path.split(SEPARATOR);


        if (split.length != pathSplits.length) {
            return false;
        }

        for (int i = 0; i < pathSplits.length; i++) {
            boolean equals = split[i].equals(pathSplits[i]);
            if (equals) {
                continue;
            } else {
                if (placeHoldCompare(pathSplits[i])) {
                    continue;
                } else {
                    return false;
                }
            }
        }

        return true;

    }

    private boolean placeHoldCompare(String pathSplit) {
        boolean startAndEndEqual = isPlaceHold(pathSplit);

        // start {  end }
        if (!startAndEndEqual) {
            return false;
        }

        // exclude  {}
        boolean lengthCondition = pathSplit.length() >= 3;

        if (!lengthCondition) {
            return false;
        }

        return true;
    }

    private boolean isPlaceHold(String pathSplit) {
        return pathSplit.startsWith("{") && pathSplit.endsWith("}");
    }


    @Override
    public String toString() {
        return "PathMather{" +
            "path='" + path + '\'' +
            ", version='" + version + '\'' +
            ", group='" + group + '\'' +
            ", port='" + port + '\'' +
            '}';
    }
}

package org.apache.dubbo.rpc.protocol.mvc.util;

import org.apache.dubbo.rpc.protocol.mvc.annotation.ArgInfo;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParamType;
import org.apache.dubbo.rpc.protocol.mvc.constans.RestConstant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * is used to parse url pathVariable
 * <p>
 * String[] splits= url.split("/")
 * List<String> strings = Arrays.asList(split);
 * strings.set(UrlSplitIndex, (String) args.get(argIndex));
 */
public class PathUtil {
    private static final String SEPARATOR = RestConstant.PATH_SEPARATOR;

    /**
     * generate real path from  rawPath according to argInfo and method args
     *
     * @param rawPath
     * @param argInfos
     * @param args
     * @return
     */
    public static String resolvePathVariable(String rawPath, List<ArgInfo> argInfos, List<Object> args) {

        String[] split = rawPath.split(SEPARATOR);

        List<String> strings = Arrays.asList(split);

        List<ArgInfo> pathArgInfos = new ArrayList<>();

        for (ArgInfo argInfo : argInfos) {
            if (ParamType.PATH.supportAnno(argInfo.getParamAnno())) {
                pathArgInfos.add(argInfo);
            }
        }


        for (ArgInfo pathArgInfo : pathArgInfos) {
            strings.set(pathArgInfo.getUrlSplitIndex(), (String) args.get(pathArgInfo.getIndex()));
        }


        String pat = SEPARATOR;

        for (String string : strings) {

            if (string.length() == 0) {
                continue;
            }

            pat = pat + string + SEPARATOR;
        }

        if (pat.endsWith(SEPARATOR)) {
            pat = pat.substring(0, pat.lastIndexOf(SEPARATOR));
        }

        return pat;

    }


    /**
     * parse pathVariable index from url by annotation info
     *
     * @param rawPath
     * @param argInfos
     */
    public void setArgInfoSplitIndex(String rawPath, List<ArgInfo> argInfos) {
        String[] split = rawPath.split(SEPARATOR);

        List<PathPair> pathPairs = new ArrayList<>();

        for (ArgInfo argInfo : argInfos) {
            if (ParamType.PATH.supportAnno(argInfo.getParamAnno())) {
                pathPairs.add(new PathPair(argInfo));
            }
        }

        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            for (PathPair pathPair : pathPairs) {
                boolean match = pathPair.match(s);
                if (match) {
                    pathPair.setArgInfoSplitIndex(i);
                }
            }
        }

    }

    public static class PathPair {

        String value;

        ArgInfo argInfo;


        public PathPair(ArgInfo argInfo) {
            this.argInfo = argInfo;
            this.value = argInfo.getAnnoNameAttribute();
        }

        public String getPatten() {
            return "{" + value + "}";
        }

        public boolean match(String value) {
            return getPatten().equals(value);
        }


        public String getValue() {
            return value;
        }

        public void setArgInfo(ArgInfo argInfo) {
            this.argInfo = argInfo;
        }

        public void setArgInfoSplitIndex(int urlSplitIndex) {
            this.argInfo.setUrlSplitIndex(urlSplitIndex);
        }
    }
}



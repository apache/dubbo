package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.utils.CollectionUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class ExceptionUtils {

    private static final int NOT_FOUND = -1;

    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    public static String getStackFrameString(List<String> stackFrameList) {
        if (CollectionUtils.isEmpty(stackFrameList)) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : stackFrameList) {
            stringBuilder.append(s).append("\n");
        }
        return stringBuilder.toString();
    }

    public static String[] getStackFrames(final Throwable throwable) {
        if (throwable == null) {
            return new String[0];
        }
        return getStackFrames(getStackTrace(throwable));
    }

    static String[] getStackFrames(final String stackTrace) {
        final String linebreak = System.lineSeparator();
        final StringTokenizer frames = new StringTokenizer(stackTrace, linebreak);
        final List<String> list = new ArrayList<>();
        while (frames.hasMoreTokens()) {
            list.add(frames.nextToken());
        }
        return list.toArray(new String[0]);
    }

    public static List<String> getStackFrameList(final Throwable t) {
        final String stackTrace = getStackTrace(t);
        final String linebreak = System.lineSeparator();
        final StringTokenizer frames = new StringTokenizer(stackTrace, linebreak);
        final List<String> list = new ArrayList<>();
        while (frames.hasMoreTokens()) {
            list.add(frames.nextToken());
        }
        return list;
    }
}

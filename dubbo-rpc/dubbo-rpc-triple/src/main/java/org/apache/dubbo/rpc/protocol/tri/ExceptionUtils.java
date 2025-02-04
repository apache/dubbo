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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.logger.Level;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class ExceptionUtils {

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

    public static List<String> getStackFrameList(final Throwable t, int maxDepth) {
        final String stackTrace = getStackTrace(t);
        final String linebreak = System.lineSeparator();
        final StringTokenizer frames = new StringTokenizer(stackTrace, linebreak);
        final List<String> list = new ArrayList<>();
        for (int i = 0; i < maxDepth && frames.hasMoreTokens(); i++) {
            list.add(frames.nextToken());
        }
        return list;
    }

    public static List<String> getStackFrameList(final Throwable t) {
        return getStackFrameList(t, Integer.MAX_VALUE);
    }

    public static Level resolveLogLevel(final Throwable t) {
        if (t instanceof HttpStatusException) {
            int httpStatusCode = ((HttpStatusException) t).getStatusCode();
            if (httpStatusCode < HttpStatus.BAD_REQUEST.getCode()) {
                return TripleProtocol.VERBOSE_ENABLED ? Level.INFO : Level.DEBUG;
            }
            if (httpStatusCode < HttpStatus.INTERNAL_SERVER_ERROR.getCode()) {
                return Level.INFO;
            }
        }
        return Level.ERROR;
    }

    /**
     * Wrap as runtime exception
     */
    public static RuntimeException wrap(Throwable t) {
        while (true) {
            if (t instanceof UndeclaredThrowableException) {
                t = ((UndeclaredThrowableException) t).getUndeclaredThrowable();
            } else if (t instanceof InvocationTargetException) {
                t = ((InvocationTargetException) t).getTargetException();
            } else if (t instanceof CompletionException || t instanceof ExecutionException) {
                Throwable cause = t.getCause();
                if (cause == t) {
                    break;
                }
                t = cause;
            } else {
                break;
            }
        }
        return t instanceof RuntimeException ? (RuntimeException) t : new ThrowableWrapper(t);
    }

    /**
     * Unwrap the wrapped exception
     */
    public static Throwable unwrap(Throwable t) {
        while (true) {
            if (t instanceof ThrowableWrapper) {
                t = ((ThrowableWrapper) t).getOriginal();
            } else if (t instanceof UndeclaredThrowableException) {
                t = ((UndeclaredThrowableException) t).getUndeclaredThrowable();
            } else if (t instanceof InvocationTargetException) {
                t = ((InvocationTargetException) t).getTargetException();
            } else if (t instanceof CompletionException || t instanceof ExecutionException) {
                Throwable cause = t.getCause();
                if (cause == t) {
                    break;
                }
                t = cause;
            } else {
                break;
            }
        }
        return t;
    }

    public static String buildVerboseMessage(Throwable t) {
        StringBuilder sb = new StringBuilder(256);

        t = unwrap(t);
        Throwable parent;
        String before = null;
        do {
            String msg = t.getMessage();
            String className = t.getClass().getName();
            if (before != null) {
                if (!before.startsWith(className) && !before.equals(msg)) {
                    sb.append(": ").append(before);
                }
                sb.append(" -> ");
            }
            sb.append(className);
            before = msg;

            parent = t;
            t = t.getCause();
        } while (t != null && t != parent);
        if (before != null) {
            sb.append(": ").append(before);
        }

        sb.append("\n -> Stack traces:");
        StackTraceElement[] elements = parent.getStackTrace();
        for (int i = 0, len = elements.length; i < 10 && i < len; i++) {
            sb.append("\n    at ").append(elements[i]);
        }

        return sb.toString();
    }
}

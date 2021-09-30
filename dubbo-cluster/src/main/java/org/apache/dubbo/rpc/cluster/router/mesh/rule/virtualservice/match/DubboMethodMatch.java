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

package org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match;

import java.util.List;
import java.util.Map;


public class DubboMethodMatch {
    private StringMatch name_match;
    private Integer argc;
    private List<DubboMethodArg> args;
    private List<StringMatch> argp;
    private Map<String, StringMatch> headers;

    public StringMatch getName_match() {
        return name_match;
    }

    public void setName_match(StringMatch name_match) {
        this.name_match = name_match;
    }

    public Integer getArgc() {
        return argc;
    }

    public void setArgc(Integer argc) {
        this.argc = argc;
    }

    public List<DubboMethodArg> getArgs() {
        return args;
    }

    public void setArgs(List<DubboMethodArg> args) {
        this.args = args;
    }

    public List<StringMatch> getArgp() {
        return argp;
    }

    public void setArgp(List<StringMatch> argp) {
        this.argp = argp;
    }

    public Map<String, StringMatch> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, StringMatch> headers) {
        this.headers = headers;
    }

    public static boolean isMatch(DubboMethodMatch dubboMethodMatch, String methodName, String[] parameterTypeList, Object[] parameters) {
        StringMatch nameMatch = dubboMethodMatch.getName_match();
        if (nameMatch != null && !StringMatch.isMatch(nameMatch, methodName)) {
            return false;
        }

        Integer argc = dubboMethodMatch.getArgc();
        if (argc != null &&
                ((argc != 0 && (parameters == null || parameters.length == 0)) || (argc != parameters.length))) {
            return false;
        }
        List<StringMatch> argp = dubboMethodMatch.getArgp();
        if (argp != null) {
            if (((parameterTypeList == null || parameterTypeList.length == 0) && argp.size() > 0)
                    || (argp.size() != parameterTypeList.length)) {
                return false;
            }

            for (int index = 0; index < argp.size(); index++) {
                if (!StringMatch.isMatch(argp.get(index), parameterTypeList[index])) {
                    return false;
                }
            }
        }

        List<DubboMethodArg> args = dubboMethodMatch.getArgs();

        if (args != null && args.size() > 0) {
            if (parameters == null || parameters.length == 0) {
                return false;
            }

            for (DubboMethodArg dubboMethodArg : args) {
                int index = dubboMethodArg.getIndex();
                if (index >= parameters.length) {
                    throw new IndexOutOfBoundsException("DubboMethodArg index >= parameters.length");
                }
                if (!DubboMethodArg.isMatch(dubboMethodArg, parameters[index])) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "DubboMethodMatch{" +
                "name_match=" + name_match +
                ", argc=" + argc +
                ", args=" + args +
                ", argp=" + argp +
                ", headers=" + headers +
                '}';
    }
}


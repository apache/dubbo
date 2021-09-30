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


public class ListStringMatch {
    private List<StringMatch> oneof;

    public List<StringMatch> getOneof() {
        return oneof;
    }

    public void setOneof(List<StringMatch> oneof) {
        this.oneof = oneof;
    }


    public static boolean isMatch(ListStringMatch listStringMatch, String input) {

        for (StringMatch stringMatch : listStringMatch.getOneof()) {
            if (StringMatch.isMatch(stringMatch, input)) {
                return true;
            }
        }
        return false;
    }
}

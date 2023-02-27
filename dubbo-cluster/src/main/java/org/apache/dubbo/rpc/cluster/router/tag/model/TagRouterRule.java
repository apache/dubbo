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
package org.apache.dubbo.rpc.cluster.router.tag.model;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.router.AbstractRouterRule;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.router.tag.TagStateRouter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.dubbo.rpc.cluster.Constants.RULE_VERSION_V30;
import static org.apache.dubbo.rpc.cluster.Constants.TAGS_KEY;

/**
 * %YAML1.2
 * ---
 * force: true
 * runtime: false
 * enabled: true
 * priority: 1
 * key: demo-provider
 * tags:
 * - name: tag1
 * addresses: [ip1, ip2]
 * - name: tag2
 * addresses: [ip3, ip4]
 * ...
 */
public class TagRouterRule extends AbstractRouterRule {
    private List<Tag> tags;

    private final Map<String, Set<String>> addressToTagnames = new HashMap<>();
    private final Map<String, Set<String>> tagnameToAddresses = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static TagRouterRule parseFromMap(Map<String, Object> map) {
        TagRouterRule tagRouterRule = new TagRouterRule();
        tagRouterRule.parseFromMap0(map);

        Object tags = map.get(TAGS_KEY);
        if (tags != null && List.class.isAssignableFrom(tags.getClass())) {
            tagRouterRule.setTags(((List<Map<String, Object>>) tags).stream()
                .map(objMap -> Tag.parseFromMap(objMap, tagRouterRule.getVersion())).collect(Collectors.toList()));
        }

        return tagRouterRule;
    }

    public void init(TagStateRouter<?> router) {
        if (!isValid()) {
            return;
        }

        BitList<? extends Invoker<?>> invokers = router.getInvokers();

        // for tags with 'addresses` field set and 'match' field not set
        tags.stream().filter(tag -> CollectionUtils.isNotEmpty(tag.getAddresses())).forEach(tag -> {
            tagnameToAddresses.put(tag.getName(), new HashSet<>(tag.getAddresses()));
            tag.getAddresses().forEach(addr -> {
                Set<String> tagNames = addressToTagnames.computeIfAbsent(addr, k -> new HashSet<>());
                tagNames.add(tag.getName());
            });
        });

        if (this.getVersion() != null && this.getVersion().startsWith(RULE_VERSION_V30)) {
            // for tags with 'match` field set and 'addresses' field not set
            if (CollectionUtils.isNotEmpty(invokers)) {
                tags.stream().filter(tag -> CollectionUtils.isEmpty(tag.getAddresses())).forEach(tag -> {
                    Set<String> addresses = new HashSet<>();
                    List<ParamMatch> paramMatchers = tag.getMatch();
                    invokers.forEach(invoker -> {
                        boolean isMatch = true;
                        for (ParamMatch matcher : paramMatchers) {
                            if (!matcher.isMatch(invoker.getUrl().getOriginalParameter(matcher.getKey()))) {
                                isMatch = false;
                                break;
                            }
                        }
                        if (isMatch) {
                            addresses.add(invoker.getUrl().getAddress());
                        }
                    });
                    if (CollectionUtils.isNotEmpty(addresses)) {// null means tag not set
                        tagnameToAddresses.put(tag.getName(), addresses);
                    }
                });
            }
        }
    }

    public Set<String> getAddresses() {
        return tagnameToAddresses.entrySet().stream()
            .filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
            .flatMap(entry -> entry.getValue().stream())
            .collect(Collectors.toSet());
    }

    public List<String> getTagNames() {
        return tags.stream().map(Tag::getName).collect(Collectors.toList());
    }

    public Map<String, Set<String>> getAddressToTagnames() {
        return addressToTagnames;
    }


    public Map<String, Set<String>> getTagnameToAddresses() {
        return tagnameToAddresses;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }
}

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
package org.apache.dubbo.rpc.protocol.tri.rest.service;

import org.apache.dubbo.remoting.http12.rest.Param;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {

    private Long id;
    private String name;
    private Group group;
    private long[] ids;
    private List<Integer> scores;
    private List<Tag> tags;
    private Tag[] tagsA;
    private List<Tag> tagsB = new ArrayList<>();
    private Tag[] tagsC = new Tag[] {new Tag("a", "b")};
    private List<Map<String, Group>> groupMaps;
    private Map<String, String> features;
    private Map<String, Tag> tagMap;
    private Map<String, Tag> tagMapA = new HashMap<>();
    private Map<Integer, Tag> tagMapB;
    private Map<String, List<Group>> groupsMap;

    public User() {
        tagsB.add(new Tag("a", "b"));
        tagMapA.put("a", new Tag("a", "b"));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public long[] getIds() {
        return ids;
    }

    public void setIds(long[] ids) {
        this.ids = ids;
    }

    public List<Integer> getScores() {
        return scores;
    }

    public void setScores(List<Integer> scores) {
        this.scores = scores;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public Tag[] getTagsA() {
        return tagsA;
    }

    public void setTagsA(Tag[] tagsA) {
        this.tagsA = tagsA;
    }

    public List<Tag> getTagsB() {
        return tagsB;
    }

    public void setTagsB(List<Tag> tagsB) {
        this.tagsB = tagsB;
    }

    public Tag[] getTagsC() {
        return tagsC;
    }

    public void setTagsC(Tag[] tagsC) {
        this.tagsC = tagsC;
    }

    public List<Map<String, Group>> getGroupMaps() {
        return groupMaps;
    }

    public void setGroupMaps(List<Map<String, Group>> groupMaps) {
        this.groupMaps = groupMaps;
    }

    public Map<String, String> getFeatures() {
        return features;
    }

    public void setFeatures(Map<String, String> features) {
        this.features = features;
    }

    public Map<String, Tag> getTagMap() {
        return tagMap;
    }

    public void setTagMap(Map<String, Tag> tagMap) {
        this.tagMap = tagMap;
    }

    public Map<String, Tag> getTagMapA() {
        return tagMapA;
    }

    public void setTagMapA(Map<String, Tag> tagMapA) {
        this.tagMapA = tagMapA;
    }

    public Map<Integer, Tag> getTagMapB() {
        return tagMapB;
    }

    public void setTagMapB(Map<Integer, Tag> tagMapB) {
        this.tagMapB = tagMapB;
    }

    public Map<String, List<Group>> getGroupsMap() {
        return groupsMap;
    }

    public void setGroupsMap(Map<String, List<Group>> groupsMap) {
        this.groupsMap = groupsMap;
    }

    public static class UserEx extends User {

        private String email;

        @Param(value = "p")
        private String phone;

        public UserEx(String email) {
            this.email = email;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }

    public static class Group {

        private int id;
        private String name;
        private User owner;
        private Group parent;
        private List<Group> children;
        private Map<String, String> features;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public User getOwner() {
            return owner;
        }

        public void setOwner(User owner) {
            this.owner = owner;
        }

        public Group getParent() {
            return parent;
        }

        public void setParent(Group parent) {
            this.parent = parent;
        }

        public List<Group> getChildren() {
            return children;
        }

        public void setChildren(List<Group> children) {
            this.children = children;
        }

        public Map<String, String> getFeatures() {
            return features;
        }

        public void setFeatures(Map<String, String> features) {
            this.features = features;
        }
    }

    public static class Tag {

        private String name;
        private String value;

        public Tag() {}

        public Tag(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}

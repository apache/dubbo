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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi.model;

import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Context;

import java.util.Map;

public final class Info extends Node<Info> {

    private String title;
    private String summary;
    private String description;
    private String termsOfService;
    private Contact contact;
    private License license;
    private String version;

    public String getTitle() {
        return title;
    }

    public Info setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getSummary() {
        return summary;
    }

    public Info setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Info setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getTermsOfService() {
        return termsOfService;
    }

    public Info setTermsOfService(String termsOfService) {
        this.termsOfService = termsOfService;
        return this;
    }

    public Contact getContact() {
        return contact;
    }

    public Info setContact(Contact contact) {
        this.contact = contact;
        return this;
    }

    public License getLicense() {
        return license;
    }

    public Info setLicense(License license) {
        this.license = license;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public Info setVersion(String version) {
        this.version = version;
        return this;
    }

    @Override
    public Info clone() {
        Info clone = super.clone();
        clone.contact = clone(contact);
        clone.license = clone(license);
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        write(node, "title", title);
        write(node, "summary", summary);
        write(node, "description", description);
        write(node, "termsOfService", termsOfService);
        write(node, "contact", contact, context);
        write(node, "license", license, context);
        write(node, "version", version);
        writeExtensions(node);
        return node;
    }
}

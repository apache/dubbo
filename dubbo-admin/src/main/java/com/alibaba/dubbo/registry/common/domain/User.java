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
package com.alibaba.dubbo.registry.common.domain;

import com.alibaba.dubbo.registry.common.route.ParseUtils;

import java.util.Arrays;
import java.util.List;

/**
 * User
 *
 */
public class User extends Entity {

    public static final String REALM = "dubbo";
    public static final String ROOT = "R";
    public static final String ADMINISTRATOR = "A";
    public static final String MANAGER = "M";
    public static final String GUEST = "G";
    public static final String ANONYMOUS = "anonymous";
    public static final String LEGACY = "legacy";
    private static final long serialVersionUID = 7330539198581235339L;
    private String username;

    private String password;

    private String role;

    private String creator;

    private boolean enabled;

    private String name;

    private String department;

    private String email;

    private String phone;

    private String alitalk;

    private String locale;

    private String servicePrivilege;

    private List<String> servicePrivileges;

    public User() {
    }

    public User(Long id) {
        super(id);
    }

    public static boolean isValidPrivilege(String servicePrivilege) {
        if (servicePrivilege == null || servicePrivilege.length() == 0) {
            return true;
        }
        String[] privileges = servicePrivilege.trim().split("\\s*,\\s*");
        for (String privilege : privileges) {
            if (privilege.endsWith("*")) {
                privilege = privilege.substring(0, privilege.length() - 1);
            }
            if (privilege.indexOf('*') > -1) {
                return false;
            }
        }
        return true;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean hasServicePrivilege(String[] services) {
        if (services == null || services.length == 0)
            throw new IllegalArgumentException("services == null");
        for (String service : services) {
            boolean r = hasServicePrivilege(service);
            if (!r)
                return false;
        }
        return true;
    }

    public boolean canGrantPrivilege(String servicePrivilege) {
        if (servicePrivilege == null || servicePrivilege.length() == 0) {
            return true;
        }
        if (servicePrivileges == null || servicePrivileges.size() == 0) {
            return false;
        }
        String[] privileges = servicePrivilege.trim().split("\\s*,\\s*");
        for (String privilege : privileges) {
            boolean hasPrivilege = false;
            for (String ownPrivilege : servicePrivileges) {
                if (matchPrivilege(ownPrivilege, privilege)) {
                    hasPrivilege = true;
                }
            }
            if (!hasPrivilege) {
                return false;
            }
        }
        return true;
    }

    private boolean matchPrivilege(String ownPrivilege, String privilege) {
        if ("*".equals(ownPrivilege) || ownPrivilege.equals(privilege)) {
            return true;
        }
        if (privilege.endsWith("*")) {
            if (!ownPrivilege.endsWith("*")) {
                return false;
            }
            privilege = privilege.substring(0, privilege.length() - 1);
            ownPrivilege = ownPrivilege.substring(0, ownPrivilege.length() - 1);
            return privilege.startsWith(ownPrivilege);
        } else {
            if (ownPrivilege.endsWith("*")) {
                ownPrivilege = ownPrivilege.substring(0, ownPrivilege.length() - 1);
            }
            return privilege.startsWith(ownPrivilege);
        }
    }

    public boolean hasServicePrivilege(String service) {
        if (service == null || service.length() == 0)
            return false;
        if (role == null || GUEST.equalsIgnoreCase(role)) {
            return false;
        }
        if (ROOT.equalsIgnoreCase(role)) {
            return true;
        }

        if (servicePrivileges != null && servicePrivileges.size() > 0) {
            for (String privilege : servicePrivileges) {
                boolean ok = ParseUtils.isMatchGlobPattern(privilege, service);
                if (ok) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getServicePrivilege() {
        return servicePrivilege;
    }

    public void setServicePrivilege(String servicePrivilege) {
        this.servicePrivilege = servicePrivilege;
        if (servicePrivilege != null && servicePrivilege.length() > 0) {
            servicePrivileges = Arrays.asList(servicePrivilege.trim().split("\\s*,\\s*"));
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAlitalk() {
        return alitalk;
    }

    public void setAlitalk(String alitalk) {
        this.alitalk = alitalk;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

}

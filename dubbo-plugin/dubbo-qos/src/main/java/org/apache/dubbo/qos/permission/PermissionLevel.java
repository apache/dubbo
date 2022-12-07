package org.apache.dubbo.qos.permission;

import org.apache.dubbo.common.utils.StringUtils;

import java.util.Arrays;

public enum PermissionLevel {
    /**
     * the lowest permission level, can access with
     * anonymousAccessPermissionLevel=PUBLIC / anonymousAccessPermissionLevel=1 or higher
     */
    PUBLIC(1),
    /**
     * the middle permission level, default permission for each cmd
     */
    PROTECTED(2),
    /**
     * the highest permission level, suppose only the localhost can access this command
     */
    PRIVATE(3),

    /**
     * It is the reserved default anonymous permission level, can not access any command
     */
    NONE(Integer.MIN_VALUE),

    ;
    private final int level;

    PermissionLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public static PermissionLevel from(String permissionLevel) {
        if (StringUtils.isNumber(permissionLevel)) {
            return Arrays.stream(values())
                .filter(p -> String.valueOf(p.getLevel()).equals(permissionLevel.trim()))
                .findFirst()
                .orElse(NONE);
        }
        return Arrays.stream(values())
            .filter(p -> p.name().equalsIgnoreCase(String.valueOf(permissionLevel).trim()))
            .findFirst()
            .orElse(NONE);
    }
}

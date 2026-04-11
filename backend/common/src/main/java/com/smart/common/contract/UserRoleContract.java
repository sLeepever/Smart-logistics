package com.smart.common.contract;

import java.util.Set;

public final class UserRoleContract {

    public static final String ADMIN = "admin";
    public static final String DISPATCHER = "dispatcher";
    public static final String DRIVER = "driver";
    public static final String CUSTOMER = "customer";

    public static final String VALIDATION_REGEX = "^(admin|dispatcher|driver|customer)$";

    public static final Set<String> OPERATIONAL_ROLES = Set.of(ADMIN, DISPATCHER);
    public static final Set<String> SUPPORTED_ROLES = Set.of(ADMIN, DISPATCHER, DRIVER, CUSTOMER);

    private UserRoleContract() {
    }

    public static boolean isSupported(String role) {
        return SUPPORTED_ROLES.contains(role);
    }

    public static boolean isOperational(String role) {
        return OPERATIONAL_ROLES.contains(role);
    }
}

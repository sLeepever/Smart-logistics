package com.smart.user.dto;

import jakarta.validation.constraints.Pattern;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateUserRequestContractTest {

    @Test
    void rolePatternIncludesCustomer() throws NoSuchFieldException {
        Field roleField = CreateUserRequest.class.getDeclaredField("role");
        Pattern pattern = roleField.getAnnotation(Pattern.class);

        assertNotNull(pattern);
        assertTrue("customer".matches(pattern.regexp()));
        assertTrue("admin".matches(pattern.regexp()));
        assertTrue("dispatcher".matches(pattern.regexp()));
        assertTrue("driver".matches(pattern.regexp()));
    }
}

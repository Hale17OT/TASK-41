package com.dispatchops.web.controller;

import com.dispatchops.application.service.UserService;
import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.web.advice.GlobalExceptionHandler;
import com.dispatchops.web.interceptor.AuthInterceptor;
import com.dispatchops.web.interceptor.RoleInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests that admin-only endpoints are properly protected.
 * Verifies COURIER, DISPATCHER, AUDITOR cannot access user management.
 */
@ExtendWith(MockitoExtension.class)
class AdminEndpointProtectionTest {

    @Mock private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new UserController(userService)
        ).addInterceptors(new AuthInterceptor(), new RoleInterceptor())
         .setControllerAdvice(new GlobalExceptionHandler())
         .build();
    }

    private MockHttpSession sessionForRole(Role role) {
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(100L);
        user.setUsername("testuser");
        user.setRole(role);
        user.setDisplayName("Test");
        user.setActive(true);
        session.setAttribute("currentUser", user);
        return session;
    }

    @Test
    void unauthenticated_cannotAccessUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"COURIER", "DISPATCHER", "AUDITOR"})
    void nonAdmin_cannotListUsers(Role role) throws Exception {
        mockMvc.perform(get("/api/users").session(sessionForRole(role)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"COURIER", "DISPATCHER", "AUDITOR"})
    void nonAdmin_cannotCreateUser(Role role) throws Exception {
        mockMvc.perform(post("/api/users")
                .session(sessionForRole(role))
                .contentType("application/json")
                .content("{\"username\":\"evil\",\"password\":\"pass\",\"role\":\"ADMIN\",\"displayName\":\"Evil\"}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"COURIER", "DISPATCHER", "AUDITOR"})
    void nonAdmin_cannotDeactivateUser(Role role) throws Exception {
        mockMvc.perform(put("/api/users/1/deactivate").session(sessionForRole(role)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_canListUsers() throws Exception {
        org.mockito.Mockito.when(userService.listUsers(0, 25, null))
                .thenReturn(new com.dispatchops.web.dto.PageResult<>(java.util.List.of(), 0, 25, 0));

        mockMvc.perform(get("/api/users").session(sessionForRole(Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void opsManager_canListUsers() throws Exception {
        org.mockito.Mockito.when(userService.listUsers(0, 25, null))
                .thenReturn(new com.dispatchops.web.dto.PageResult<>(java.util.List.of(), 0, 25, 0));

        mockMvc.perform(get("/api/users").session(sessionForRole(Role.OPS_MANAGER)))
                .andExpect(status().isOk());
    }
}

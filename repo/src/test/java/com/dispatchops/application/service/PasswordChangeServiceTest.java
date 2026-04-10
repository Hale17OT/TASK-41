package com.dispatchops.application.service;

import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.infrastructure.persistence.mapper.CreditLevelMapper;
import com.dispatchops.infrastructure.persistence.mapper.UserMapper;
import com.dispatchops.infrastructure.persistence.mapper.UserProfileMapper;
import com.dispatchops.infrastructure.security.FieldEncryptionService;
import com.dispatchops.infrastructure.security.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests that changePassword correctly persists the NEW hash
 * and clears must_change_password in a single update.
 */
@ExtendWith(MockitoExtension.class)
class PasswordChangeServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private UserProfileMapper userProfileMapper;
    @Mock private CreditLevelMapper creditLevelMapper;
    @Mock private PasswordService passwordService;
    @Mock private FieldEncryptionService fieldEncryptionService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, userProfileMapper, creditLevelMapper,
                passwordService, fieldEncryptionService, 5, 15);
    }

    @Test
    void changePassword_persistsNewHashNotOldHash() {
        User user = new User();
        user.setId(10L);
        user.setUsername("testuser");
        user.setRole(Role.DISPATCHER);
        user.setPasswordHash("$2a$12$OLD_HASH_SHOULD_BE_REPLACED");
        user.setMustChangePassword(true);
        user.setActive(true);

        when(userMapper.findById(10L)).thenReturn(user);
        when(passwordService.hash("NewSecurePass1!")).thenReturn("$2a$12$NEW_HASH_CORRECT");

        userService.changePassword(10L, "NewSecurePass1!");

        // Capture the User object passed to update()
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).update(captor.capture());

        User persisted = captor.getValue();
        // The persisted hash MUST be the new hash, not the old one
        assertEquals("$2a$12$NEW_HASH_CORRECT", persisted.getPasswordHash(),
                "changePassword must persist the NEW hash, not the old one from findById");
        assertFalse(persisted.isMustChangePassword(),
                "must_change_password flag must be cleared");
    }

    @Test
    void changePassword_clearsMustChangePasswordFlag() {
        User user = new User();
        user.setId(5L);
        user.setUsername("courier1");
        user.setRole(Role.COURIER);
        user.setPasswordHash("old_hash");
        user.setMustChangePassword(true);
        user.setActive(true);

        when(userMapper.findById(5L)).thenReturn(user);
        when(passwordService.hash(anyString())).thenReturn("new_hash");

        userService.changePassword(5L, "AnyPassword1!");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).update(captor.capture());

        assertFalse(captor.getValue().isMustChangePassword());
    }

    @Test
    void changePassword_singleUpdateCall() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setRole(Role.ADMIN);
        user.setPasswordHash("old");
        user.setMustChangePassword(true);
        user.setActive(true);

        when(userMapper.findById(1L)).thenReturn(user);
        when(passwordService.hash(anyString())).thenReturn("new");

        userService.changePassword(1L, "Pw123456!");

        // Should call update() exactly once (hash + flag in same call)
        verify(userMapper, times(1)).update(any(User.class));
        // Should NOT call updatePassword separately anymore
        verify(userMapper, never()).updatePassword(anyLong(), anyString());
    }
}

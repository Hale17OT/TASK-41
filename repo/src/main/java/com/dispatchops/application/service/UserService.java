package com.dispatchops.application.service;

import com.dispatchops.domain.exception.AccountLockedException;
import com.dispatchops.domain.exception.AuthenticationException;
import com.dispatchops.domain.exception.ResourceNotFoundException;
import com.dispatchops.domain.model.CreditLevelSnapshot;
import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.UserProfile;
import com.dispatchops.domain.model.enums.CreditLevel;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.infrastructure.persistence.mapper.CreditLevelMapper;
import com.dispatchops.infrastructure.persistence.mapper.UserMapper;
import com.dispatchops.infrastructure.persistence.mapper.UserProfileMapper;
import com.dispatchops.infrastructure.security.FieldEncryptionService;
import com.dispatchops.infrastructure.security.PasswordService;
import org.springframework.beans.factory.annotation.Value;
import com.dispatchops.web.dto.PageResult;
import com.dispatchops.web.dto.UserCreateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final int maxFailedAttempts;
    private final int lockoutDurationMinutes;

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final CreditLevelMapper creditLevelMapper;
    private final PasswordService passwordService;
    private final FieldEncryptionService fieldEncryptionService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private SearchService searchService;

    public UserService(UserMapper userMapper,
                       UserProfileMapper userProfileMapper,
                       CreditLevelMapper creditLevelMapper,
                       PasswordService passwordService,
                       FieldEncryptionService fieldEncryptionService,
                       @Value("${security.lockout.max-attempts:5}") int maxFailedAttempts,
                       @Value("${security.lockout.duration-minutes:15}") int lockoutDurationMinutes) {
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.creditLevelMapper = creditLevelMapper;
        this.passwordService = passwordService;
        this.fieldEncryptionService = fieldEncryptionService;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }

    @Transactional(noRollbackFor = {AuthenticationException.class, AccountLockedException.class})
    public User authenticate(String username, String rawPassword) {
        log.debug("Authenticating user: {}", username);

        User user = userMapper.findByUsername(username);
        if (user == null) {
            log.warn("Authentication failed: user '{}' not found", username);
            throw new AuthenticationException("Invalid username or password");
        }

        if (!user.isActive()) {
            log.warn("Authentication failed: user '{}' is deactivated", username);
            throw new AuthenticationException("Account is deactivated");
        }

        // Check lockout
        if (user.getLockoutExpiry() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (user.getLockoutExpiry().isAfter(now)) {
                long remainingSeconds = Duration.between(now, user.getLockoutExpiry()).getSeconds();
                long remainingMinutes = (remainingSeconds + 59) / 60;
                log.warn("Authentication failed: user '{}' is locked out for {} more minute(s)", username, remainingMinutes);
                throw new AccountLockedException(
                        "Account is locked. Try again in " + remainingMinutes + " minute(s).",
                        remainingSeconds
                );
            } else {
                // Lockout has expired, auto-reset
                log.info("Lockout expired for user '{}', resetting failed attempts", username);
                userMapper.resetFailedAttempts(user.getId());
                user.setFailedAttempts(0);
                user.setLockoutExpiry(null);
            }
        }

        // Verify password
        if (!passwordService.verify(rawPassword, user.getPasswordHash())) {
            int newFailedAttempts = user.getFailedAttempts() + 1;
            log.warn("Authentication failed: invalid password for user '{}'. Attempt {} of {}",
                    username, newFailedAttempts, maxFailedAttempts);

            LocalDateTime lockoutExpiry = null;
            if (newFailedAttempts >= maxFailedAttempts) {
                lockoutExpiry = LocalDateTime.now().plusMinutes(lockoutDurationMinutes);
                log.warn("User '{}' locked out until {}", username, lockoutExpiry);
            }

            userMapper.updateFailedAttempts(user.getId(), newFailedAttempts, lockoutExpiry);
            int remaining = maxFailedAttempts - newFailedAttempts;
            throw new AuthenticationException("Invalid username or password",
                    remaining > 0 ? remaining : 0);
        }

        // Success: reset failed attempts
        if (user.getFailedAttempts() > 0) {
            userMapper.resetFailedAttempts(user.getId());
        }

        log.info("User '{}' authenticated successfully", username);
        return user;
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(Long id) {
        log.debug("Fetching current user with id: {}", id);
        User user = userMapper.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with id: " + id, "User", id);
        }
        return user;
    }

    public User createUser(UserCreateDTO dto) {
        log.info("Creating new user with username: {}", dto.getUsername());

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPasswordHash(passwordService.hash(dto.getPassword()));
        user.setRole(Role.valueOf(dto.getRole()));
        user.setDisplayName(dto.getDisplayName());
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            user.setEmailEncrypted(fieldEncryptionService.encrypt(dto.getEmail()));
        }
        user.setActive(true);
        user.setFailedAttempts(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            user.setPhoneEncrypted(fieldEncryptionService.encrypt(dto.getPhone()));
        }

        userMapper.insert(user);
        log.info("User created with id: {}", user.getId());

        // Create default UserProfile
        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setVisibilityLevel(0);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        userProfileMapper.insert(profile);
        log.debug("Default UserProfile created for user id: {}", user.getId());

        // Create default CreditLevel if role is COURIER
        if (user.getRole() == Role.COURIER) {
            CreditLevelSnapshot snapshot = new CreditLevelSnapshot();
            snapshot.setCourierId(user.getId());
            snapshot.setLevel(CreditLevel.C);
            snapshot.setMaxConcurrent(CreditLevel.C.getMaxConcurrent());
            snapshot.setAvgRating30d(BigDecimal.ZERO);
            snapshot.setViolationsActive(0);
            snapshot.setCalculatedAt(LocalDateTime.now());
            creditLevelMapper.upsert(snapshot);
            log.debug("Default CreditLevel snapshot created for courier id: {}", user.getId());
        }

        if (searchService != null) {
            try { searchService.indexEntity("USER", user.getId(), user.getDisplayName(),
                    user.getUsername(),
                    user.getRole().name(), user.getId()); }
            catch (Exception e) { log.warn("Failed to index user: {}", e.getMessage()); }
        }

        return user;
    }

    public User updateUser(Long id, UserCreateDTO dto) {
        log.info("Updating user with id: {}", id);

        User existing = userMapper.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("User not found with id: " + id, "User", id);
        }

        existing.setUsername(dto.getUsername());
        existing.setDisplayName(dto.getDisplayName());
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            existing.setEmailEncrypted(fieldEncryptionService.encrypt(dto.getEmail()));
        } else {
            existing.setEmailEncrypted(null);
        }
        existing.setRole(Role.valueOf(dto.getRole()));
        existing.setUpdatedAt(LocalDateTime.now());

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            existing.setPasswordHash(passwordService.hash(dto.getPassword()));
        }

        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            existing.setPhoneEncrypted(fieldEncryptionService.encrypt(dto.getPhone()));
        } else {
            existing.setPhoneEncrypted(null);
        }

        userMapper.update(existing);
        log.info("User with id: {} updated successfully", id);
        if (searchService != null) {
            try { searchService.indexEntity("USER", existing.getId(), existing.getDisplayName(),
                    existing.getUsername(),
                    existing.getRole().name(), existing.getId()); }
            catch (Exception e) { log.warn("Failed to reindex user: {}", e.getMessage()); }
        }
        return existing;
    }

    public void deactivateUser(Long id) {
        log.info("Deactivating user with id: {}", id);

        User user = userMapper.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with id: " + id, "User", id);
        }

        userMapper.deactivate(id);
        log.info("User with id: {} deactivated", id);
        if (searchService != null) {
            try { searchService.deindexEntity("USER", id); }
            catch (Exception e) { log.warn("Failed to deindex user: {}", e.getMessage()); }
        }
    }

    public void unlockUser(Long id) {
        log.info("Unlocking user with id: {}", id);

        User user = userMapper.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with id: " + id, "User", id);
        }

        userMapper.resetFailedAttempts(id);
        log.info("User with id: {} unlocked", id);
    }

    public void changePassword(Long id, String newPassword) {
        log.info("Changing password for user with id: {}", id);

        User user = userMapper.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with id: " + id, "User", id);
        }

        String hash = passwordService.hash(newPassword);
        // Set new hash AND clear must_change_password on the in-memory user before persisting
        user.setPasswordHash(hash);
        user.setMustChangePassword(false);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);
        log.info("Password changed for user with id: {} (must_change_password cleared)", id);
    }

    @Transactional(readOnly = true)
    public PageResult<User> listUsers(int page, int size, String roleFilter) {
        log.debug("Listing users - page: {}, size: {}, roleFilter: {}", page, size, roleFilter);

        int offset = page * size;
        List<User> users;
        long total;

        if (roleFilter != null && !roleFilter.isBlank()) {
            users = userMapper.findAllFiltered(roleFilter, offset, size);
            total = userMapper.countByRole(roleFilter);
        } else {
            users = userMapper.findAll(offset, size);
            total = userMapper.countAll();
        }

        // Scrub sensitive fields
        for (User user : users) {
            user.setPasswordHash(null);
            user.setEmailEncrypted(null);
            user.setPhoneEncrypted(null);
        }

        return new PageResult<>(users, page, size, total);
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        log.debug("Finding user by id: {}", id);
        User user = userMapper.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with id: " + id, "User", id);
        }
        return user;
    }
}

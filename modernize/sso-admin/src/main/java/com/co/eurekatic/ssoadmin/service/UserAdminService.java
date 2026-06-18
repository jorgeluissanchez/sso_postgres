package com.co.eurekatic.ssoadmin.service;

import com.co.eurekatic.common.entity.Role;
import com.co.eurekatic.common.entity.User;
import com.co.eurekatic.common.repository.RoleRepository;
import com.co.eurekatic.common.repository.UserRepository;
import com.co.eurekatic.ssoadmin.dto.CreateAccountRequest;
import com.co.eurekatic.ssoadmin.dto.UpdateAccountRequest;
import com.co.eurekatic.ssoadmin.dto.UserResponse;
import com.co.eurekatic.ssoadmin.exception.EmailInvalidException;
import com.co.eurekatic.ssoadmin.exception.NotFoundException;
import com.co.eurekatic.ssoadmin.exception.UserDuplicateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * User administration service. Mirrors the user-management
 * surface of the legacy {@code com.co.lowcode.sso.service.UserService}
 * — modernized to use JPA repos, the {@code common} module's
 * {@link User} / {@link Role} entities, and {@link EmailService}
 * for activation / restore flows.
 *
 * <p>Side effects:
 * <ul>
 *   <li>{@link #createAccount} — creates a User in the DB
 *       (status: {@code active=true}, {@code enabled=false} until
 *       activation), issues an activation token, sends an
 *       activation email.</li>
 *   <li>{@link #activateAccount} — clears the activation token,
 *       sets {@code enabled=true} and {@code active=true},
 *       BCrypt-encodes the new password.</li>
 *   <li>{@link #forgotPassword} — issues a restore token and
 *       sends a restore-password email. Always returns silently
 *       even if the email is unknown, to avoid leaking which
 *       addresses are registered.</li>
 * </ul>
 */
@Service
public class UserAdminService {

    private static final Logger log = LoggerFactory.getLogger(UserAdminService.class);
    // RFC-5322 is huge; this is a practical subset that catches
    // the common typos. The full check happens in the DTO with
    // @Email — this is the second line of defense.
    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailService emailService;

    public UserAdminService(UserRepository userRepository,
                            RoleRepository roleRepository,
                            PasswordEncoder passwordEncoder,
                            TokenService tokenService,
                            EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.emailService = emailService;
    }

    /**
     * Creates a new user. The user is created in
     * {@code active=true} but {@code enabled=false} state — they
     * have to click the activation link in the email before they
     * can log in. (This mirrors the legacy flow.)
     */
    @Transactional
    public UserResponse createAccount(CreateAccountRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new UserDuplicateException(req.username());
        }
        if (req.email() == null || !EMAIL_REGEX.matcher(req.email()).matches()) {
            throw new EmailInvalidException(req.email());
        }
        if (req.passwordConfirm() != null && !req.password().equals(req.passwordConfirm())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = new User();
        user.setUsername(req.username());
        user.setFullName(req.fullName());
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setActive(true);
        user.setEnabled(false);
        user.setLdap(false);

        if (req.roleNames() != null && !req.roleNames().isEmpty()) {
            Set<Role> roles = resolveRoles(req.roleNames());
            for (Role r : roles) {
                user.addRole(r);
            }
        }

        String token = tokenService.issueActivationToken(user);
        User saved = userRepository.save(user);

        emailService.sendActivationEmail(saved, token);
        log.info("Created user '{}' (active=true, enabled=false, pending activation)",
                saved.getUsername());
        return UserResponse.fromEntity(saved);
    }

    /**
     * Updates mutable fields on an existing user. Null fields are
     * left unchanged. If {@code roleNames} is non-null, it
     * REPLACES the user's current role set.
     */
    @Transactional
    public UserResponse updateAccount(UpdateAccountRequest req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new NotFoundException("User", req.username()));

        if (req.fullName() != null) user.setFullName(req.fullName());
        if (req.email() != null) {
            if (!EMAIL_REGEX.matcher(req.email()).matches()) {
                throw new EmailInvalidException(req.email());
            }
            user.setEmail(req.email());
        }
        if (req.active() != null) user.setActive(req.active());
        if (req.ldap() != null) user.setLdap(req.ldap());

        if (req.roleNames() != null) {
            // Replace strategy: remove roles that aren't in the
            // new set, add the ones that are. The
            // @Setter(AccessLevel.NONE) on User.roles means we
            // mutate the existing Set rather than replacing it.
            Set<Role> current = user.getRoles();
            Set<String> wanted = req.roleNames().stream()
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            current.removeIf(r -> !wanted.contains(r.getName()));
            for (String name : wanted) {
                if (current.stream().noneMatch(r -> r.getName().equals(name))) {
                    Role role = roleRepository.findByName(name)
                            .orElseThrow(() -> new NotFoundException("Role", name));
                    user.addRole(role);
                }
            }
        }

        return UserResponse.fromEntity(userRepository.save(user));
    }

    /**
     * Activates a user via the token issued in
     * {@link #createAccount}. Sets {@code enabled=true},
     * {@code active=true}, BCrypt-encodes the new password, and
     * clears the token column.
     */
    @Transactional
    public void activateAccount(String token, String password) {
        if (password == null || password.isBlank() || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        User user = tokenService.consumeActivationToken(token);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.setActive(true);
        userRepository.save(user);
        log.info("Activated user '{}'", user.getUsername());
    }

    /**
     * Restores a password via the token sent in
     * {@link #forgotPassword}. Same as {@link #activateAccount}
     * but for the restore flow.
     */
    @Transactional
    public void restorePassword(String token, String password) {
        if (password == null || password.isBlank() || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        User user = tokenService.consumeRestoreToken(token);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        log.info("Restored password for user '{}'", user.getUsername());
    }

    /**
     * Issues a restore-password email. Always returns
     * successfully (even if the email is unknown) so the API
     * doesn't leak which addresses are registered.
     */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findAll().stream()
                .filter(u -> email.equals(u.getEmail()))
                .findFirst()
                .ifPresent(u -> {
                    String token = tokenService.issueRestoreToken(u);
                    userRepository.save(u);
                    emailService.sendRestorePasswordEmail(u, token);
                });
    }

    /**
     * Lists all users (id, fullName, username, ldap, active).
     * The legacy returned a flat Map list; we return a typed
     * {@code List<UserResponse>}.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    /**
     * Returns the role names for the given username. The legacy
     * returned a {@code List<String>} of role names; we keep
     * the same shape for API compatibility.
     */
    @Transactional(readOnly = true)
    public List<String> getRolesByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User", username))
                .getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }

    /**
     * Binds a role to a user. Idempotent — re-binding the same
     * pair is a no-op.
     */
    @Transactional
    public void bindUserRole(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role", roleId));
        user.addRole(role);
        userRepository.save(user);
    }

    /**
     * Unbinds a role from a user. Idempotent — unbinding a
     * non-existent pair is a no-op.
     */
    @Transactional
    public void unbindUserRole(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));
        user.getRoles().stream()
                .filter(r -> r.getId().equals(roleId))
                .findFirst()
                .ifPresent(user::removeRole);
        userRepository.save(user);
    }

    /**
     * Returns the user's roles as a list of role names. The
     * legacy endpoint {@code GET /user/roles?userId} returned a
     * {@code List<Map>} — we return a typed
     * {@code List<String>}.
     */
    @Transactional(readOnly = true)
    public List<String> getRolesForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));
        return user.getRoles().stream().map(Role::getName).toList();
    }

    private Set<Role> resolveRoles(List<String> names) {
        Set<Role> result = new LinkedHashSet<>();
        for (String name : names) {
            Role role = roleRepository.findByName(name)
                    .orElseThrow(() -> new NotFoundException("Role", name));
            result.add(role);
        }
        return result;
    }
}

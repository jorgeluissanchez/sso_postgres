package com.co.eurekatic.ssoadmin.service;

import com.co.eurekatic.common.entity.User;
import com.co.eurekatic.common.repository.UserRepository;
import com.co.eurekatic.ssoadmin.exception.TokenNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Generates and validates the activation and restore-password
 * tokens that link the email-link flow to the user record.
 *
 * <p>The token is a 36-char UUID written to either
 * {@code User.tokenActivation} (new account) or
 * {@code User.tokenRestore} (forgot password). No expiry column
 * — the legacy never had one either, and a token is invalidated
 * by clearing the column on successful use. A future
 * enhancement could add an expiry by storing the issuance
 * timestamp and checking on validate.
 */
@Service
public class TokenService {

    private final UserRepository userRepository;

    public TokenService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Issues a new activation token and persists it on the
     * user's {@code tokenActivation} column. The user is not
     * saved elsewhere by this method — caller is responsible
     * for saving the user (e.g. on createAccount).
     *
     * @return the new token (36-char UUID)
     */
    public String issueActivationToken(User user) {
        String token = generate();
        user.setTokenActivation(token);
        return token;
    }

    /**
     * Issues a new restore-password token and persists it on
     * the user's {@code tokenRestore} column. Caller is
     * responsible for saving the user.
     */
    public String issueRestoreToken(User user) {
        String token = generate();
        user.setTokenRestore(token);
        return token;
    }

    /**
     * Looks up the user that holds the given activation token.
     * Clears the token column on the returned entity — caller
     * is responsible for saving the user (i.e. persisting the
     * cleared column).
     *
     * @throws TokenNotFoundException if no user has this token
     */
    @Transactional
    public User consumeActivationToken(String token) {
        User user = userRepository.findByTokenActivation(token)
                .orElseThrow(TokenNotFoundException::new);
        user.setTokenActivation(null);
        return user;
    }

    /**
     * Same as {@link #consumeActivationToken} but for the
     * restore-password flow.
     */
    @Transactional
    public User consumeRestoreToken(String token) {
        User user = userRepository.findByTokenRestore(token)
                .orElseThrow(TokenNotFoundException::new);
        user.setTokenRestore(null);
        return user;
    }

    private static String generate() {
        return UUID.randomUUID().toString();
    }
}

package com.example.geminiapp.service;

import com.example.geminiapp.entity.User;
import com.example.geminiapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Process OAuth2 user - create new user or return existing one
     */
    public User processOAuth2User(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String googleId = oAuth2User.getAttribute("sub");
        String name = oAuth2User.getAttribute("name");

        logger.info("Processing OAuth2 user with email: {} and Google ID: {}", email, googleId);

        // First try to find by Google ID
        Optional<User> existingUser = userRepository.findByGoogleId(googleId);
        if (existingUser.isPresent()) {
            logger.info("Found existing user by Google ID: {}", googleId);
            return existingUser.get();
        }

        // Then try to find by email (in case user registered with email first)
        existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Link the Google account to existing email account
            user.setGoogleId(googleId);
            user.setIsAnonymous(false);
            logger.info("Linked Google account to existing email user: {}", email);
            return userRepository.save(user);
        }

        // Create new user
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setGoogleId(googleId);
        newUser.setIsAnonymous(false);
        // No fingerprint for OAuth users initially
        
        User savedUser = userRepository.save(newUser);
        logger.info("Created new OAuth2 user with email: {}", email);
        return savedUser;
    }

    /**
     * Create anonymous user with fingerprint
     */
    public User createAnonymousUser(String fingerprint) {
        // Check if anonymous user with this fingerprint already exists
        Optional<User> existingUser = userRepository.findByFingerprintAndIsAnonymousTrue(fingerprint);
        if (existingUser.isPresent()) {
            logger.info("Found existing anonymous user with fingerprint: {}", fingerprint);
            return existingUser.get();
        }

        User anonymousUser = new User(fingerprint);
        User savedUser = userRepository.save(anonymousUser);
        logger.info("Created new anonymous user with fingerprint: {}", fingerprint);
        return savedUser;
    }

    /**
     * Find or create anonymous user with fingerprint
     */
    public User findOrCreateAnonymousUser(String fingerprint) {
        return createAnonymousUser(fingerprint);
    }

    /**
     * Register user with email and password
     */
    public User registerUserWithEmail(String email, String password, String fingerprint) {
        // Check if user with this email already exists
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            throw new RuntimeException("User with email " + email + " already exists");
        }

        // Check if there's an anonymous user with this fingerprint to link
        Optional<User> anonymousUser = userRepository.findByFingerprintAndIsAnonymousTrue(fingerprint);
        
        User user;
        if (anonymousUser.isPresent()) {
            // Convert anonymous user to registered user
            user = anonymousUser.get();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setIsAnonymous(false);
            logger.info("Converted anonymous user to registered user with email: {}", email);
        } else {
            // Create new registered user
            user = new User(email, passwordEncoder.encode(password), fingerprint);
            logger.info("Created new registered user with email: {}", email);
        }

        return userRepository.save(user);
    }

    /**
     * Authenticate user with email and password
     */
    public Optional<User> authenticateUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword() != null && passwordEncoder.matches(password, user.getPassword())) {
                logger.info("Successfully authenticated user with email: {}", email);
                return Optional.of(user);
            }
        }
        logger.warn("Failed authentication attempt for email: {}", email);
        return Optional.empty();
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Link anonymous user to registered user when they register
     */
    public User linkAnonymousToRegistered(String fingerprint, User registeredUser) {
        Optional<User> anonymousUser = userRepository.findByFingerprintAndIsAnonymousTrue(fingerprint);
        if (anonymousUser.isPresent()) {
            User anonymous = anonymousUser.get();
            // Transfer request logs from anonymous user to registered user
            anonymous.getRequestLogs().forEach(requestLog -> {
                requestLog.setUser(registeredUser);
                registeredUser.addRequestLog(requestLog);
            });
            
            // Delete the anonymous user record
            userRepository.delete(anonymous);
            logger.info("Linked anonymous user requests to registered user: {}", registeredUser.getEmail());
        }
        return userRepository.save(registeredUser);
    }

    /**
     * Get user's remaining request quota
     */
    public long getRemainingQuota(User user) {
        return user.getRemainingRequests();
    }

    /**
     * Check if user has reached their quota limit
     */
    public boolean hasReachedQuota(User user) {
        return user.hasReachedQuota();
    }

    /**
     * Find user by fingerprint (for anonymous users)
     */
    public Optional<User> findByFingerprint(String fingerprint) {
        return userRepository.findByFingerprint(fingerprint);
    }

    /**
     * Update user fingerprint (useful for linking sessions)
     */
    public User updateUserFingerprint(UUID userId, String fingerprint) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setFingerprint(fingerprint);
            User savedUser = userRepository.save(user);
            logger.info("Updated fingerprint for user: {}", userId);
            return savedUser;
        }
        throw new RuntimeException("User not found with id: " + userId);
    }

    /**
     * Process OAuth2 user with fingerprint linking
     */
    public User processOAuth2UserWithFingerprint(OAuth2User oAuth2User, String fingerprint) {
        User user = processOAuth2User(oAuth2User);
        
        // If user doesn't have a fingerprint, set it
        if (user.getFingerprint() == null || user.getFingerprint().isEmpty()) {
            user.setFingerprint(fingerprint);
            user = userRepository.save(user);
        }
        
        // Try to link any existing anonymous user with this fingerprint
        if (fingerprint != null && !fingerprint.isEmpty()) {
            linkAnonymousToRegistered(fingerprint, user);
        }
        
        return user;
    }

    /**
     * Get user statistics
     */
    public UserStats getUserStats(User user) {
        return new UserStats(
            user.getId(),
            user.getEmail(),
            user.getIsAnonymous(),
            user.getRequestCount(),
            user.getRemainingRequests(),
            user.getCreatedAt()
        );
    }

    /**
     * Inner class for user statistics
     */
    public static class UserStats {
        private final UUID id;
        private final String email;
        private final boolean isAnonymous;
        private final long totalRequests;
        private final long remainingRequests;
        private final java.time.LocalDateTime createdAt;

        public UserStats(UUID id, String email, boolean isAnonymous, long totalRequests, 
                        long remainingRequests, java.time.LocalDateTime createdAt) {
            this.id = id;
            this.email = email;
            this.isAnonymous = isAnonymous;
            this.totalRequests = totalRequests;
            this.remainingRequests = remainingRequests;
            this.createdAt = createdAt;
        }

        // Getters
        public UUID getId() { return id; }
        public String getEmail() { return email; }
        public boolean isAnonymous() { return isAnonymous; }
        public long getTotalRequests() { return totalRequests; }
        public long getRemainingRequests() { return remainingRequests; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    }
}
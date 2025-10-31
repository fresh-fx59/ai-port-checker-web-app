package com.example.geminiapp.controller;

import com.example.geminiapp.dto.AuthResponse;
import com.example.geminiapp.dto.LoginRequest;
import com.example.geminiapp.dto.RegisterRequest;
import com.example.geminiapp.entity.User;
import com.example.geminiapp.security.JwtTokenProvider;
import com.example.geminiapp.security.UserPrincipal;
import com.example.geminiapp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    /**
     * Register a new user with email and password
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest,
                                                   HttpServletRequest request) {
        try {
            // Validate password confirmation
            if (!registerRequest.isPasswordMatching()) {
                return ResponseEntity.badRequest()
                    .body(AuthResponse.error("Passwords do not match"));
            }

            // Get fingerprint from request or use provided one
            String fingerprint = registerRequest.getFingerprint();
            if (fingerprint == null || fingerprint.trim().isEmpty()) {
                fingerprint = generateFingerprint(request);
            }

            // Register the user
            User user = userService.registerUserWithEmail(
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                fingerprint
            );

            // Generate JWT token
            String jwt = tokenProvider.generateTokenFromUserId(user.getId().toString());

            // Create user info
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getIsAnonymous(),
                user.getRemainingRequests()
            );

            logger.info("User registered successfully: {}", user.getEmail());
            return ResponseEntity.ok(AuthResponse.success("User registered successfully", jwt, userInfo));

        } catch (RuntimeException e) {
            logger.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(AuthResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Registration error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AuthResponse.error("Registration failed. Please try again."));
        }
    }

    /**
     * Authenticate user with email and password
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Authenticate user
            Optional<User> userOpt = userService.authenticateUser(
                loginRequest.getEmail(),
                loginRequest.getPassword()
            );

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Invalid email or password"));
            }

            User user = userOpt.get();

            // Create authentication token
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate JWT token
            String jwt = tokenProvider.generateToken(authentication);

            // Create user info
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getIsAnonymous(),
                user.getRemainingRequests()
            );

            logger.info("User logged in successfully: {}", user.getEmail());
            return ResponseEntity.ok(AuthResponse.success("Login successful", jwt, userInfo));

        } catch (Exception e) {
            logger.error("Login error for email: {}", loginRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponse.error("Invalid email or password"));
        }
    }

    /**
     * Get current user information
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("User not authenticated"));
            }

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Optional<User> userOpt = userService.findById(userPrincipal.getId());

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AuthResponse.error("User not found"));
            }

            User user = userOpt.get();
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getIsAnonymous(),
                user.getRemainingRequests()
            );

            return ResponseEntity.ok(AuthResponse.success("User information retrieved", null, userInfo));

        } catch (Exception e) {
            logger.error("Error getting current user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AuthResponse.error("Failed to get user information"));
        }
    }

    /**
     * Logout user (client-side token removal)
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logoutUser() {
        // Since we're using stateless JWT tokens, logout is handled client-side
        // by removing the token from storage
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(AuthResponse.success("Logout successful", null, null));
    }

    /**
     * Google OAuth callback endpoint
     * This endpoint handles the OAuth2 callback and returns user information with JWT token
     */
    @GetMapping("/oauth2/callback/google")
    public ResponseEntity<AuthResponse> googleOAuthCallback(@RequestParam(required = false) String token,
                                                          @RequestParam(required = false) String error,
                                                          HttpServletRequest request) {
        try {
            if (error != null && !error.isEmpty()) {
                logger.error("OAuth2 authentication failed: {}", error);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("OAuth2 authentication failed: " + error));
            }

            if (token == null || token.isEmpty()) {
                logger.error("No token received from OAuth2 callback");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthResponse.error("No authentication token received"));
            }

            // Validate the JWT token
            if (!tokenProvider.validateToken(token)) {
                logger.error("Invalid JWT token received from OAuth2 callback");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Invalid authentication token"));
            }

            // Extract user ID from token
            String userId = tokenProvider.getUserIdFromToken(token);
            Optional<User> userOpt = userService.findById(UUID.fromString(userId));

            if (userOpt.isEmpty()) {
                logger.error("User not found for OAuth2 callback: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AuthResponse.error("User not found"));
            }

            User user = userOpt.get();
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getIsAnonymous(),
                user.getRemainingRequests()
            );

            logger.info("OAuth2 callback successful for user: {}", user.getEmail());
            return ResponseEntity.ok(AuthResponse.success("OAuth2 authentication successful", token, userInfo));

        } catch (Exception e) {
            logger.error("Error processing OAuth2 callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AuthResponse.error("OAuth2 callback processing failed"));
        }
    }

    /**
     * Create anonymous user session
     */
    @PostMapping("/anonymous")
    public ResponseEntity<AuthResponse> createAnonymousSession(HttpServletRequest request) {
        try {
            String fingerprint = generateFingerprint(request);
            User anonymousUser = userService.createAnonymousUser(fingerprint);

            // Generate JWT token for anonymous user
            String jwt = tokenProvider.generateTokenFromUserId(anonymousUser.getId().toString());

            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                anonymousUser.getId(),
                null, // No email for anonymous users
                anonymousUser.getIsAnonymous(),
                anonymousUser.getRemainingRequests()
            );

            logger.info("Anonymous user session created with fingerprint: {}", fingerprint);
            return ResponseEntity.ok(AuthResponse.success("Anonymous session created", jwt, userInfo));

        } catch (Exception e) {
            logger.error("Error creating anonymous session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AuthResponse.error("Failed to create anonymous session"));
        }
    }

    /**
     * Generate fingerprint from request
     */
    private String generateFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String acceptLanguage = request.getHeader("Accept-Language");
        String remoteAddr = getClientIpAddress(request);
        
        // Create a simple fingerprint (in production, use more sophisticated methods)
        return String.valueOf((userAgent + acceptLanguage + remoteAddr).hashCode());
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
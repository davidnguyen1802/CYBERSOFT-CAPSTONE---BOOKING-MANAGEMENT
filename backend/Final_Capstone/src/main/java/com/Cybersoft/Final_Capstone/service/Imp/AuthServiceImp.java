package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.*;
import com.Cybersoft.Final_Capstone.Enum.Gender;
import com.Cybersoft.Final_Capstone.components.JwtTokenUtil;
import com.Cybersoft.Final_Capstone.events.UserCreatedEvent;
import com.Cybersoft.Final_Capstone.exception.*;
import com.Cybersoft.Final_Capstone.payload.request.SignInRequest;
import com.Cybersoft.Final_Capstone.payload.request.SignUpRequest;
import com.Cybersoft.Final_Capstone.repository.*;
import com.Cybersoft.Final_Capstone.service.AuthService;
import com.Cybersoft.Final_Capstone.service.EmailService;
import com.Cybersoft.Final_Capstone.service.FileStorageService;
import com.Cybersoft.Final_Capstone.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.lang.IllegalArgumentException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImp implements AuthService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private TokenService tokenService;

    @Value("${password-reset.token-expiration}")
    private Long tokenExpirationMs;

    @Transactional
    @Override
    public UserAccount signUp(SignUpRequest signUpRequest, MultipartFile avatar) throws Exception {
        // Check if username already exists
        if (userAccountRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new UsernameDuplicateException("Username already exists");
        }

        // Check if email already exists
        if (userAccountRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new EmailDuplicateException("Email already exists");
        }

        // Check if phone already exists (if phone is provided)
        if (signUpRequest.getPhone() != null && !signUpRequest.getPhone().trim().isEmpty()) {
            if (userAccountRepository.existsByPhone(signUpRequest.getPhone())) {
                throw new PhoneDuplicateException("Phone number already exists");
            }
        }

        // Create new user account
        UserAccount newUser = new UserAccount();
        newUser.setFullName(signUpRequest.getFullName());
        newUser.setUsername(signUpRequest.getUsername());
        newUser.setEmail(signUpRequest.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(signUpRequest.getPassword()));
        newUser.setPhone(signUpRequest.getPhone());
        newUser.setAddress(signUpRequest.getAddress());
        newUser.setGender(signUpRequest.getGender());
        newUser.setDob(signUpRequest.getDob());
        newUser.setRole(new Role(3));
        newUser.setStatus(new Status(1));
        newUser.setCreateDate(LocalDate.now());
        newUser.setPriority(0); // Default priority

        // Save user first to get the ID
        UserAccount savedUser = userAccountRepository.save(newUser);

        // Save avatar if provided
        if (avatar != null && !avatar.isEmpty()) {
            try {
                String avatarPath = fileStorageService.saveFileWithCustomName(
                    avatar,
                    "avatar_user_" + savedUser.getId()
                );
                savedUser.setAvatar(avatarPath);
                savedUser = userAccountRepository.save(savedUser);
            } catch (Exception e) {
                // Log error but don't fail the registration
                System.err.println("Failed to save avatar: " + e.getMessage());
            }
        }

        // Publish event for system stats tracking
        eventPublisher.publishEvent(new UserCreatedEvent(savedUser.getId()));

        // Return the saved user entity - controller will handle token generation
        return savedUser;
    }

    @Override
    public String getUserIdFromToken(String token) {
        Integer userId = jwtTokenUtil.getUserId(token);
        return userId != null ? userId.toString() : null;
    }

    @Override
    public String uploadAvatar(Integer userId, MultipartFile avatar) {
        // Find user
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + userId));

        // Delete old avatar if exists
        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            try {
                fileStorageService.deleteFile(user.getAvatar());
            } catch (Exception e) {
                // Log but don't fail if old avatar can't be deleted
                System.err.println("Could not delete old avatar: " + e.getMessage());
            }
        }

        // Save new avatar
        String avatarPath = fileStorageService.saveFileWithCustomName(
            avatar,
            "avatar_user_" + userId
        );

        // Update user record
        user.setAvatar(avatarPath);
        userAccountRepository.save(user);

        return avatarPath;
    }

    @Transactional
    @Override
    public String forgotPassword(String email) {
        // Find user by email
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new DataNotFoundException("No account found with email: " + email));

        // Delete any existing reset tokens for this user
        passwordResetTokenRepository.deleteByUserAccount(user);

        // Generate unique reset token
        String token = UUID.randomUUID().toString();

        // Calculate expiry date (current time + configured expiration)
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(tokenExpirationMs / 1000);

        // Create and save password reset token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUserAccount(user);
        resetToken.setExpiryDate(expiryDate);
        resetToken.setUsed(false);
        resetToken.setCreatedAt(LocalDateTime.now());

        passwordResetTokenRepository.save(resetToken);

        // Send password reset email
        emailService.sendPasswordResetEmail(user.getEmail(), token, user.getFullName());

        return "Password reset link has been sent to your email address";
    }

    @Transactional
    @Override
    public String resetPassword(String token, String newPassword) {
        // Find token
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidException("Invalid password reset token"));

        // Check if token is expired
        if (resetToken.isExpired()) {
            throw new InvalidException("Password reset token has expired. Please request a new one.");
        }

        // Check if token has been used
        if (resetToken.isUsed()) {
            throw new InvalidException("This password reset token has already been used");
        }

        // Get user and update password
        UserAccount user = resetToken.getUserAccount();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userAccountRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return "Password has been reset successfully. You can now sign in with your new password.";
    }

    @Override
    public boolean validatePasswordResetToken(String token) {
        return passwordResetTokenRepository.findByToken(token)
                .map(resetToken -> !resetToken.isExpired() && !resetToken.isUsed())
                .orElse(false);
    }

    @Override
    public String signOut(String token) {
        // Validate token and get user ID
        Integer userId = jwtTokenUtil.getUserId(token);
        if (userId == null) {
            throw new InvalidException("Invalid token");
        }

        // Verify user exists
        userAccountRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found"));

        // Revoke the token in database
        tokenService.revokeToken(token);

        return "Sign out successful";
    }

    @Override
    public String signOutAllDevices(String token) {
        // Validate token and get user ID
        Integer userId = jwtTokenUtil.getUserId(token);
        if (userId == null) {
            throw new InvalidException("Invalid token");
        }

        // Get user
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found"));

        // Revoke all tokens for this user
        tokenService.revokeAllUserTokens(user);

        return "Successfully signed out from all devices";
    }

    @Override
    public String login(SignInRequest signInRequest) throws Exception {
        String usernameOrEmail = signInRequest.getUsernameOrEmail();
        
        // Try to find user by email first
        Optional<UserAccount> optionalUserAccount = userAccountRepository.findByEmail(usernameOrEmail);
        
        // If not found by email, try username
        if (optionalUserAccount.isEmpty()) {
            optionalUserAccount = userAccountRepository.findByUsername(usernameOrEmail);
        }
        
        // If still not found, throw exception
        UserAccount existUser = optionalUserAccount.orElseThrow(() -> 
            new DataNotFoundException("Invalid username or email!"));

        if(!passwordEncoder.matches(signInRequest.getPassword(), existUser.getPasswordHash())) {
            throw new InvalidPasswordException("Wrong Password !");
        }

        if(!existUser.getStatus().getName().equals("ACTIVE")) {
            throw new DataNotFoundException("Your account has been deactivated !");
        }

        return jwtTokenUtil.generateAccessToken(existUser);
    }

    @Override
    public UserAccount getUserDetailsFromToken(String token) throws Exception {
        if(jwtTokenUtil.isTokenExpired(token)) {
            throw new ExpiredTokenException("Token is expired");
        }

        // Get userId from token claims (primary method)
        Integer userId = jwtTokenUtil.getUserId(token);
        if (userId != null) {
            Optional<UserAccount> user = userAccountRepository.findById(userId);
            if (user.isPresent()) {
                return user.get();
            }
        }

        // Fallback: Get subject from token (for backward compatibility)
        String subject = jwtTokenUtil.getSubject(token);
        Optional<UserAccount> user;

        // Try to find by email first
        user = userAccountRepository.findByEmail(subject);
        if(user.isEmpty()) {
            // Then try username
            user = userAccountRepository.findByUsername(subject);
        }

        return user.orElseThrow(() -> new Exception("User not found"));
    }

    @Override
    public UserAccount getUserDetailsFromRefreshToken(String refreshToken) throws Exception {
        // Validate refresh token first
        if (!jwtTokenUtil.validateRefreshToken(refreshToken)) {
            throw new InvalidException("Invalid refresh token");
        }

        // Get JTI from token
        String jti = jwtTokenUtil.getJtiFromToken(refreshToken);

        // Find token by JTI
        Token existingToken = tokenRepository.findByJti(jti)
                .orElseThrow(() -> new DataNotFoundException("Refresh token not found"));

        // Check if token is revoked
        if (existingToken.isRevoked()) {
            throw new InvalidException("Refresh token has been revoked");
        }

        // Check if token is expired
        if (existingToken.getExpiresAt().isBefore(java.time.Instant.now())) {
            throw new ExpiredTokenException("Refresh token has expired");
        }

        return existingToken.getUser();
    }

    @Override
    public String loginSocial(SignInRequest signInRequests) throws Exception {
        Optional<UserAccount> optionalUser = Optional.empty();
        Role roleUser = roleRepository.findByName(Role.GUEST)
                .orElseThrow(() -> new DataNotFoundException("Role doesn't exist."));

        // Kiểm tra Google Account ID
        if (signInRequests.isGoogleAccountIdValid()) {
            optionalUser = userAccountRepository.findByGoogleAccountId(signInRequests.getGoogleAccountId());

            // Tạo người dùng mới nếu không tìm thấy
            if (optionalUser.isEmpty()) {
                // Generate unique username from email or Google account ID
                String username = signInRequests.getEmail() != null && !signInRequests.getEmail().isEmpty()
                        ? signInRequests.getEmail().split("@")[0] + "_" + System.currentTimeMillis()
                        : "google_" + signInRequests.getGoogleAccountId();
                
                UserAccount newUser = UserAccount.builder()
                        .fullName(Optional.ofNullable(signInRequests.getFullname()).orElse("Unknown"))
                        .username(username)
                        .email(Optional.ofNullable(signInRequests.getEmail()).orElse("Unknown"))
                        .avatar(Optional.ofNullable(signInRequests.getProfileImage()).orElse("Unknown"))
                        .role(roleUser)
                        .googleAccountId(signInRequests.getGoogleAccountId())
                        .gender(Gender.NONE)
                        .passwordHash("") // Mật khẩu trống cho đăng nhập mạng xã hội
                        .status(new Status(1)) // Active status
                        .createDate(LocalDate.now())
                        .priority(0)
                        .build();

                // Lưu người dùng mới
                newUser = userAccountRepository.save(newUser);
                optionalUser = Optional.of(newUser);
            }
        }
        // Kiểm tra Facebook Account ID
        else if (signInRequests.isFacebookAccountIdValid()) {
            optionalUser = userAccountRepository.findByFacebookAccountId(signInRequests.getFacebookAccountId());

            // Tạo người dùng mới nếu không tìm thấy
            if (optionalUser.isEmpty()) {
                // Generate unique username from email or Facebook account ID
                String username = signInRequests.getEmail() != null && !signInRequests.getEmail().isEmpty()
                        ? signInRequests.getEmail().split("@")[0] + "_" + System.currentTimeMillis()
                        : "facebook_" + signInRequests.getFacebookAccountId();
                
                UserAccount newUser = UserAccount.builder()
                        .fullName(Optional.ofNullable(signInRequests.getFullname()).orElse(""))
                        .username(username)
                        .email(Optional.ofNullable(signInRequests.getEmail()).orElse(""))
                        .avatar(Optional.ofNullable(signInRequests.getProfileImage()).orElse(""))
                        .role(roleUser)
                        .facebookAccountId(signInRequests.getFacebookAccountId())
                        .passwordHash("") // Mật khẩu trống cho đăng nhập mạng xã hội
                        .status(new Status(1)) // Active status
                        .createDate(LocalDate.now())
                        .priority(0)
                        .build();

                // Lưu người dùng mới
                newUser = userAccountRepository.save(newUser);
                optionalUser = Optional.of(newUser);
            }
        } else {
            throw new IllegalArgumentException("Invalid social account information.");
        }

        UserAccount user = optionalUser.get();

        // Kiểm tra nếu tài khoản bị khóa
        if (!user.getStatus().getName().equals("ACTIVE")) {
            throw new DataNotFoundException("User account is locked or inactive.");
        }

        // Tạo JWT token cho người dùng
        return jwtTokenUtil.generateAccessToken(user);
    }
}

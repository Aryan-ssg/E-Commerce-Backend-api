package com.example.Ecommerce.AppUser;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Ecommerce.AppUser.DTOs.request.ChangePasswordRequest;
import com.example.Ecommerce.AppUser.DTOs.request.RegisterRequest;
import com.example.Ecommerce.AppUser.DTOs.response.UserAdminResponse;
import com.example.Ecommerce.Common.AuthenticationHelper;
import com.example.Ecommerce.Common.DTOs.PagedResponse;
import com.example.Ecommerce.Common.Exceptions.UserAlreadyExistsException;
import com.example.Ecommerce.Common.RefreshTokenService;

@Service
public class AppUserServiceImpl implements AppUserService {
    private PasswordEncoder passwordEncoder;
    private AppUserRepository userRepository;
    private AuthenticationHelper authenticationHelper;
    private RefreshTokenService refreshTokenService;

    public AppUserServiceImpl(PasswordEncoder passwordEncoder, AppUserRepository userRepository,
            AuthenticationHelper authenticationHelper, RefreshTokenService refreshTokenService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.authenticationHelper = authenticationHelper;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public void registerUser(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException();
        }

        AppUser user = new AppUser();

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setUsername(request.getUsername());

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Lost a race against the unique constraint on username (pre-check passed
            // for both requests). Translates the 500 the catch-all would produce into 409.
            throw new UserAlreadyExistsException();
        }
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        AppUser currentUser = authenticationHelper.getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        currentUser.setTokenVersion(currentUser.getTokenVersion() + 1);
        userRepository.save(currentUser);

        refreshTokenService.revokeAllForUser(currentUser.getUserId());
    }

    @Override
    public PagedResponse<UserAdminResponse> getAllUsers(String username, Role role, Pageable pageable) {

        Specification<AppUser> spec = Specification.allOf();

        if (username != null && !username.isBlank()) {
            spec = spec.and(AppUserSpecifications.hasUsernameLike(username));
        }
        if (role != null) {
            spec = spec.and(AppUserSpecifications.hasRole(role));
        }

        Page<AppUser> userPage = userRepository.findAll(spec, pageable);

        List<UserAdminResponse> content = new ArrayList<>();
        for (AppUser user : userPage.getContent()) {
            content.add(new UserAdminResponse(
                    user.getUserId(),
                    user.getUsername(),
                    user.getRole().name()));
        }

        return new PagedResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isLast());
    }

}

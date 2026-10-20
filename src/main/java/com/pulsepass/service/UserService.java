package com.pulsepass.service;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UserService {

    UserResponse register(RegisterUserRequest request);

    /** El propio usuario, o un ADMIN. */
    @PreAuthorize("@authorizationService.canViewUserByEmail(#email, authentication)")
    UserResponse findByEmail(String email);

    /** El propio usuario, o un ADMIN. */
    @PreAuthorize("@authorizationService.canViewUserByUsername(#username, authentication)")
    UserResponse findByUsername(String username);
}
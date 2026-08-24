package com.example.Ecommerce.AppUser;

import org.springframework.data.domain.Pageable;

import com.example.Ecommerce.AppUser.DTOs.request.ChangePasswordRequest;
import com.example.Ecommerce.AppUser.DTOs.request.RegisterRequest;
import com.example.Ecommerce.AppUser.DTOs.response.UserAdminResponse;
import com.example.Ecommerce.Common.DTOs.PagedResponse;

public interface AppUserService {

    public void registerUser(RegisterRequest request);

    void changePassword(ChangePasswordRequest request);

    PagedResponse<UserAdminResponse> getAllUsers(String username, Role role, Pageable pageable);

}

package tn.esprit.pi5eme.service;

import tn.esprit.pi5eme.dto.ChangePasswordRequest;
import tn.esprit.pi5eme.dto.UserCreateRequest;
import tn.esprit.pi5eme.dto.UserResponse;
import tn.esprit.pi5eme.dto.UserUpdateRequest;

import java.util.List;

public interface IUserService {

    // Admin CRUD
    List<UserResponse> getAllUsers();

    UserResponse getUserById(Long id);

    UserResponse createUser(UserCreateRequest request);

    UserResponse updateUser(Long id, UserUpdateRequest request);

    void deleteUser(Long id, String currentAdminEmail);

    UserResponse toggleUserStatus(Long id, String currentAdminEmail);

    // Current User Self-Service
    UserResponse getCurrentUserProfile(String email);

    UserResponse updateCurrentUserProfile(String email, UserUpdateRequest request);

    void changePassword(String email, ChangePasswordRequest request);
}


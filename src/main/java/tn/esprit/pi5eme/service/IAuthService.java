package tn.esprit.pi5eme.service;

import tn.esprit.pi5eme.dto.*;

public interface IAuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    String forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}


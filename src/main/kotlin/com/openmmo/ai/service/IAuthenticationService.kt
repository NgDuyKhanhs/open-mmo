package com.openmmo.ai.service

import com.openmmo.ai.dto.AuthResponse
import com.openmmo.ai.dto.RegisterRequest
import com.openmmo.ai.dto.LoginRequest
import com.openmmo.ai.dto.GoogleLoginRequest
import com.openmmo.ai.dto.RefreshTokenRequest
import com.openmmo.ai.dto.ChangePasswordRequest
import com.openmmo.ai.dto.UserResponse
import com.openmmo.ai.entity.User

/**
 * Authentication Service Interface
 */
interface IAuthenticationService {
    fun register(request: RegisterRequest): AuthResponse
    fun login(request: LoginRequest): AuthResponse
    fun googleLogin(request: GoogleLoginRequest): AuthResponse
    fun refreshToken(request: RefreshTokenRequest): AuthResponse
    fun changePassword(userId: String, request: ChangePasswordRequest): AuthResponse
    fun getUserById(userId: String): UserResponse?
    fun getUserByEmail(email: String): UserResponse?
}


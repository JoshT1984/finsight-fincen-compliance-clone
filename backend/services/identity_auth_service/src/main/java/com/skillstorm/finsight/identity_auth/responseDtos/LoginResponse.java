package com.skillstorm.finsight.identity_auth.responseDtos;

public record LoginResponse(String accessToken, String userId, String refreshToken) {
}

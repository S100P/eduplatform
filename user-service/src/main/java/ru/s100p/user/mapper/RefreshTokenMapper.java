package ru.s100p.user.mapper;


import ru.s100p.user.dto.RefreshTokenDto;
import ru.s100p.user.entity.RefreshToken;

public final class RefreshTokenMapper {

    private RefreshTokenMapper() {}

    public static RefreshTokenDto toDto(RefreshToken token) {
        if (token == null) return null;
        return new RefreshTokenDto(
                token.getId(),
                token.getUser() != null ? token.getUser().getId() : null,
                token.getToken(),
                token.getExpiresAt(),
                token.getCreatedAt(),
                token.getIsRevoked()
        );
    }
}


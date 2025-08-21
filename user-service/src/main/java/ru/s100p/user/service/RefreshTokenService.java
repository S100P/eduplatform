package ru.s100p.user.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.s100p.user.dto.RefreshTokenDto;
import ru.s100p.user.entity.RefreshToken;
import ru.s100p.user.entity.User;
import ru.s100p.user.mapper.RefreshTokenMapper;
import ru.s100p.user.repository.RefreshTokenRepository;
import ru.s100p.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public RefreshTokenDto createToken(Long userId, long expiresInSeconds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusSeconds(expiresInSeconds));
        token.setIsRevoked(false);

        return RefreshTokenMapper.toDto(refreshTokenRepository.save(token));
    }

    @Transactional
    public void revokeToken(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new EntityNotFoundException("Token not found"));
        token.setIsRevoked(true);
        refreshTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public List<RefreshTokenDto> getActiveTokens(Long userId) {
        return refreshTokenRepository
                .findByUser_IdAndIsRevokedFalseAndExpiresAtAfter(userId, LocalDateTime.now())
                .stream()
                .map(RefreshTokenMapper::toDto)
                .toList();
    }
}


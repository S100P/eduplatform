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

    /**
     * Создает и сохраняет в базе данных новый refresh-токен для указанного пользователя.
     *
     * <p>Этот метод не создает JWT, а генерирует уникальную строку (UUID),
     * которая будет служить в качестве refresh-токена. Этот токен сохраняется в базе данных
     * с привязкой к пользователю, временем создания и временем истечения срока действия.
     * Он может быть использован в дальнейшем для получения новой пары access и refresh токенов.</p>
     *
     * @param userId ID пользователя, для которого создается токен.
     * @param expiresInSeconds Время жизни токена в секундах.
     * @return DTO созданного refresh-токена.
     * @throws EntityNotFoundException если пользователь с указанным ID не найден.
     */
    @Transactional
    public RefreshTokenDto createToken(Long userId, long expiresInSeconds) {
        // 1. Находим пользователя в базе данных по его ID.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        // 2. Создаем новый объект RefreshToken.
        RefreshToken token = new RefreshToken();
        // 3. Привязываем токен к найденному пользователю.
        token.setUser(user);
        // 4. Генерируем уникальное значение для токена с помощью UUID.
        token.setToken(UUID.randomUUID().toString());
        // 5. Устанавливаем текущее время как время создания токена.
        token.setCreatedAt(LocalDateTime.now());
        // 6. Вычисляем и устанавливаем время истечения срока действия токена.
        token.setExpiresAt(LocalDateTime.now().plusSeconds(expiresInSeconds));
        // 7. Устанавливаем флаг, что токен не отозван.
        token.setIsRevoked(false);

        // 8. Сохраняем токен в базе данных и преобразуем его в DTO для ответа.
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

    public RefreshToken validateAndGetToken(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new EntityNotFoundException("Token not found"));
    }
}

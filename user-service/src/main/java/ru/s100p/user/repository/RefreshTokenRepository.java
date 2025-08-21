package ru.s100p.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.s100p.user.entity.RefreshToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUser_Id(Long userId);

    List<RefreshToken> findByUser_IdAndIsRevokedFalseAndExpiresAtAfter(Long userId, LocalDateTime now);

    void deleteByUser_Id(Long userId);
}


package ru.s100p.user.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.s100p.shared.constants.ApiConstants;
import ru.s100p.user.dto.RefreshTokenDto;
import ru.s100p.user.service.RefreshTokenService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.API_V1 + ApiConstants.TOKENS_ENDPOINT)
@RequiredArgsConstructor
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;

    @PostMapping("/user/{userId}")
    public ResponseEntity<RefreshTokenDto> createToken(@PathVariable Long userId,
                                                       @RequestParam(defaultValue = "3600") long expiresIn) {
        RefreshTokenDto dto = refreshTokenService.createToken(userId, expiresIn);
        return ResponseEntity.created(URI.create(ApiConstants.API_V1 + ApiConstants.TOKENS_ENDPOINT + dto.id())).body(dto);
    }

    @PostMapping("/revoke/{token}")
    public ResponseEntity<Void> revokeToken(@PathVariable String token) {
        refreshTokenService.revokeToken(token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}/active")
    public List<RefreshTokenDto> getActiveTokens(@PathVariable Long userId) {
        return refreshTokenService.getActiveTokens(userId);
    }
}

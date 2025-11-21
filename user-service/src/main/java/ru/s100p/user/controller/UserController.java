package ru.s100p.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.s100p.shared.dto.UserDto;
import ru.s100p.user.service.UserService;

import java.util.List;

import static ru.s100p.shared.constants.ApiConstants.*;

@RestController
@RequestMapping(INTERNAL_API_PREFIX + API_V1_ADMIN + USERS_ENDPOINT)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping()
    public List<UserDto> listUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable("id") Long id, Authentication authentication) {
        Long requesterId = Long.valueOf(authentication.getName());
        return userService.getUserById(id, requesterId);
    }


    @PostMapping("/{id}/roles/{roleName}")
    public ResponseEntity<Void> assignRole(@PathVariable("id") Long id, @PathVariable("roleName") String roleName, Authentication authentication) {
        Long assignedBy = Long.valueOf(authentication.getName());
        userService.assignRole(id, roleName, assignedBy);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/roles/{roleName}")
    public ResponseEntity<Void> revokeRole(@PathVariable("id") Long id, @PathVariable("roleName") String roleName) {
        userService.revokeRole(id, roleName);
        return ResponseEntity.noContent().build();
    }
}

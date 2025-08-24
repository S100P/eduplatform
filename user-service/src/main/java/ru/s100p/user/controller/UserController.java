package ru.s100p.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.s100p.shared.constants.ApiConstants;
import ru.s100p.shared.dto.UserDto;
import ru.s100p.user.entity.User;
import ru.s100p.user.service.UserService;

import java.net.URI;
import java.util.List;

import static ru.s100p.shared.constants.ApiConstants.*;

@RestController
@RequestMapping(API_V1 + USERS_ENDPOINT)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserDto> listUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody User user) {
        UserDto dto = userService.createUser(user);
        return ResponseEntity.created(URI.create("/api/users/" + dto.id())).body(dto);
    }

    @PostMapping("/{id}/roles/{roleName}")
    public ResponseEntity<Void> assignRole(@PathVariable Long id, @PathVariable String roleName) {
        userService.assignRole(id, roleName);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/roles/{roleName}")
    public ResponseEntity<Void> revokeRole(@PathVariable Long id, @PathVariable String roleName) {
        userService.revokeRole(id, roleName);
        return ResponseEntity.noContent().build();
    }
}


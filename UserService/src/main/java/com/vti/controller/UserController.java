package com.vti.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

import com.vti.dto.UserDto;
import com.vti.form.UserFormUpdate;
import com.vti.service.IUserService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private IUserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @RequestHeader(value = "X-User-Role", required = false) String currentUserRole) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUserRole);
        boolean isSelf = currentUserId != null && currentUserId.equals(id);
        if (!isAdmin && !isSelf) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem thông tin người dùng này");
        }
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAll() {
        return ResponseEntity.ok(userService.getAllUser());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> update(
            @PathVariable Long id,
            @RequestBody @Valid UserFormUpdate form,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @RequestHeader(value = "X-User-Role", required = false) String currentUserRole) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUserRole);
        boolean isSelf = currentUserId != null && currentUserId.equals(id);
        if (!isAdmin && !isSelf) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền cập nhật thông tin người dùng này");
        }
        return ResponseEntity.ok(userService.updateUser(id, form));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> changeStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(userService.changeUserStatus(id, status));
    }
}
package com.company.erp.users;

import com.company.erp.common.dto.PageResponse;
import com.company.erp.permissions.PermissionCatalog;
import com.company.erp.users.dto.CreateUserRequest;
import com.company.erp.users.dto.UpdateUserRequest;
import com.company.erp.users.dto.UserResponse;
import com.company.erp.users.entity.UserStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;
import java.util.function.Function;

/** Tenant-scoped user administration. All endpoints are permission-gated. */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Tenant user administration")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCatalog.USER_READ + "')")
    @Operation(summary = "List / search users in the current tenant")
    public PageResponse<UserResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(userService.list(search, status, pageable), Function.identity());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCatalog.USER_READ + "')")
    @Operation(summary = "Get a single user by id")
    public UserResponse get(@PathVariable UUID id) {
        return UserResponse.from(userService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCatalog.USER_CREATE + "')")
    @Operation(summary = "Create a user")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request,
                                               UriComponentsBuilder uriBuilder) {
        UserResponse body = UserResponse.from(userService.create(request));
        URI location = uriBuilder.path("/api/v1/users/{id}").buildAndExpand(body.id()).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCatalog.USER_UPDATE + "')")
    @Operation(summary = "Update a user's profile, status and/or role assignment")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + PermissionCatalog.USER_DELETE + "')")
    @Operation(summary = "Delete a user")
    public void delete(@PathVariable UUID id) {
        userService.delete(id);
    }
}

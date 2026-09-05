package com.miresta.auth;

import java.util.List;

public interface IUserService {
    UserResponse createUser(CreateUserRequest request, String currentUserEmail);

    List<UserResponse> getUsers();

    UserResponse updateUser(Long id, UpdateUserRequest request, String currentUserEmail);

    void deleteUser(Long id, String currentUserEmail);
}

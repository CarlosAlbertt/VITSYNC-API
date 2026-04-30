package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.dto.UserUpdateRequest;
import com.ejemplo.vitsync.model.User;

import java.util.List;
import java.util.Map;


public interface IUserService {

    List<User> findAll();

    User findById(Long id);

    void saveUser(User user);

    void deleteUser(User id);

    void suspendUser(Long id);

    Map<String, Object> exportUserData(Long id);

    User updateProfile(Long id, UserUpdateRequest request);

    void changePassword(Long id, String currentPassword, String newPassword);

    void setTwoFactorEnabled(Long id, boolean enabled);

    void saveSecurityQuestions(Long id, Map<String, String> questions);
}
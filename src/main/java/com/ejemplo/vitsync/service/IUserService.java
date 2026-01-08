package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.model.User;

import java.util.List;


public interface IUserService { //INTERFAZ QUE DEFINE LAS FUNCIONALIDADES DEL SERVICIO DE USUARIO

    List<User> findAll();

    User findById(Long id);

    void saveUser(User user);

    void deleteUser(User id);
}
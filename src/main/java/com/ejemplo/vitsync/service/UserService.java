package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.dto.UserUpdateRequest;
import com.ejemplo.vitsync.model.Paciente;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.CitaRepository;
import com.ejemplo.vitsync.repository.InformeRepository;
import com.ejemplo.vitsync.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CitaRepository citaRepository;
    private final InformeRepository informeRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, CitaRepository citaRepository, InformeRepository informeRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.citaRepository = citaRepository;
        this.informeRepository = informeRepository;
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public void saveUser(User user) {
        userRepository.save(user);
    }

    @Override
    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    @Override
    public void suspendUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        user.setSuspended(true);
        userRepository.save(user);
    }

    @Override
    public Map<String, Object> exportUserData(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        
        Map<String, Object> data = new HashMap<>();
        data.put("profile", user);
        data.put("appointments", citaRepository.findByPacienteId(id));
        data.put("reports", informeRepository.findByPacienteId(id));
        
        return data;
    }

    @Override
    public User updateProfile(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        user.setName(request.getName());
        user.setFirstName(request.getFirstName());
        user.setSecondName(request.getSecondName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setPostCode(request.getPostCode());
        user.setCountry(request.getCountry());
        user.setGender(request.getGender());
        user.setBirthDate(request.getBirthDate());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (user instanceof Paciente paciente) {
            paciente.setGrupoSanguineo(request.getBloodType());
            paciente.setAlergias(request.getAllergies());
            paciente.setCondicionesPrevias(request.getMedicalConditions());
            paciente.setContactoEmergencia(request.getEmergencyContact());
        }

        return userRepository.save(user);
    }

    @Override
    public void changePassword(Long id, String currentPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void setTwoFactorEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        user.setTwoFactorEnabled(enabled);
        userRepository.save(user);
    }

    @Override
    public void saveSecurityQuestions(Long id, Map<String, String> questions) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        
        user.setSecurityQuestion1(questions.get("q1"));
        user.setSecurityAnswer1(questions.get("a1"));
        user.setSecurityQuestion2(questions.get("q2"));
        user.setSecurityAnswer2(questions.get("a2"));
        
        userRepository.save(user);
    }
}

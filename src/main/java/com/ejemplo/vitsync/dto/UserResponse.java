package com.ejemplo.vitsync.dto;

import com.ejemplo.vitsync.enums.Gender;
import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String firstName;
    private String secondName;
    private String nif;
    private String email;
    private Gender gender;
    private Role role;
    private LocalDate birthDate;
    private String phone;
    private String address;
    private String postCode;
    private String country;
    // Sin esto, el getter isVerified() se serializa como "verified" y el front
    // (que lee isVerified) nunca veía el estado real de verificación.
    @JsonProperty("isVerified")
    private boolean isVerified;

    // Método factory para convertir entidad a DTO (sin exponer password)
    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .firstName(user.getFirstName())
                .secondName(user.getSecondName())
                .nif(user.getNif())
                .email(user.getEmail())
                .gender(user.getGender())
                .role(user.getRole())
                .birthDate(user.getBirthDate())
                .phone(user.getPhone())
                .address(user.getAddress())
                .postCode(user.getPostCode())
                .country(user.getCountry())
                .isVerified(user.isVerified())
                .build();
    }
}

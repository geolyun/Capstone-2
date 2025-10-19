package com.capstone.Capstone_2.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpDto {

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 8)
    private String password;

    @NotBlank
    private String passwordConfirm;

    @NotBlank
    private String nickname;

    @AssertTrue(message = "서비스 이용 약관에 동의해야 합니다.")
    private boolean termsAgreed;

    @AssertTrue(message = "개인정보 처리 방침에 동의해야 합니다.")
    private boolean privacyAgreed;
}
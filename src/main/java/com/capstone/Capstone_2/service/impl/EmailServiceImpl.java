package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.service.EmailService;
import jakarta.mail.MessagingException; // ✅ 추가
import jakarta.mail.internet.MimeMessage; // ✅ 추가
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper; // ✅ 추가
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendVerificationCode(User user, String code) {
        try {
            // ✅ SimpleMailMessage 대신 MimeMessage 사용
            MimeMessage message = mailSender.createMimeMessage();
            // MimeMessageHelper: 파일 첨부, HTML 내용 등을 쉽게 처리하게 해주는 도우미
            // 두 번째 파라미터 true는 멀티파트(이미지 등 첨부 가능) 모드 활성화, 'utf-8'은 인코딩 설정
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");

            helper.setTo(user.getEmail());
            helper.setSubject("[링코] 회원가입 인증번호 안내");

            // ✅ HTML 디자인 적용
            String htmlContent = getVerificationHtml(code);
            helper.setText(htmlContent, true); // true: HTML 모드 활성화

            mailSender.send(message);

        } catch (MessagingException e) {
            // 이메일 발송 실패 로그
            System.err.println("이메일 발송 실패: " + e.getMessage());
            // 필요 시 예외를 다시 던져서 컨트롤러가 알 수 있게 처리 가능
        }
    }

    // ✅ HTML 템플릿 생성 메서드
    private String getVerificationHtml(String code) {
        return "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px;'>" +
                "  <div style='background-color: #ffffff; max-width: 600px; margin: 0 auto; padding: 40px; border-radius: 10px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); text-align: center;'>" +
                "    <h1 style='color: #333333; margin-bottom: 20px;'>링코(Ringco) 회원가입 인증</h1>" +
                "    <p style='color: #666666; font-size: 16px; line-height: 1.5; margin-bottom: 30px;'>" +
                "      안녕하세요! 링코 서비스 가입을 환영합니다.<br>" +
                "      아래의 <strong>인증 번호 6자리</strong>를 입력하여 가입을 완료해주세요." +
                "    </p>" +
                "    <div style='background-color: #eef2f5; padding: 15px; border-radius: 5px; display: inline-block; margin-bottom: 30px;'>" +
                "      <span style='font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #007bff;'>" + code + "</span>" +
                "    </div>" +
                "    <p style='color: #999999; font-size: 14px;'>" +
                "      * 이 인증 번호는 <strong>5분간</strong> 유효합니다.<br>" +
                "      * 본인이 요청하지 않은 경우 이 메일을 무시하셔도 됩니다." +
                "    </p>" +
                "    <div style='margin-top: 40px; border-top: 1px solid #eeeeee; padding-top: 20px; color: #aaaaaa; font-size: 12px;'>" +
                "      &copy; 2025 Ringco Team. All rights reserved." +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }
}
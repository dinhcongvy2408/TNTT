package vn.tntt.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chốt chặn cho lỗi đã mô tả ở docs/99 mục E2.
 *
 * <p>Nếu ai đó lỡ xoá {@code handleAccessDenied} khỏi
 * {@link GlobalExceptionHandler}, exception sẽ rơi vào lưới
 * {@code Exception.class} và thành 500. Test này đỏ ngay lúc đó.
 *
 * <p><b>Phạm vi test.</b> Nó kiểm chứng việc ÁNH XẠ exception → 403, tức là
 * đúng chỗ đã hỏng. Nó KHÔNG kiểm chứng {@code @PreAuthorize} thật sự ném ra
 * exception — muốn thế phải dựng cả Spring context kèm database, mà CI thì
 * không có PostgreSQL. Phần đó để Sprint 1 làm bằng Testcontainers.
 *
 * <p>Dùng {@code standaloneSetup} như {@code HealthControllerTest}: không
 * khởi động ứng dụng, chạy trong vài chục mili-giây.
 */
class GlobalExceptionHandlerAccessDeniedTest {

    /** Controller giả, chỉ tồn tại để ném ra đúng loại exception cần test. */
    @RestController
    static class ControllerGia {

        @GetMapping("/gia/access-denied")
        public String accessDenied() {
            throw new AccessDeniedException("Access is denied");
        }

        /**
         * Đây mới là thứ {@code @PreAuthorize} ném ra ở Spring Security 6.
         * Test riêng để chắc chắn handler bắt được cả lớp con, chứ không chỉ
         * lớp cha.
         */
        @GetMapping("/gia/authorization-denied")
        public String authorizationDenied() {
            AuthorizationResult ketQua = new AuthorizationDecision(false);
            throw new AuthorizationDeniedException("Access Denied", ketQua);
        }
    }

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ControllerGia())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    @DisplayName("AccessDeniedException trả 403 kèm mã ACCESS_DENIED, KHÔNG phải 500")
    void accessDeniedTra403() throws Exception {
        mockMvc.perform(get("/gia/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                // Message phải là câu chung cho người dùng, KHÔNG được lộ
                // biểu thức quyền đã thất bại.
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này"));
    }

    @Test
    @DisplayName("AuthorizationDeniedException của @PreAuthorize cũng trả 403")
    void authorizationDeniedTra403() throws Exception {
        mockMvc.perform(get("/gia/authorization-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }
}

package vn.tntt.common.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.tntt.common.exception.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test cho HealthController.
 *
 * <p>Dùng {@code MockMvcBuilders.standaloneSetup} thay vì
 * {@code @SpringBootTest}: cách này KHÔNG khởi động cả ứng dụng, không cần
 * database, chạy trong vài chục mili-giây thay vì vài giây. Ta chỉ muốn
 * kiểm tra một controller nên không có lý do gì phải dựng cả context.
 *
 * <p>Test tích hợp thật (có DB) sẽ dùng Testcontainers — xem docs/05 phần
 * "Sau khi lên production".
 */
class HealthControllerTest {

    private MockMvc mockMvc;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        HealthController controller = new HealthController(jdbcTemplate);

        // Hai field này bình thường do @Value điền. Ở standalone setup không
        // có Spring context nên phải gán tay.
        ReflectionTestUtils.setField(controller, "tenUngDung", "tntt-backend");
        ReflectionTestUtils.setField(controller, "profile", "test");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("DB sống thì trả success=true và database=UP")
    void databaseSong() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trangThai").value("UP"))
                .andExpect(jsonPath("$.data.database").value("UP"))
                .andExpect(jsonPath("$.data.profile").value("test"));
    }

    @Test
    @DisplayName("DB chết thì vẫn trả 200, nhưng database=DOWN")
    void databaseChet() throws Exception {
        // Đây là phần đáng test nhất: endpoint health phải SỐNG SÓT khi DB
        // chết, để còn báo được là DB chết. Nếu nó cũng đổ 500 thì người
        // trực hệ thống không phân biệt được "app sập" với "DB sập".
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenThrow(new RuntimeException("connection refused"));

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trangThai").value("UP"))
                .andExpect(jsonPath("$.data.database").value("DOWN"));
    }
}

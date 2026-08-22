package vn.tntt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động của ứng dụng.
 *
 * <p>{@code @SpringBootApplication} là gộp của 3 annotation:
 * <ul>
 *   <li>{@code @Configuration} — lớp này có thể khai báo bean</li>
 *   <li>{@code @EnableAutoConfiguration} — Spring tự cấu hình dựa trên thư viện
 *       nào có trong classpath (thấy postgresql driver → tự tạo DataSource...)</li>
 *   <li>{@code @ComponentScan} — quét mọi {@code @Component/@Service/@RestController}
 *       trong package {@code vn.tntt} và các package con</li>
 * </ul>
 * Vì lớp này nằm ở {@code vn.tntt}, mọi module bên dưới đều được quét tự động.
 */
@SpringBootApplication
public class TnttApplication {

    public static void main(String[] args) {
        SpringApplication.run(TnttApplication.class, args);
    }
}

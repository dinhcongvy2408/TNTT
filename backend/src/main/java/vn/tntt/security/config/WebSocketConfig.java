package vn.tntt.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import vn.tntt.common.config.AppProperties;
import vn.tntt.security.service.JwtService;
import vn.tntt.security.service.NguoiDungDangDangNhap;

import java.util.List;

/**
 * WebSocket + STOMP cho màn hình trực cổng.
 *
 * <p><b>Vì sao cần STOMP chứ không dùng WebSocket trần?</b> WebSocket trần chỉ
 * cho ta một ống truyền byte hai chiều — muốn có khái niệm "đăng ký nhận tin
 * của topic X" thì phải tự nghĩ ra giao thức, tự quản danh sách ai đăng ký gì.
 * STOMP là giao thức đã có sẵn cho đúng việc đó, và Spring cài sẵn một broker
 * trong bộ nhớ.
 *
 * <p><b>Broker trong bộ nhớ có đủ không?</b> Đủ ở quy mô này: một tiến trình
 * backend, vài chục màn hình trực. Nếu Sprint 8 chạy nhiều instance thì bản
 * tin đẩy ở instance A sẽ không tới màn hình đang nối vào instance B — lúc đó
 * mới cần một broker ngoài (RabbitMQ). Ghi ở docs/99 mục H5.
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final AppProperties appProperties;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // WebSocket KHÔNG chịu ràng buộc CORS của trình duyệt như
                // fetch/XHR, nên phải tự khai origin được phép ở đây. Bỏ trống
                // là mở cho mọi website nối vào.
                .setAllowedOrigins(appProperties.corsAllowedOrigins().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Client đăng ký nhận tin ở /topic/...
        registry.enableSimpleBroker("/topic");
        // Client gửi lên server thì tiền tố /app. Hiện chưa dùng: mọi thao tác
        // ghi đều đi qua REST để dùng chung validation, phân quyền và
        // transaction. WebSocket ở đây CHỈ một chiều server -> client.
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Xác thực JWT ở khung CONNECT của STOMP — docs/04 quy định đúng như vậy.
     *
     * <p><b>Vì sao không dùng lại {@code JwtAuthenticationFilter}?</b> Filter
     * đó là filter của servlet, nó chỉ chạy cho request HTTP. Sau khi bắt tay
     * xong, WebSocket không còn là HTTP nữa — mọi khung tin đi qua kênh của
     * Spring Messaging, hoàn toàn ngoài tầm với của filter chain.
     *
     * <p>Trình duyệt cũng không cho đặt header tuỳ ý khi mở WebSocket, nên
     * token được gửi trong header của khung CONNECT chứ không phải header HTTP.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor
                        .getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
                    return message;
                }

                String header = accessor.getFirstNativeHeader("Authorization");
                if (header == null || !header.startsWith("Bearer ")) {
                    // Ném exception ở đây làm Spring từ chối CONNECT, client
                    // nhận ERROR frame. Cho qua thì bất kỳ ai cũng nghe được
                    // tên và lớp của các em qua topic phiếu ra cổng.
                    throw new IllegalArgumentException("Thiếu token khi mở WebSocket");
                }

                var claims = jwtService.docToken(header.substring(7))
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Token không hợp lệ khi mở WebSocket"));

                @SuppressWarnings("unchecked")
                List<String> maVaiTro = claims.get(JwtService.CLAIM_VAI_TRO, List.class);
                var quyen = maVaiTro == null ? List.<SimpleGrantedAuthority>of()
                        : maVaiTro.stream()
                                .map(ma -> new SimpleGrantedAuthority("ROLE_" + ma)).toList();

                var nguoiDung = new NguoiDungDangDangNhap(
                        jwtService.layNguoiDungId(claims),
                        claims.get(JwtService.CLAIM_HO_TEN, String.class),
                        maVaiTro == null ? List.of() : maVaiTro,
                        Boolean.TRUE.equals(
                                claims.get(JwtService.CLAIM_CAN_DOI_MAT_KHAU, Boolean.class)));

                accessor.setUser(new UsernamePasswordAuthenticationToken(
                        nguoiDung, null, quyen));
                log.debug("WebSocket CONNECT: {}", nguoiDung.id());
                return message;
            }
        });
    }
}

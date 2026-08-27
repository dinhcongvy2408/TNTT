package vn.tntt.discipline.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import vn.tntt.discipline.dto.PhieuRaCongResponse;

import java.util.UUID;

/**
 * Đẩy bản tin phiếu ra cổng tới mọi màn hình đang mở.
 *
 * <p>Topic: {@code /topic/phieu-ra-cong/{namHocId}} — đúng như docs/04. Chia
 * topic theo năm học nghĩa là nếu sau này nhiều xứ đoàn dùng chung hệ thống,
 * màn hình trực của xứ này không nhận tin của xứ kia.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PhieuRaCongPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * @param loai PHIEU_MOI | DA_XAC_NHAN | DA_HUY — khớp docs/04
     */
    public void day(UUID namHocId, String loai, PhieuRaCongResponse phieu) {
        String topic = "/topic/phieu-ra-cong/" + namHocId;
        try {
            messagingTemplate.convertAndSend(topic, new BanTin(loai, phieu));
            log.debug("Đã đẩy {} tới {}", loai, topic);
        } catch (Exception ex) {
            // NUỐT lỗi có chủ đích. Bản tin realtime là tiện ích, không phải
            // nguồn sự thật: dữ liệu đã nằm an toàn trong DB rồi. Broker trục
            // trặc mà làm hỏng cả thao tác tạo phiếu thì đó là đánh đổi sai.
            // Màn hình trực còn cơ chế polling dự phòng 10 giây một lần.
            log.error("Không đẩy được bản tin {} tới {}", loai, topic, ex);
        }
    }

    /** Khớp payload ở docs/04 mục WebSocket. */
    public record BanTin(String type, PhieuRaCongResponse phieu) {
    }
}

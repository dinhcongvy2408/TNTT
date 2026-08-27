import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import type { BanTinPhieu } from '@/services/phieuRaCongService'
import { layAccessToken } from '@/services/http'

/**
 * Nối WebSocket để nhận bản tin phiếu ra cổng theo thời gian thực.
 *
 * <b>Vì sao vẫn cần polling dự phòng?</b> Mạng ở sân nhà thờ chập chờn, và
 * WebSocket là kết nối GIỮ MỞ — nó đứt thường xuyên hơn nhiều so với một
 * request HTTP ngắn. Nếu chỉ dựa vào socket thì có lúc màn hình trực cổng
 * đứng im hàng phút mà không ai biết, trong khi phụ huynh đang đứng chờ ngoài
 * cổng. docs/05 Sprint 7 ghi rõ yêu cầu này.
 *
 * Hook trả về trạng thái kết nối để màn hình tự quyết định có bật polling hay
 * không — nó không tự gọi API, vì việc tải danh sách là của TanStack Query.
 */
export type TrangThaiSocket = 'dang-noi' | 'da-noi' | 'mat-ket-noi'

export function useWebSocketPhieu(
  namHocId: string | null,
  khiCoBanTin: (banTin: BanTinPhieu) => void,
): TrangThaiSocket {
  const [trangThai, datTrangThai] = useState<TrangThaiSocket>('dang-noi')

  // Giữ callback trong ref để effect KHÔNG phụ thuộc vào nó. Không làm vậy
  // thì mỗi lần component render lại (mà nó render liên tục khi danh sách
  // đổi) là socket bị ngắt rồi nối lại — đúng lúc đang bận nhất.
  const refCallback = useRef(khiCoBanTin)
  useEffect(() => {
    // Gán trong effect chứ không gán thẳng lúc render: ghi vào ref giữa lúc
    // render là hành vi không xác định trong chế độ đồng thời của React 19.
    refCallback.current = khiCoBanTin
  }, [khiCoBanTin])

  useEffect(() => {
    if (!namHocId) return

    const token = layAccessToken()
    if (!token) return

    const client = new Client({
      // Dùng WebSocket gốc, không qua SockJS. SockJS chỉ cần cho trình duyệt
      // quá cũ không hỗ trợ WebSocket; mọi điện thoại còn dùng được năm 2026
      // đều hỗ trợ, nên thêm SockJS chỉ là thêm một thư viện và một lớp giả lập.
      brokerURL: `${viTriWs()}/ws`,

      // Trình duyệt không cho đặt header HTTP tuỳ ý khi mở WebSocket, nên
      // token đi trong header của khung STOMP CONNECT — xem WebSocketConfig
      // bên backend.
      connectHeaders: { Authorization: `Bearer ${token}` },

      // Tự nối lại sau 5 giây. Trong lúc đứt thì polling gánh.
      reconnectDelay: 5000,

      // Nhịp tim: phát hiện kết nối chết mà chưa kịp báo đứt — chuyện thường
      // gặp khi điện thoại chuyển từ wifi sang 4G.
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      debug: () => {},
    })

    client.onConnect = () => {
      datTrangThai('da-noi')
      client.subscribe(`/topic/phieu-ra-cong/${namHocId}`, (message) => {
        try {
          refCallback.current(JSON.parse(message.body) as BanTinPhieu)
        } catch {
          // Bản tin hỏng thì bỏ qua, KHÔNG để nó làm sập cả màn hình trực.
          // Danh sách vẫn đúng nhờ lần polling kế tiếp.
        }
      })
    }

    client.onWebSocketClose = () => datTrangThai('mat-ket-noi')
    client.onStompError = () => datTrangThai('mat-ket-noi')

    client.activate()
    return () => {
      void client.deactivate()
    }
  }, [namHocId])

  return trangThai
}

/**
 * Đổi http(s) của trang hiện tại thành ws(s).
 *
 * Không hardcode localhost:8080 vì ở dev, Vite proxy làm frontend và API cùng
 * origin; lên production thì origin lại là tên miền thật. Lấy từ
 * `window.location` là đúng ở cả hai nơi.
 */
function viTriWs(): string {
  const giaoThuc = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${giaoThuc}//${window.location.host}`
}

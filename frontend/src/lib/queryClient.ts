import { QueryClient } from '@tanstack/react-query'

/**
 * Cấu hình TanStack Query dùng chung.
 *
 * Các mặc định dưới đây được chọn cho hoàn cảnh cụ thể của dự án:
 * huynh trưởng dùng điện thoại, mạng 3G/4G chập chờn trong khuôn viên
 * nhà thờ, và mỗi Chủ Nhật có 150 người dùng cùng lúc.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Dữ liệu coi là còn "tươi" trong 1 phút → không gọi lại API khi
      // người dùng chuyển qua chuyển lại giữa các màn hình. Tiết kiệm
      // băng thông đáng kể trên mạng di động.
      staleTime: 60_000,

      // Thử lại 1 lần khi lỗi. Mặc định của thư viện là 3, quá nhiều:
      // mạng yếu thì 3 lần chờ khiến người dùng tưởng app treo.
      retry: 1,

      // Tắt refetch khi chuyển tab. Mặc định là true, nhưng trên điện
      // thoại việc "quay lại app" xảy ra liên tục → gọi API liên tục.
      refetchOnWindowFocus: false,

      // Gọi lại NGAY khi máy có mạng trở lại — đúng thứ ta cần khi sóng
      // vừa chập chờn trong nhà thờ.
      refetchOnReconnect: true,
    },
    mutations: {
      // KHÔNG tự retry mutation. Query (đọc) retry được vì gọi lại vô hại;
      // mutation (ghi) mà tự gọi lại có thể tạo hai phiếu ra cổng cho cùng
      // một em. Sprint 5 và 7 sẽ dùng thiết kế idempotent để xử lý đúng.
      retry: 0,
    },
  },
})

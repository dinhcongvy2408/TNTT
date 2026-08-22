import { http } from './http'

/**
 * Dữ liệu do GET /api/v1/health trả về.
 * Khớp với record HealthInfo trong HealthController.java.
 */
export interface HealthInfo {
  ungDung: string
  profile: string
  trangThai: string
  database: string
  thoiGianMayChu: string
}

/**
 * Mỗi module nghiệp vụ sẽ có một file service như thế này:
 * namHocService.ts, thieuNhiService.ts, diemDanhService.ts...
 *
 * Quy ước: service CHỈ gọi API và trả dữ liệu. Không đụng tới React,
 * không giữ state, không hiện thông báo. Nhờ vậy có thể gọi nó từ bất kỳ
 * đâu và test được mà không cần render component nào.
 */
export const healthService = {
  kiemTra: () => http.get<HealthInfo>('/api/v1/health'),
}

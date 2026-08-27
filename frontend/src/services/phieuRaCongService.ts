import { http } from './http'

export type TrangThaiPhieu = 'CHO_RA_CONG' | 'DA_RA_CONG' | 'HUY'

/**
 * Một phiếu ra cổng.
 *
 * Khớp record `PhieuRaCongResponse` bên Java, và cũng chính là payload đẩy
 * qua WebSocket — cố ý dùng chung một hình dạng cho cả hai đường, để frontend
 * chỉ có một bộ code xử lý.
 */
export interface PhieuRaCong {
  id: string
  thieuNhiId: string
  maThieuNhi: string
  tenThanh: string | null
  hoTen: string
  /** null khi em chưa được ghi danh vào lớp nào — giao diện hiện "chưa có lớp". */
  tenLop: string | null
  lyDo: string
  nguoiTao: string
  nguoiXacNhan: string | null
  thoiGianTao: string
  thoiGianRaCong: string | null
  trangThai: TrangThaiPhieu
}

/** Bản tin đẩy qua WebSocket — khớp docs/04 mục WebSocket. */
export interface BanTinPhieu {
  type: 'PHIEU_MOI' | 'DA_XAC_NHAN' | 'DA_HUY'
  phieu: PhieuRaCong
}

const GOC = '/api/v1/phieu-ra-cong'

export const phieuRaCongService = {
  /** Giáo lý viên xin cho một em về sớm. */
  tao: (thieuNhiId: string, lyDo: string) =>
    http.post<PhieuRaCong>(GOC, { thieuNhiId, lyDo }),

  /**
   * Danh sách đang chờ.
   *
   * Màn hình trực cổng gọi hàm này lúc mở, VÀ gọi lại mỗi 10 giây khi
   * WebSocket đứt. Đó là lý do nó phải rẻ và trả về trạng thái đầy đủ chứ
   * không phải chỉ phần thay đổi.
   */
  dangCho: () => http.get<PhieuRaCong[]>(`${GOC}/dang-cho`),

  xacNhan: (id: string) => http.patch<PhieuRaCong>(`${GOC}/${id}/xac-nhan`),

  huy: (id: string) => http.patch<PhieuRaCong>(`${GOC}/${id}/huy`),

  lichSu: (ngay?: string) => http.get<PhieuRaCong[]>(`${GOC}/lich-su`, ngay ? { ngay } : undefined),
}

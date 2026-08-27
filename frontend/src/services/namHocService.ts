import { http } from './http'

/**
 * Trạng thái năm học. Ba giá trị khớp đúng enum TrangThaiNamHoc bên Java và
 * ràng buộc CHECK trong migration V1.
 *
 * Vì sao dùng union kiểu chuỗi chứ không dùng `enum` của TypeScript?
 * `enum` sinh ra một object có thật lúc chạy, tức là thêm code vào bundle.
 * Union chuỗi bị xoá sạch khi biên dịch mà vẫn kiểm tra kiểu chặt y hệt —
 * gõ 'DANG_HOAT_DONg' là TypeScript báo lỗi ngay.
 */
export type TrangThaiNamHoc = 'CHUAN_BI' | 'DANG_HOAT_DONG' | 'DA_KET_THUC'

/** Khớp với record NamHocResponse bên Java. */
export interface NamHoc {
  id: string
  tenNamHoc: string
  /** Dạng ISO "2026-09-01". Giữ nguyên chuỗi, không đổi sang Date ở đây. */
  ngayBatDau: string
  ngayKetThuc: string
  trangThai: TrangThaiNamHoc
}

export interface TaoNamHocRequest {
  tenNamHoc: string
  ngayBatDau: string
  ngayKetThuc: string
}

/** Nhãn tiếng Việt để hiển thị. Tách khỏi component để mọi màn hình dùng chung. */
export const NHAN_TRANG_THAI: Record<TrangThaiNamHoc, string> = {
  CHUAN_BI: 'Chuẩn bị',
  DANG_HOAT_DONG: 'Đang hoạt động',
  DA_KET_THUC: 'Đã kết thúc',
}

const GOC = '/api/v1/nam-hoc'

export const namHocService = {
  layTatCa: () => http.get<NamHoc[]>(GOC),

  /**
   * Trả về null khi chưa kích hoạt năm học nào — dùng `getNullable` chứ
   * không phải `get`, vì `get` coi data rỗng là vi phạm hợp đồng API.
   */
  layHienTai: () => http.getNullable<NamHoc>(`${GOC}/hien-tai`),

  tao: (body: TaoNamHocRequest) => http.post<NamHoc>(GOC, body),

  kichHoat: (id: string) => http.patch<NamHoc>(`${GOC}/${id}/kich-hoat`),

  ketThuc: (id: string) => http.patch<NamHoc>(`${GOC}/${id}/ket-thuc`),
}

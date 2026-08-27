import { http, httpVoid } from './http'
import type { PageResponse } from './types'

export interface ThieuNhi {
  id: string
  maThieuNhi: string
  tenThanh: string | null
  hoTen: string
  tenDayDu: string
  ngaySinh: string
  /** Tính sẵn ở server để mọi máy cho ra cùng một con số. */
  tuoi: number
  gioiTinh: 'NAM' | 'NU' | null
  tenBo: string | null
  tenMe: string | null
  sdtPhuHuynh: string | null
  diaChi: string | null
  giaoHo: string | null
  ghiChu: string | null
}

export interface LuuThieuNhiRequest {
  tenThanh?: string
  hoTen: string
  ngaySinh: string
  gioiTinh?: string
  tenBo?: string
  tenMe?: string
  sdtPhuHuynh?: string
  diaChi?: string
  giaoHo?: string
  ghiChu?: string
}

export type TrangThaiGhiDanh = 'DANG_HOC' | 'CHUYEN_XU' | 'NGHI_HOC' | 'HOAN_THANH'

export interface GhiDanh {
  id: string
  thieuNhiId: string
  maThieuNhi: string
  tenThieuNhi: string
  lopId: string
  tenLop: string
  namHocId: string
  trangThai: TrangThaiGhiDanh
  ngayGhiDanh: string
}

const GOC = '/api/v1/thieu-nhi'

export const thieuNhiService = {
  /**
   * Danh sách có phân trang.
   *
   * Tìm kiếm hiện chỉ khớp chữ CÓ DẤU đúng như đã gõ, chưa bỏ dấu — gõ "bich"
   * không ra "Bích". Tìm không dấu cần chỉ mục GIN và truy vấn native khớp
   * chính xác biểu thức của index, thuộc phần Sprint 4 đầy đủ.
   */
  tim: (tuKhoa: string, page = 0, size = 20) =>
    http.get<PageResponse<ThieuNhi>>(GOC, { tuKhoa, page, size }),

  xem: (id: string) => http.get<ThieuNhi>(`${GOC}/${id}`),

  tao: (body: LuuThieuNhiRequest) => http.post<ThieuNhi>(GOC, body),

  sua: (id: string, body: LuuThieuNhiRequest) => http.put<ThieuNhi>(`${GOC}/${id}`, body),

  /** Xoá MỀM: hồ sơ vẫn còn trong DB, chỉ ẩn khỏi danh sách. */
  xoa: (id: string) => httpVoid.del(`${GOC}/${id}`),
}

export const ghiDanhService = {
  danhSachLop: (lopId: string) => http.get<GhiDanh[]>('/api/v1/ghi-danh', { lopId }),

  ghiDanh: (thieuNhiId: string, lopId: string) =>
    http.post<GhiDanh>('/api/v1/ghi-danh', { thieuNhiId, lopId }),
}

import { http, httpVoid } from './http'

/** Khớp với record NganhResponse bên Java. */
export interface Nganh {
  id: string
  tenNganh: string
  maNganh: string
  tuoiToiThieu: number
  tuoiToiDa: number
  /** Thứ tự chuyển cấp: Chiên Con = 1 ... Hiệp Sĩ = 5. */
  thuTu: number
}

/** Khớp với record LopHocResponse bên Java. */
export interface LopHoc {
  id: string
  tenLop: string
  capDo: number
  ghiChu: string | null
  nganhId: string
  /** Backend trả kèm tên để frontend khỏi phải gọi thêm API rồi tự ghép. */
  tenNganh: string
  namHocId: string
  tenNamHoc: string
}

export interface LuuLopHocRequest {
  tenLop: string
  nganhId: string
  namHocId: string
  capDo: number
  ghiChu?: string | null
}

export const nganhService = {
  layTatCa: () => http.get<Nganh[]>('/api/v1/nganh'),
}

const GOC = '/api/v1/lop'

export const lopHocService = {
  /**
   * `namHocId` bắt buộc, không có API "lấy tất cả lớp mọi năm".
   *
   * Backend cố ý ép như vậy: sau vài năm vận hành bảng này có vài trăm dòng
   * thuộc nhiều năm khác nhau, mà gần như không màn hình nào cần trộn chúng.
   */
  layTheoNamHoc: (namHocId: string, nganhId?: string) =>
    http.get<LopHoc[]>(GOC, nganhId ? { namHocId, nganhId } : { namHocId }),

  tao: (body: LuuLopHocRequest) => http.post<LopHoc>(GOC, body),

  sua: (id: string, body: LuuLopHocRequest) => http.put<LopHoc>(`${GOC}/${id}`, body),

  // Dùng httpVoid vì endpoint này chỉ trả message, không có `data`.
  xoa: (id: string) => httpVoid.del(`${GOC}/${id}`),
}

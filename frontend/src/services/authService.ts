import { http, httpVoid } from './http'

/** Khớp với record ThongTinToiResponse bên Java. */
export interface NguoiDung {
  id: string
  hoTen: string
  email: string | null
  /** ADMIN, KHOI_TRUONG, HUYNH_TRUONG, KY_LUAT — KHÔNG có tiền tố ROLE_. */
  vaiTro: string[]
  canDoiMatKhau: boolean
}

export interface DangNhapResponse {
  accessToken: string
  hetHanSauGiay: number
  nguoiDung: NguoiDung
}

const GOC = '/api/v1/auth'

export const authService = {
  /**
   * `dinhDanh` là email HOẶC số điện thoại — backend tìm cả hai trong một
   * truy vấn, nên màn hình chỉ cần một ô nhập.
   */
  dangNhap: (dinhDanh: string, matKhau: string) =>
    http.post<DangNhapResponse>(`${GOC}/login`, { dinhDanh, matKhau }),

  /**
   * Không nhận tham số: refresh token nằm trong cookie HttpOnly, trình duyệt
   * tự đính kèm. JavaScript không đọc được nó, và đó chính là điều ta muốn.
   */
  lamMoi: () => http.post<DangNhapResponse>(`${GOC}/refresh`),

  dangXuat: () => httpVoid.post(`${GOC}/logout`),

  toi: () => http.get<NguoiDung>(`${GOC}/me`),

  doiMatKhau: (matKhauCu: string, matKhauMoi: string) =>
    httpVoid.post(`${GOC}/doi-mat-khau`, { matKhauCu, matKhauMoi }),
}

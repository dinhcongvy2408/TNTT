import { createContext, useContext } from 'react'
import type { NguoiDung } from '@/services/authService'

/**
 * Định nghĩa context và hook truy cập.
 *
 * <p>Tách khỏi <code>AuthProvider.tsx</code> vì Fast Refresh của Vite chỉ
 * hoạt động khi một file <code>.tsx</code> chỉ export component. Trộn
 * component với hằng số/hook trong cùng file khiến mỗi lần sửa là cả cây
 * component bị dựng lại thay vì cập nhật tại chỗ — mất state đang gõ dở.
 */

export type TrangThaiDangNhap = 'dang-kiem-tra' | 'da-dang-nhap' | 'chua-dang-nhap'

export interface BoiCanhAuth {
  trangThai: TrangThaiDangNhap
  nguoiDung: NguoiDung | null
  dangNhap: (dinhDanh: string, matKhau: string) => Promise<void>
  dangXuat: () => Promise<void>
  /** Gọi sau khi đổi mật khẩu để cập nhật cờ canDoiMatKhau. */
  napLai: () => Promise<void>
  /** Đúng nếu người dùng có ÍT NHẤT MỘT trong các vai trò truyền vào. */
  coVaiTro: (...ma: string[]) => boolean
}

export const AuthContext = createContext<BoiCanhAuth | null>(null)

export function useAuth(): BoiCanhAuth {
  const boi = useContext(AuthContext)
  if (!boi) {
    // Ném lỗi rõ ràng thay vì trả null: quên bọc <AuthProvider> là lỗi lập
    // trình, và nó phải nổ ngay lần render đầu chứ không phải biến thành
    // "Cannot read property of null" ở một dòng nào đó xa hơn.
    throw new Error('useAuth phải nằm trong <AuthProvider>')
  }
  return boi
}

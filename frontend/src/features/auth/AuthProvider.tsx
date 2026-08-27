import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { authService, type NguoiDung } from '@/services/authService'
import { datAccessToken, datXuLyPhienHetHan } from '@/services/http'
import { AuthContext, type TrangThaiDangNhap } from './authContext'

/**
 * Giữ trạng thái đăng nhập cho cả ứng dụng.
 *
 * <b>Vì sao dùng Context chứ không TanStack Query?</b> CLAUDE.md mục 5 phân
 * biệt rõ: TanStack Query cho "state của server" (dữ liệu tải về, có thể cũ,
 * cần làm mới), còn <code>useState</code>/<code>useContext</code> cho "state
 * của UI". Danh tính người đăng nhập thuộc loại thứ hai — nó quyết định hiện
 * màn hình nào, và không được phép "cũ" một cách âm thầm.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [trangThai, datTrangThai] = useState<TrangThaiDangNhap>('dang-kiem-tra')
  const [nguoiDung, datNguoiDung] = useState<NguoiDung | null>(null)

  const quenPhien = useCallback(() => {
    datAccessToken(null)
    datNguoiDung(null)
    datTrangThai('chua-dang-nhap')
  }, [])

  /**
   * Khôi phục phiên khi mở lại tab hoặc bấm F5.
   *
   * Access token nằm trong biến JavaScript nên F5 là mất. Nhưng refresh token
   * nằm trong cookie HttpOnly và vẫn còn, nên ta thử đổi lấy access token mới.
   * Nhờ vậy người dùng không phải đăng nhập lại mỗi lần tải trang, mà token
   * vẫn không cần cất ở chỗ JavaScript đọc được.
   */
  useEffect(() => {
    datXuLyPhienHetHan(quenPhien)

    // Cờ này chặn việc gọi setState sau khi component đã bị gỡ. Ở chế độ
    // StrictMode của React 19, effect chạy hai lần lúc dev — không có cờ thì
    // lần chạy đầu (đã bị huỷ) vẫn ghi đè state của lần thứ hai.
    let conSong = true

    authService
      .lamMoi()
      .then((ketQua) => {
        if (!conSong) return
        datAccessToken(ketQua.accessToken)
        datNguoiDung(ketQua.nguoiDung)
        datTrangThai('da-dang-nhap')
      })
      .catch(() => {
        // Không có cookie, hoặc cookie đã hết hạn. Đây là đường đi BÌNH
        // THƯỜNG của người mở web lần đầu, không phải lỗi cần báo.
        if (conSong) datTrangThai('chua-dang-nhap')
      })

    return () => {
      conSong = false
      datXuLyPhienHetHan(null)
    }
  }, [quenPhien])

  const dangNhap = useCallback(async (dinhDanh: string, matKhau: string) => {
    const ketQua = await authService.dangNhap(dinhDanh, matKhau)
    datAccessToken(ketQua.accessToken)
    datNguoiDung(ketQua.nguoiDung)
    datTrangThai('da-dang-nhap')
  }, [])

  const dangXuat = useCallback(async () => {
    try {
      await authService.dangXuat()
    } finally {
      // finally chứ không phải then: dù gọi API thất bại (mất mạng) thì phía
      // trình duyệt vẫn phải quên phiên. Bấm đăng xuất mà vẫn thấy mình đang
      // đăng nhập là điều tệ nhất có thể xảy ra ở màn hình này.
      quenPhien()
    }
  }, [quenPhien])

  const napLai = useCallback(async () => {
    datNguoiDung(await authService.toi())
  }, [])

  const coVaiTro = useCallback(
    (...ma: string[]) => ma.some((m) => nguoiDung?.vaiTro.includes(m) ?? false),
    [nguoiDung],
  )

  // useMemo để object context không đổi tham chiếu ở mỗi lần render. Không có
  // nó thì MỌI component dùng useAuth đều render lại mỗi khi bất kỳ thứ gì
  // trong cây thay đổi.
  const giaTri = useMemo(
    () => ({ trangThai, nguoiDung, dangNhap, dangXuat, napLai, coVaiTro }),
    [trangThai, nguoiDung, dangNhap, dangXuat, napLai, coVaiTro],
  )

  return <AuthContext.Provider value={giaTri}>{children}</AuthContext.Provider>
}

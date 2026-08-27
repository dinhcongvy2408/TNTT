import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './authContext'

/**
 * Cổng chặn: chỉ cho qua khi đã đăng nhập.
 *
 * <p><b>Đây KHÔNG phải hàng rào bảo mật.</b> Bất kỳ ai cũng sửa được
 * JavaScript trong trình duyệt của họ để bỏ qua component này. Hàng rào thật
 * nằm ở backend — {@code SecurityConfig} với {@code anyRequest().authenticated()}
 * và các {@code @PreAuthorize}. Đây chỉ là giao diện: đưa người dùng tới đúng
 * màn hình thay vì để họ nhìn một trang trống rồi nhận hàng loạt lỗi 401.
 *
 * <p>CLAUDE.md mục 6 nói đúng ý này: "Phân quyền kiểm tra ở tầng service,
 * không chỉ ở tầng UI".
 */
export function CanDangNhap() {
  const { trangThai, nguoiDung } = useAuth()
  const viTri = useLocation()

  if (trangThai === 'dang-kiem-tra') {
    // Khoảnh khắc này có thật: lúc mở app, ta đang gọi /auth/refresh để khôi
    // phục phiên. Không có nhánh này thì người dùng đã đăng nhập sẽ thấy màn
    // hình đăng nhập loé lên rồi biến mất — trông như lỗi.
    return (
      <div className="flex min-h-dvh items-center justify-center">
        <p className="text-sm text-slate-400">Đang tải…</p>
      </div>
    )
  }

  if (trangThai === 'chua-dang-nhap') {
    // Nhớ nơi người dùng định tới, để đăng nhập xong đưa họ về đúng đó.
    return <Navigate to="/dang-nhap" replace state={{ tuDau: viTri.pathname }} />
  }

  // Tài khoản đang bị bắt đổi mật khẩu: backend đã chặn mọi endpoint khác,
  // nên có cho đi tiếp thì màn hình cũng chỉ toàn lỗi 403.
  if (nguoiDung?.canDoiMatKhau && viTri.pathname !== '/doi-mat-khau') {
    return <Navigate to="/doi-mat-khau" replace />
  }

  return <Outlet />
}

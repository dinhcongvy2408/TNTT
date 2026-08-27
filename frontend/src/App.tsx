import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from '@/lib/queryClient'
import { BoCucChinh } from '@/components/layout/BoCucChinh'
import { TrangChuaLam } from '@/components/TrangChuaLam'
import { TrangKhongTimThay } from '@/components/TrangKhongTimThay'
import { AuthProvider } from '@/features/auth/AuthProvider'
import { CanDangNhap } from '@/features/auth/CanDangNhap'
import { TrangDangNhap } from '@/features/auth/TrangDangNhap'
import { TrangDoiMatKhau } from '@/features/auth/TrangDoiMatKhau'
import { TrangChu } from '@/features/trang-chu/TrangChu'
import { TrangKiemTra } from '@/features/health/TrangKiemTra'
import { TrangNamHoc } from '@/features/nam-hoc/TrangNamHoc'
import { TrangLopHoc } from '@/features/lop-hoc/TrangLopHoc'

/**
 * Gốc của ứng dụng: chỉ lắp ráp provider và khai báo route.
 *
 * Giữ file này MỎNG là có chủ đích. Mọi màn hình đều nằm trong
 * src/features/<tên-module>/ theo CLAUDE.md mục 5, nên khi thêm sprint mới
 * ta chỉ thay đúng một dòng <Route> ở đây.
 *
 * Ba tầng lồng nhau, mỗi tầng một việc:
 *   <AuthProvider>   biết ai đang đăng nhập
 *     <CanDangNhap>  chặn người chưa đăng nhập
 *       <BoCucChinh> khung giao diện chung
 */
export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            {/* Đăng nhập nằm NGOÀI mọi tầng chặn và ngoài bố cục chính:
                chưa đăng nhập thì chưa được đi đâu, thanh điều hướng dưới
                đáy không có lý do tồn tại. */}
            <Route path="/dang-nhap" element={<TrangDangNhap />} />

            <Route element={<CanDangNhap />}>
              <Route element={<BoCucChinh />}>
                <Route path="/" element={<TrangChu />} />
                <Route path="/nam-hoc" element={<TrangNamHoc />} />
                <Route path="/lop-hoc" element={<TrangLopHoc />} />
                <Route path="/doi-mat-khau" element={<TrangDoiMatKhau />} />
                <Route path="/kiem-tra" element={<TrangKiemTra />} />

                <Route
                  path="/diem-danh"
                  element={
                    <TrangChuaLam
                      tenManHinh="Điểm danh"
                      sprint={5}
                      moTa="Điểm danh đi lễ và đi học theo từng lớp, từng buổi Chủ Nhật. Cần có ghi danh của Sprint 5 trước."
                    />
                  }
                />
                <Route
                  path="/thieu-nhi"
                  element={
                    <TrangChuaLam
                      tenManHinh="Hồ sơ thiếu nhi"
                      sprint={4}
                      moTa="Danh sách và hồ sơ từng em, kèm bí tích đã lãnh nhận và nhập từ file Excel."
                    />
                  }
                />

                {/* Trang 404 thật, không còn lặng lẽ đá về trang chủ.
                    Đá về trang chủ khiến người gõ sai URL tưởng mình bấm
                    nhầm nút, và giấu mất lỗi link hỏng khi ta gửi đường dẫn
                    cho nhau. */}
                <Route path="*" element={<TrangKhongTimThay />} />
              </Route>
            </Route>

            {/* Route bắt mọi thứ còn lại khi CHƯA đăng nhập. */}
            <Route path="*" element={<Navigate to="/dang-nhap" replace />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

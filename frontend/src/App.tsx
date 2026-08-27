import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from '@/lib/queryClient'
import { BoCucChinh } from '@/components/layout/BoCucChinh'
import { TrangChuaLam } from '@/components/TrangChuaLam'
import { TrangChu } from '@/features/trang-chu/TrangChu'
import { TrangKiemTra } from '@/features/health/TrangKiemTra'

/**
 * Gốc của ứng dụng: chỉ lắp ráp provider và khai báo route.
 *
 * Giữ file này MỎNG là có chủ đích. Mọi màn hình đều nằm trong
 * src/features/<tên-module>/ theo CLAUDE.md mục 5, nên khi thêm sprint mới
 * ta chỉ thay đúng một dòng <Route> ở đây.
 *
 * Route được LỒNG trong <Route element={<BoCucChinh />}>: mọi route con hiện
 * ra ở chỗ <Outlet/> bên trong bố cục, nên thanh tiêu đề và thanh điều hướng
 * không bị dựng lại mỗi lần chuyển màn hình.
 */
export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route element={<BoCucChinh />}>
            <Route path="/" element={<TrangChu />} />

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

            <Route
              path="/lop-hoc"
              element={
                <TrangChuaLam
                  tenManHinh="Lớp học"
                  sprint={2}
                  moTa="Năm học, ngành và danh sách lớp. Đây là nền của mọi module sau, vì mọi dữ liệu đều gắn với một năm học."
                />
              }
            />

            <Route path="/kiem-tra" element={<TrangKiemTra />} />

            {/* Sprint 1 sẽ thêm /dang-nhap NGOÀI bố cục này — trang đăng nhập
                không có thanh điều hướng, vì lúc đó chưa đăng nhập thì chưa
                được đi đâu cả. */}

            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

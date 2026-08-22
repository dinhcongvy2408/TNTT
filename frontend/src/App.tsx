import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from '@/lib/queryClient'
import { TrangKiemTra } from '@/features/health/TrangKiemTra'

/**
 * Gốc của ứng dụng: chỉ lắp ráp provider và khai báo route.
 *
 * Giữ file này MỎNG là có chủ đích. Mọi màn hình đều nằm trong
 * src/features/<tên-module>/ theo CLAUDE.md mục 5, nên khi thêm sprint mới
 * ta chỉ thêm đúng một dòng <Route> ở đây.
 */
export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/kiem-tra" element={<TrangKiemTra />} />

          {/* Sprint 1: /dang-nhap
              Sprint 2: /nam-hoc, /lop
              Sprint 5: /diem-danh  <- màn hình quan trọng nhất
              Sprint 7: /truc-cong                                    */}

          <Route path="*" element={<Navigate to="/kiem-tra" replace />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

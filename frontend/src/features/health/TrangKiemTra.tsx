import { useQuery } from '@tanstack/react-query'
import { healthService } from '@/services/healthService'
import { ApiError } from '@/services/types'

/**
 * Màn hình kiểm tra kết nối — sản phẩm của Sprint 0.
 *
 * Nó tồn tại để chứng minh cả chuỗi đã thông:
 *   React -> Vite proxy -> Spring Controller -> HikariCP -> PostgreSQL
 * và trả ngược lại. Sprint 1 sẽ thay nó bằng trang đăng nhập, nhưng giữ
 * lại đường /kiem-tra để còn chỗ chẩn đoán khi hệ thống lỗi trên VPS.
 *
 * Vì sao dùng useQuery thay vì useEffect + useState?
 * Vì useEffect thì ta phải tự viết: state loading, state error, huỷ request
 * khi component unmount, chống gọi hai lần ở StrictMode, retry khi lỗi
 * mạng... TanStack Query cho sẵn tất cả. Nó là "state của server", khác
 * hẳn state của UI — CLAUDE.md mục 5 phân biệt rõ hai loại này.
 */
export function TrangKiemTra() {
  const {
    data: health,
    isPending,
    error,
    refetch,
    isFetching,
  } = useQuery({
    queryKey: ['health'],
    queryFn: healthService.kiemTra,
    // Tự gọi lại mỗi 30 giây để thấy ngay khi backend chết
    refetchInterval: 30_000,
    retry: 1,
  })

  return (
    <main className="mx-auto max-w-md px-4 py-10">
      <header className="mb-8 text-center">
        <h1 className="text-xl font-bold text-xudoan-700">
          Xứ đoàn Thiếu Nhi Thánh Thể
        </h1>
        <p className="mt-1 text-sm text-slate-500">
          Hệ thống quản lý &middot; Sprint 0
        </p>
      </header>

      <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="font-semibold text-slate-800">Trạng thái hệ thống</h2>
          <button
            type="button"
            onClick={() => void refetch()}
            disabled={isFetching}
            className="nut-cham rounded-lg bg-xudoan-600 px-3 text-sm font-medium
                       text-white transition active:bg-xudoan-700
                       disabled:opacity-50"
          >
            {isFetching ? 'Đang tải…' : 'Tải lại'}
          </button>
        </div>

        {isPending && <p className="text-sm text-slate-500">Đang kiểm tra…</p>}

        {error && <ThongBaoLoi error={error} />}

        {health && (
          <dl className="divide-y divide-slate-100 text-sm">
            <Dong nhan="Backend" gia_tri={health.trangThai} tot={health.trangThai === 'UP'} />
            <Dong nhan="Database" gia_tri={health.database} tot={health.database === 'UP'} />
            <Dong nhan="Ứng dụng" gia_tri={health.ungDung} />
            <Dong nhan="Môi trường" gia_tri={health.profile} />
            <Dong
              nhan="Giờ máy chủ"
              gia_tri={new Date(health.thoiGianMayChu).toLocaleString('vi-VN')}
            />
          </dl>
        )}
      </section>

      <p className="mt-6 text-center text-xs text-slate-400">
        Nếu thấy lỗi kết nối: kiểm tra backend đã chạy ở cổng 8080 chưa,
        và <code>docker compose ps</code> xem PostgreSQL còn sống không.
      </p>
    </main>
  )
}

/** Một dòng nhãn — giá trị trong bảng trạng thái. */
function Dong({
  nhan,
  gia_tri,
  tot,
}: {
  nhan: string
  gia_tri: string
  tot?: boolean
}) {
  return (
    <div className="flex items-center justify-between py-2.5">
      <dt className="text-slate-500">{nhan}</dt>
      <dd
        className={
          tot === undefined
            ? 'font-medium text-slate-800'
            : tot
              ? 'font-semibold text-emerald-600'
              : 'font-semibold text-red-600'
        }
      >
        {gia_tri}
      </dd>
    </div>
  )
}

/**
 * Hiển thị lỗi.
 *
 * Chú ý cách phân biệt: lỗi mất mạng thì bảo người dùng kiểm tra đường
 * truyền; lỗi khác thì hiện thông điệp backend gửi về. Đừng bao giờ đổ
 * một câu "Có lỗi xảy ra" chung chung cho mọi trường hợp — huynh trưởng
 * đứng giữa sân nhà thờ sẽ không biết phải làm gì tiếp.
 */
function ThongBaoLoi({ error }: { error: unknown }) {
  const laLoiKetNoi = error instanceof ApiError && error.laLoiKetNoi
  return (
    <div className="rounded-lg bg-red-50 p-3 text-sm text-red-700">
      <p className="font-medium">
        {laLoiKetNoi ? 'Không kết nối được máy chủ' : 'Máy chủ báo lỗi'}
      </p>
      <p className="mt-1 text-red-600">
        {error instanceof Error ? error.message : String(error)}
      </p>
    </div>
  )
}

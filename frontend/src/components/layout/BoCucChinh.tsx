import { useEffect, useState } from 'react'
import { Link, Outlet } from 'react-router-dom'
import { ThanhDieuHuongDuoi } from './ThanhDieuHuongDuoi'
import { useAuth } from '@/features/auth/authContext'

/**
 * Bố cục chung của mọi màn hình: thanh tiêu đề — nội dung — thanh điều hướng.
 *
 * <b>Vì sao dùng route lồng nhau + {@code <Outlet/>} chứ không bọc từng
 * màn hình bằng {@code <BoCucChinh>}?</b>
 * Với {@code <Outlet/>}, khi chuyển màn hình React chỉ thay phần ruột; thanh
 * tiêu đề và thanh điều hướng KHÔNG bị tháo ra lắp lại. Nếu bọc thủ công thì
 * mỗi lần đổi route là cả cây component bị unmount rồi mount lại — thanh điều
 * hướng nhấp nháy, và mọi state trong đó mất sạch. Sang Sprint 7, badge "có
 * phiếu ra cổng mới" nằm trên thanh này và nhận tin qua WebSocket: mount lại
 * nghĩa là ngắt rồi nối lại kết nối sau mỗi cú bấm.
 *
 * <b>Vì sao {@code min-h-dvh} chứ không {@code min-h-screen}?</b>
 * {@code min-h-screen} là 100vh — trên Safari iOS, 100vh tính CẢ phần bị
 * thanh địa chỉ che, nên thanh điều hướng đáy bị đẩy xuống dưới mép màn hình.
 * {@code dvh} (dynamic viewport height) co giãn theo thanh địa chỉ, đúng thứ
 * ta cần.
 */
export function BoCucChinh() {
  const { nguoiDung, dangXuat } = useAuth()
  const [moMenu, datMoMenu] = useState(false)

  return (
    <div className="flex min-h-dvh flex-col">
      <DaiMatMang />

      <header
        className="sticky top-0 z-10 border-b border-slate-200 bg-xudoan-700
                   pt-[env(safe-area-inset-top)] text-white"
      >
        <div className="mx-auto flex h-14 max-w-lg items-center gap-2 px-4">
          <h1 className="min-w-0 flex-1 truncate text-base font-semibold">
            Xứ đoàn Thiếu Nhi Thánh Thể
          </h1>

          {nguoiDung && (
            <div className="relative shrink-0">
              <button
                type="button"
                onClick={() => datMoMenu((truoc) => !truoc)}
                aria-expanded={moMenu}
                className="nut-cham flex items-center justify-center rounded-full
                           bg-white/15 px-3 text-sm font-semibold active:bg-white/25"
              >
                {chuCaiDau(nguoiDung.hoTen)}
              </button>

              {moMenu && (
                <>
                  {/* Lớp phủ trong suốt: bấm ra ngoài là đóng menu. Cách này
                      hoạt động cả với chạm lẫn chuột, không cần nghe sự kiện
                      trên document. */}
                  <button
                    type="button"
                    aria-label="Đóng menu"
                    onClick={() => datMoMenu(false)}
                    className="fixed inset-0 z-10 cursor-default"
                  />
                  <div
                    className="absolute right-0 z-20 mt-2 w-56 overflow-hidden rounded-xl
                               border border-slate-200 bg-white shadow-lg"
                  >
                    <div className="border-b border-slate-100 px-4 py-3">
                      <p className="truncate text-sm font-semibold text-slate-800">
                        {nguoiDung.hoTen}
                      </p>
                      <p className="mt-0.5 truncate text-xs text-slate-500">
                        {nguoiDung.vaiTro.join(', ')}
                      </p>
                    </div>
                    <Link
                      to="/doi-mat-khau"
                      onClick={() => datMoMenu(false)}
                      className="block px-4 py-3 text-sm text-slate-700 active:bg-slate-50"
                    >
                      Đổi mật khẩu
                    </Link>
                    <button
                      type="button"
                      onClick={() => {
                        datMoMenu(false)
                        void dangXuat()
                      }}
                      className="block w-full px-4 py-3 text-left text-sm text-red-600
                                 active:bg-red-50"
                    >
                      Đăng xuất
                    </button>
                  </div>
                </>
              )}
            </div>
          )}
        </div>
      </header>

      {/*
        flex-1 để phần nội dung nở ra chiếm hết chỗ trống. Nhờ vậy khi trang
        ngắn, thanh điều hướng vẫn bị đẩy xuống đáy màn hình thay vì lửng lơ
        ngay dưới nội dung.
      */}
      <main className="mx-auto w-full max-w-lg flex-1 px-4 py-5">
        <Outlet />
      </main>

      <ThanhDieuHuongDuoi />
    </div>
  )
}

/**
 * Dải báo mất mạng.
 *
 * <p>Rất cần với hoàn cảnh dùng thật: huynh trưởng đứng giữa sân nhà thờ,
 * sóng chập chờn. Không có dải này thì mọi thao tác chỉ im lặng thất bại và
 * họ sẽ bấm đi bấm lại, tưởng app hỏng.
 *
 * <p>{@code navigator.onLine} không hoàn hảo — nó chỉ biết máy có kết nối
 * mạng hay không, chứ không biết mạng đó có ra được Internet. Nhưng nó bắt
 * đúng trường hợp phổ biến nhất (tắt wifi, mất sóng) và không tốn gì.
 */
function DaiMatMang() {
  const [coMang, datCoMang] = useState(() =>
    typeof navigator === 'undefined' ? true : navigator.onLine,
  )

  useEffect(() => {
    const online = () => datCoMang(true)
    const offline = () => datCoMang(false)
    window.addEventListener('online', online)
    window.addEventListener('offline', offline)
    return () => {
      window.removeEventListener('online', online)
      window.removeEventListener('offline', offline)
    }
  }, [])

  if (coMang) return null

  return (
    <div
      role="status"
      className="bg-amber-500 px-4 py-2 text-center text-sm font-medium text-white"
    >
      Mất kết nối mạng. Thao tác sẽ không lưu được.
    </div>
  )
}

/** "Giuse Nguyễn Văn A" → "NA". Đủ để nhận ra mình, đủ ngắn cho nút tròn. */
function chuCaiDau(hoTen: string): string {
  const tu = hoTen.trim().split(/\s+/).filter(Boolean)
  if (tu.length === 0) return '?'
  if (tu.length === 1) return (tu[0] ?? '?').slice(0, 2).toUpperCase()
  const cuoi = tu.slice(-2)
  return cuoi.map((t) => t.charAt(0)).join('').toUpperCase()
}

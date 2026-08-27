import { Outlet } from 'react-router-dom'
import { ThanhDieuHuongDuoi } from './ThanhDieuHuongDuoi'

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
  return (
    <div className="flex min-h-dvh flex-col">
      <header
        className="sticky top-0 z-10 border-b border-slate-200 bg-xudoan-700
                   pt-[env(safe-area-inset-top)] text-white"
      >
        <div className="mx-auto flex h-14 max-w-lg items-center px-4">
          <h1 className="truncate text-base font-semibold">
            Xứ đoàn Thiếu Nhi Thánh Thể
          </h1>
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

import { useCallback, useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { phieuRaCongService, type PhieuRaCong } from '@/services/phieuRaCongService'
import { namHocService } from '@/services/namHocService'
import { useWebSocketPhieu, type TrangThaiSocket } from '@/lib/useWebSocketPhieu'

/**
 * Màn hình trực cổng — Live Dashboard của Sprint 7.
 *
 * <p>Người trực cổng mở màn hình này và để yên suốt buổi lễ. Khi giáo lý viên
 * gửi phiếu, tên em phải hiện ra NGAY, kèm tiếng chuông — vì người trực đang
 * nhìn ra sân chứ không nhìn màn hình.
 *
 * <p><b>Hai đường nhận dữ liệu, có chủ đích:</b>
 * <ul>
 *   <li>WebSocket — tức thì, là đường chính.</li>
 *   <li>Polling 10 giây — chỉ bật khi socket đứt. Mạng ở sân nhà thờ chập
 *       chờn, mà một màn hình trực đứng im vài phút thì phụ huynh đứng chờ
 *       ngoài cổng. docs/05 Sprint 7 ghi rõ yêu cầu này.</li>
 * </ul>
 */

const KHOA = ['phieu-dang-cho'] as const

export function TrangTrucCong() {
  const queryClient = useQueryClient()
  const [phieuMoiNhat, datPhieuMoiNhat] = useState<string | null>(null)
  const phatChuong = useChuong()

  const { data: namHienTai } = useQuery({
    queryKey: ['nam-hoc-hien-tai'],
    queryFn: namHocService.layHienTai,
  })

  const trangThaiSocket = useWebSocketPhieu(
    namHienTai?.id ?? null,
    useCallback(
      (banTin) => {
        // Không tự sửa danh sách trong cache theo bản tin, mà bảo TanStack
        // Query tải lại. Bản tin có thể tới lệch thứ tự hoặc mất một cái khi
        // mạng chập chờn; tải lại thì màn hình luôn khớp với DB.
        void queryClient.invalidateQueries({ queryKey: KHOA })

        if (banTin.type === 'PHIEU_MOI') {
          phatChuong()
          datPhieuMoiNhat(banTin.phieu.id)
          // Bỏ đánh dấu sau 30 giây để dòng mới không sáng mãi.
          window.setTimeout(() => datPhieuMoiNhat(null), 30_000)
        }
      },
      [queryClient, phatChuong],
    ),
  )

  const matKetNoi = trangThaiSocket === 'mat-ket-noi'

  const { data: danhSach, isPending, error } = useQuery({
    queryKey: KHOA,
    queryFn: phieuRaCongService.dangCho,
    // Đây là toàn bộ cơ chế dự phòng: khi socket đứt thì hỏi lại mỗi 10 giây,
    // khi socket sống thì thôi hẳn để khỏi tốn băng thông vô ích.
    refetchInterval: matKetNoi ? 10_000 : false,
  })

  return (
    <div className="space-y-4">
      <DaiKetNoi trangThai={trangThaiSocket} />

      <div className="flex items-baseline justify-between px-1">
        <h2 className="font-semibold text-slate-800">Đang chờ ra cổng</h2>
        <span className="text-sm text-slate-500">{danhSach?.length ?? 0} em</span>
      </div>

      {isPending && <p className="text-sm text-slate-500">Đang tải…</p>}
      {error && (
        <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">
          {error instanceof Error ? error.message : 'Không tải được danh sách'}
        </p>
      )}

      {danhSach?.length === 0 && (
        <div className="rounded-xl border border-dashed border-slate-300 bg-white p-8 text-center">
          <p className="text-sm text-slate-500">Chưa có em nào xin về.</p>
          <p className="mt-1 text-xs text-slate-400">
            Màn hình sẽ tự cập nhật khi có phiếu mới.
          </p>
        </div>
      )}

      <ul className="space-y-2">
        {danhSach?.map((phieu) => (
          <li key={phieu.id}>
            <ThePhieu phieu={phieu} noiBat={phieu.id === phieuMoiNhat} />
          </li>
        ))}
      </ul>
    </div>
  )
}

// ---------------------------------------------------------------------

function DaiKetNoi({ trangThai }: { trangThai: TrangThaiSocket }) {
  if (trangThai === 'da-noi') {
    return (
      <p className="flex items-center gap-2 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
        <span className="h-2 w-2 shrink-0 rounded-full bg-emerald-500" />
        Đang nhận tin trực tiếp
      </p>
    )
  }
  if (trangThai === 'dang-noi') {
    return (
      <p className="rounded-lg bg-slate-100 px-3 py-2 text-sm text-slate-500">
        Đang kết nối…
      </p>
    )
  }
  return (
    <p className="flex items-center gap-2 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-800">
      <span className="h-2 w-2 shrink-0 rounded-full bg-amber-500" />
      Mất kết nối trực tiếp. Đang tự tải lại mỗi 10 giây.
    </p>
  )
}

// ---------------------------------------------------------------------

function ThePhieu({ phieu, noiBat }: { phieu: PhieuRaCong; noiBat: boolean }) {
  const queryClient = useQueryClient()
  const lamMoi = () => void queryClient.invalidateQueries({ queryKey: KHOA })

  const xacNhan = useMutation({
    mutationFn: () => phieuRaCongService.xacNhan(phieu.id),
    onSuccess: lamMoi,
  })

  return (
    <div
      className={`rounded-xl border bg-white p-4 transition ${
        noiBat ? 'border-amber-400 ring-2 ring-amber-200' : 'border-slate-200'
      }`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-lg font-semibold text-slate-800">
            {phieu.tenThanh ? `${phieu.tenThanh} ` : ''}
            {phieu.hoTen}
          </p>
          <p className="mt-0.5 text-sm text-slate-500">
            {phieu.maThieuNhi} ·{' '}
            {/* tenLop rỗng khi em chưa được ghi danh — nói thẳng thay vì để
                trống, người trực cần biết vì sao không thấy tên lớp. */}
            {phieu.tenLop ?? <span className="text-amber-600">chưa có lớp</span>}
          </p>
        </div>
        <span className="shrink-0 text-xs text-slate-400">
          {gio(phieu.thoiGianTao)}
        </span>
      </div>

      <p className="mt-2 rounded-lg bg-slate-50 px-3 py-2 text-sm text-slate-700">
        {phieu.lyDo}
      </p>

      <p className="mt-2 text-xs text-slate-400">Người xin: {phieu.nguoiTao}</p>

      <button
        type="button"
        onClick={() => xacNhan.mutate()}
        disabled={xacNhan.isPending}
        // Nút cao hơn bình thường: người trực cổng bấm vội, một tay cầm điện
        // thoại, tay kia dắt em nhỏ.
        className="nut-cham mt-3 w-full rounded-lg bg-emerald-600 py-3 text-sm font-semibold
                   text-white active:bg-emerald-700 disabled:opacity-50"
      >
        {xacNhan.isPending ? 'Đang lưu…' : 'Xác nhận em đã ra về'}
      </button>

      {xacNhan.error && (
        <p className="mt-2 text-xs text-red-600">
          {xacNhan.error instanceof Error ? xacNhan.error.message : 'Thất bại'}
        </p>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------

/**
 * Tiếng chuông báo có phiếu mới.
 *
 * <p>Sinh âm bằng Web Audio API thay vì tải một file mp3: một tiếng "bing"
 * hai nốt chỉ cần vài dòng, còn file âm thanh thì phải thêm tài nguyên vào
 * bundle và phải tải về qua mạng — đúng thứ đang chập chờn.
 *
 * <p><b>Vì sao tạo AudioContext lười (chỉ khi cần)?</b> Trình duyệt chặn phát
 * âm thanh cho tới khi người dùng đã tương tác với trang. Tạo sẵn từ lúc mở
 * màn hình thì context sinh ra ở trạng thái "suspended" và tiếng chuông đầu
 * tiên bị nuốt mất — đúng cái tiếng quan trọng nhất.
 */
function useChuong(): () => void {
  const refContext = useRef<AudioContext | null>(null)

  useEffect(() => {
    return () => {
      void refContext.current?.close()
    }
  }, [])

  return useCallback(() => {
    try {
      refContext.current ??= new AudioContext()
      const ctx = refContext.current
      void ctx.resume()

      // Hai nốt ngắn, quãng năm — nghe rõ trong tiếng ồn sân nhà thờ mà không
      // chói như còi báo động.
      ;[880, 1320].forEach((tanSo, i) => {
        const dao = ctx.createOscillator()
        const amLuong = ctx.createGain()
        dao.frequency.value = tanSo
        dao.type = 'sine'
        amLuong.gain.setValueAtTime(0.001, ctx.currentTime + i * 0.18)
        amLuong.gain.exponentialRampToValueAtTime(0.3, ctx.currentTime + i * 0.18 + 0.02)
        amLuong.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + i * 0.18 + 0.16)
        dao.connect(amLuong).connect(ctx.destination)
        dao.start(ctx.currentTime + i * 0.18)
        dao.stop(ctx.currentTime + i * 0.18 + 0.18)
      })
    } catch {
      // Không phát được âm thanh thì thôi — dòng phiếu vẫn sáng lên và danh
      // sách vẫn cập nhật. Chuông là phần thêm, không phải phần thiết yếu.
    }
  }, [])
}

/** "2026-09-06T08:15:00+07:00" → "08:15". */
function gio(iso: string): string {
  return new Date(iso).toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

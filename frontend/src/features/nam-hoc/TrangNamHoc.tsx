import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  namHocService,
  NHAN_TRANG_THAI,
  type NamHoc,
  type TaoNamHocRequest,
} from '@/services/namHocService'
import { ApiError } from '@/services/types'

/**
 * Màn hình quản lý năm học — sản phẩm đầu tiên của Sprint 2.
 *
 * docs/02 bước 1: "Không có gì hoạt động được nếu hệ thống chưa biết đang ở
 * năm học nào". Đây là màn hình phải dùng trước tất cả các màn hình khác.
 */

const KHOA = ['nam-hoc'] as const

export function TrangNamHoc() {
  const { data: danhSach, isPending, error } = useQuery({
    queryKey: KHOA,
    queryFn: namHocService.layTatCa,
  })

  return (
    <div className="space-y-5">
      <FormTaoNamHoc />

      <section>
        <h2 className="mb-2 px-1 text-sm font-semibold text-slate-500">
          Danh sách năm học
        </h2>

        {isPending && <p className="text-sm text-slate-500">Đang tải…</p>}

        {error && (
          <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">
            {error instanceof Error ? error.message : 'Không tải được danh sách'}
          </p>
        )}

        {danhSach?.length === 0 && (
          <p className="rounded-xl border border-dashed border-slate-300 bg-white p-6 text-center text-sm text-slate-500">
            Chưa có năm học nào. Tạo năm học đầu tiên ở trên.
          </p>
        )}

        <ul className="space-y-2">
          {danhSach?.map((namHoc) => (
            <li key={namHoc.id}>
              <TheNamHoc namHoc={namHoc} />
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}

// ---------------------------------------------------------------------
// Form tạo
// ---------------------------------------------------------------------

function FormTaoNamHoc() {
  const queryClient = useQueryClient()
  const [form, datForm] = useState<TaoNamHocRequest>({
    tenNamHoc: '',
    ngayBatDau: '',
    ngayKetThuc: '',
  })

  const taoMoi = useMutation({
    mutationFn: namHocService.tao,
    onSuccess: () => {
      // Không tự nhét bản ghi mới vào cache bằng tay. Bảo TanStack Query tải
      // lại danh sách: server là nguồn sự thật, và nó còn quyết cả thứ tự sắp
      // xếp lẫn trạng thái khởi tạo.
      void queryClient.invalidateQueries({ queryKey: KHOA })
      datForm({ tenNamHoc: '', ngayBatDau: '', ngayKetThuc: '' })
    },
  })

  // Backend trả fieldErrors theo TÊN FIELD, nên ta bôi đỏ đúng ô sai thay vì
  // đổ một câu chung chung lên đầu form.
  const loi = taoMoi.error instanceof ApiError ? taoMoi.error : null
  const loiField = loi?.fieldErrors

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-4">
      <h2 className="mb-3 font-semibold text-slate-800">Tạo năm học mới</h2>

      <form
        className="space-y-3"
        onSubmit={(e) => {
          e.preventDefault()
          taoMoi.mutate(form)
        }}
      >
        <O
          nhan="Tên năm học"
          giaTri={form.tenNamHoc}
          datGiaTri={(v) => datForm({ ...form, tenNamHoc: v })}
          goiY="2026-2027"
          loi={loiField?.tenNamHoc}
        />
        <O
          nhan="Ngày bắt đầu"
          loai="date"
          giaTri={form.ngayBatDau}
          datGiaTri={(v) => datForm({ ...form, ngayBatDau: v })}
          loi={loiField?.ngayBatDau}
        />
        <O
          nhan="Ngày kết thúc"
          loai="date"
          giaTri={form.ngayKetThuc}
          datGiaTri={(v) => datForm({ ...form, ngayKetThuc: v })}
          loi={loiField?.ngayKetThuc}
        />

        {/* Lỗi nghiệp vụ (422) và lỗi trùng (409) không gắn với field nào,
            hiện riêng ở đây. */}
        {loi && !loiField && (
          <p className="rounded-lg bg-red-50 p-2.5 text-sm text-red-700">
            {loi.message}
          </p>
        )}

        <button
          type="submit"
          disabled={taoMoi.isPending}
          className="nut-cham w-full rounded-lg bg-xudoan-600 px-4 text-sm font-semibold
                     text-white active:bg-xudoan-700 disabled:opacity-50"
        >
          {taoMoi.isPending ? 'Đang lưu…' : 'Tạo năm học'}
        </button>
      </form>

      <p className="mt-2 text-xs text-slate-400">
        Năm học mới luôn ở trạng thái Chuẩn bị. Phải bấm Kích hoạt thì mới đưa
        vào vận hành.
      </p>
    </section>
  )
}

/** Một ô nhập kèm nhãn và chỗ hiện lỗi. */
function O({
  nhan,
  giaTri,
  datGiaTri,
  loai = 'text',
  goiY,
  loi,
}: {
  nhan: string
  giaTri: string
  datGiaTri: (v: string) => void
  loai?: 'text' | 'date'
  goiY?: string
  loi?: string
}) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-slate-700">{nhan}</span>
      <input
        type={loai}
        value={giaTri}
        placeholder={goiY}
        onChange={(e) => datGiaTri(e.target.value)}
        className={`w-full rounded-lg border px-3 py-2 outline-none
                    focus:ring-2 focus:ring-xudoan-500/40
                    ${loi ? 'border-red-400 bg-red-50' : 'border-slate-300'}`}
      />
      {loi && <span className="mt-1 block text-xs text-red-600">{loi}</span>}
    </label>
  )
}

// ---------------------------------------------------------------------
// Thẻ một năm học
// ---------------------------------------------------------------------

function TheNamHoc({ namHoc }: { namHoc: NamHoc }) {
  const queryClient = useQueryClient()
  const lamMoi = () => void queryClient.invalidateQueries({ queryKey: KHOA })

  const kichHoat = useMutation({ mutationFn: namHocService.kichHoat, onSuccess: lamMoi })
  const ketThuc = useMutation({ mutationFn: namHocService.ketThuc, onSuccess: lamMoi })

  const dangChay = namHoc.trangThai === 'DANG_HOAT_DONG'
  const daXong = namHoc.trangThai === 'DA_KET_THUC'
  const loi = kichHoat.error ?? ketThuc.error

  return (
    <div
      className={`rounded-xl border bg-white p-4 ${
        dangChay ? 'border-emerald-300 ring-1 ring-emerald-200' : 'border-slate-200'
      }`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="font-semibold text-slate-800">{namHoc.tenNamHoc}</p>
          <p className="mt-0.5 text-xs text-slate-500">
            {dinhDangNgay(namHoc.ngayBatDau)} — {dinhDangNgay(namHoc.ngayKetThuc)}
          </p>
        </div>
        <HuyHieu trangThai={namHoc.trangThai} />
      </div>

      {!daXong && (
        <div className="mt-3 flex gap-2">
          {!dangChay && (
            <button
              type="button"
              onClick={() => kichHoat.mutate(namHoc.id)}
              disabled={kichHoat.isPending}
              className="nut-cham flex-1 rounded-lg bg-xudoan-600 px-3 text-sm font-medium
                         text-white active:bg-xudoan-700 disabled:opacity-50"
            >
              Kích hoạt
            </button>
          )}
          {dangChay && (
            <button
              type="button"
              // Xác nhận bằng confirm() của trình duyệt: kết thúc năm học là
              // thao tác MỘT CHIỀU, không có nút hoàn tác. Hộp thoại đẹp hơn
              // là việc của phần trau chuốt sau, còn hàng rào thì cần ngay.
              onClick={() => {
                const dongY = window.confirm(
                  `Kết thúc năm học ${namHoc.tenNamHoc}?\n\nSau khi kết thúc, dữ liệu của năm này thành chỉ đọc và KHÔNG mở lại được.`,
                )
                if (dongY) ketThuc.mutate(namHoc.id)
              }}
              disabled={ketThuc.isPending}
              className="nut-cham flex-1 rounded-lg border border-slate-300 px-3 text-sm
                         font-medium text-slate-700 active:bg-slate-100 disabled:opacity-50"
            >
              Kết thúc năm học
            </button>
          )}
        </div>
      )}

      {loi && (
        <p className="mt-2 rounded-lg bg-red-50 p-2 text-xs text-red-700">
          {loi instanceof Error ? loi.message : 'Thao tác thất bại'}
        </p>
      )}
    </div>
  )
}

function HuyHieu({ trangThai }: { trangThai: NamHoc['trangThai'] }) {
  const kieu = {
    CHUAN_BI: 'bg-slate-100 text-slate-600',
    DANG_HOAT_DONG: 'bg-emerald-100 text-emerald-700',
    DA_KET_THUC: 'bg-slate-100 text-slate-400',
  }[trangThai]

  return (
    <span className={`shrink-0 rounded-full px-2.5 py-1 text-[11px] font-semibold ${kieu}`}>
      {NHAN_TRANG_THAI[trangThai]}
    </span>
  )
}

/** "2026-09-01" -> "01/09/2026". Người Việt đọc ngày trước, tháng sau. */
function dinhDangNgay(iso: string): string {
  const [nam, thang, ngay] = iso.split('-')
  return `${ngay}/${thang}/${nam}`
}

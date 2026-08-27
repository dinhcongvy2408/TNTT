import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ghiDanhService,
  thieuNhiService,
  type LuuThieuNhiRequest,
  type ThieuNhi,
} from '@/services/thieuNhiService'
import { phieuRaCongService } from '@/services/phieuRaCongService'
import { namHocService } from '@/services/namHocService'
import { lopHocService } from '@/services/lopHocService'
import { ApiError } from '@/services/types'
import { useAuth } from '@/features/auth/authContext'

/**
 * Hồ sơ thiếu nhi — lát cắt Sprint 4.
 *
 * Ba việc trên một màn hình vì chúng luôn đi liền nhau trong thực tế: tìm em,
 * xếp em vào lớp, và xin cho em về sớm.
 *
 * CHƯA có: lịch sử bí tích, import Excel, tìm không dấu.
 */

const KHOA = ['thieu-nhi'] as const

export function TrangThieuNhi() {
  const { coVaiTro } = useAuth()
  const [tuKhoa, datTuKhoa] = useState('')
  const [moForm, datMoForm] = useState(false)

  const { data: trang, isPending, error } = useQuery({
    queryKey: [...KHOA, tuKhoa],
    queryFn: () => thieuNhiService.tim(tuKhoa),
  })

  const laQuanTri = coVaiTro('ADMIN', 'KHOI_TRUONG')

  return (
    <div className="space-y-4">
      <div className="flex gap-2">
        <input
          value={tuKhoa}
          onChange={(e) => datTuKhoa(e.target.value)}
          placeholder="Tìm theo tên hoặc mã"
          className="min-w-0 flex-1 rounded-lg border border-slate-300 px-3 py-2 outline-none
                     focus:ring-2 focus:ring-xudoan-500/40"
        />
        {laQuanTri && (
          <button
            type="button"
            onClick={() => datMoForm((truoc) => !truoc)}
            className="nut-cham shrink-0 rounded-lg bg-xudoan-600 px-4 text-sm font-semibold text-white"
          >
            {moForm ? 'Đóng' : 'Thêm'}
          </button>
        )}
      </div>

      {/* Tìm chưa bỏ dấu — nói thẳng để người dùng không tưởng là app hỏng. */}
      {tuKhoa && trang?.content.length === 0 && (
        <p className="rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-800">
          Không tìm thấy. Lưu ý: hiện phải gõ đúng dấu tiếng Việt, "bich" không
          ra "Bích".
        </p>
      )}

      {moForm && <FormThieuNhi dong={() => datMoForm(false)} />}

      {isPending && <p className="text-sm text-slate-500">Đang tải…</p>}
      {error && (
        <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">
          {error instanceof Error ? error.message : 'Không tải được danh sách'}
        </p>
      )}

      {trang && (
        <p className="px-1 text-xs text-slate-400">
          {trang.totalElements} hồ sơ
        </p>
      )}

      <ul className="space-y-2">
        {trang?.content.map((em) => (
          <li key={em.id}>
            <TheThieuNhi em={em} laQuanTri={laQuanTri} />
          </li>
        ))}
      </ul>
    </div>
  )
}

// ---------------------------------------------------------------------

function TheThieuNhi({ em, laQuanTri }: { em: ThieuNhi; laQuanTri: boolean }) {
  const [moThaoTac, datMoThaoTac] = useState(false)

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4">
      <button
        type="button"
        onClick={() => datMoThaoTac((truoc) => !truoc)}
        className="flex w-full items-start justify-between gap-3 text-left"
      >
        <div className="min-w-0">
          <p className="truncate font-semibold text-slate-800">{em.tenDayDu}</p>
          <p className="mt-0.5 text-xs text-slate-500">
            {em.maThieuNhi} · {em.tuoi} tuổi
            {em.gioiTinh ? ` · ${em.gioiTinh === 'NAM' ? 'Nam' : 'Nữ'}` : ''}
          </p>
        </div>
        <span className="shrink-0 text-xs text-slate-400">
          {moThaoTac ? 'Thu gọn' : 'Mở'}
        </span>
      </button>

      {moThaoTac && (
        <div className="mt-3 space-y-3 border-t border-slate-100 pt-3">
          {em.sdtPhuHuynh && (
            <p className="text-xs text-slate-500">
              Phụ huynh:{' '}
              <a href={`tel:${em.sdtPhuHuynh}`} className="text-xudoan-600 underline">
                {em.sdtPhuHuynh}
              </a>
              {/* Bấm số là gọi luôn — người trực cổng cần gọi phụ huynh ngay,
                  không phải chép tay sang ứng dụng điện thoại. */}
            </p>
          )}

          <XinChoVe em={em} />
          {laQuanTri && <XepLop em={em} />}
        </div>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------

/** Tạo phiếu ra cổng — thao tác của giáo lý viên (docs/02 mục 6.2). */
function XinChoVe({ em }: { em: ThieuNhi }) {
  const [lyDo, datLyDo] = useState('')
  const [xong, datXong] = useState(false)

  const guiPhieu = useMutation({
    mutationFn: () => phieuRaCongService.tao(em.id, lyDo),
    onSuccess: () => {
      datXong(true)
      datLyDo('')
    },
  })

  if (xong) {
    return (
      <p className="rounded-lg bg-emerald-50 p-2.5 text-sm text-emerald-800">
        Đã gửi phiếu tới cổng. Người trực sẽ thấy ngay trên màn hình của họ.
      </p>
    )
  }

  return (
    <div className="rounded-lg bg-slate-50 p-3">
      <p className="mb-2 text-xs font-semibold text-slate-600">Xin cho về sớm</p>
      <div className="flex gap-2">
        <input
          value={lyDo}
          onChange={(e) => datLyDo(e.target.value)}
          placeholder="Lý do, VD: Ốm, phụ huynh xin về"
          className="min-w-0 flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none"
        />
        <button
          type="button"
          disabled={!lyDo.trim() || guiPhieu.isPending}
          onClick={() => guiPhieu.mutate()}
          className="nut-cham shrink-0 rounded-lg bg-amber-500 px-3 text-sm font-semibold
                     text-white active:bg-amber-600 disabled:opacity-50"
        >
          Gửi
        </button>
      </div>
      {guiPhieu.error && (
        <p className="mt-2 text-xs text-red-600">
          {guiPhieu.error instanceof Error ? guiPhieu.error.message : 'Gửi thất bại'}
        </p>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------

/** Ghi danh em vào một lớp của năm học đang hoạt động. */
function XepLop({ em }: { em: ThieuNhi }) {
  const queryClient = useQueryClient()
  const [lopId, datLopId] = useState('')

  const { data: namHienTai } = useQuery({
    queryKey: ['nam-hoc-hien-tai'],
    queryFn: namHocService.layHienTai,
  })

  const { data: dsLop } = useQuery({
    queryKey: ['lop-hoc', namHienTai?.id ?? '', ''],
    queryFn: () => lopHocService.layTheoNamHoc(namHienTai?.id ?? ''),
    // Chỉ chạy khi đã biết năm học — không có năm thì không có lớp nào để chọn.
    enabled: Boolean(namHienTai?.id),
  })

  const xepLop = useMutation({
    mutationFn: () => ghiDanhService.ghiDanh(em.id, lopId),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: KHOA }),
  })

  if (!namHienTai) {
    return (
      <p className="text-xs text-slate-400">
        Chưa có năm học đang hoạt động nên chưa xếp lớp được.
      </p>
    )
  }

  return (
    <div className="rounded-lg bg-slate-50 p-3">
      <p className="mb-2 text-xs font-semibold text-slate-600">
        Xếp lớp — năm {namHienTai.tenNamHoc}
      </p>
      <div className="flex gap-2">
        <select
          value={lopId}
          onChange={(e) => datLopId(e.target.value)}
          className="min-w-0 flex-1 rounded-lg border border-slate-300 px-2 py-2 text-sm"
        >
          <option value="">Chọn lớp</option>
          {dsLop?.map((lop) => (
            <option key={lop.id} value={lop.id}>
              {lop.tenLop} ({lop.tenNganh})
            </option>
          ))}
        </select>
        <button
          type="button"
          disabled={!lopId || xepLop.isPending}
          onClick={() => xepLop.mutate()}
          className="nut-cham shrink-0 rounded-lg bg-xudoan-600 px-3 text-sm font-semibold
                     text-white disabled:opacity-50"
        >
          Ghi danh
        </button>
      </div>
      {xepLop.isSuccess && (
        <p className="mt-2 text-xs text-emerald-700">Đã ghi danh</p>
      )}
      {xepLop.error && (
        <p className="mt-2 text-xs text-red-600">
          {xepLop.error instanceof Error ? xepLop.error.message : 'Ghi danh thất bại'}
        </p>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------

function FormThieuNhi({ dong }: { dong: () => void }) {
  const queryClient = useQueryClient()
  const [form, datForm] = useState<LuuThieuNhiRequest>({
    tenThanh: '',
    hoTen: '',
    ngaySinh: '',
    gioiTinh: 'NAM',
    tenBo: '',
    sdtPhuHuynh: '',
  })

  const tao = useMutation({
    mutationFn: () => thieuNhiService.tao(form),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: KHOA })
      dong()
    },
  })

  const loi = tao.error instanceof ApiError ? tao.error : null

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        tao.mutate()
      }}
      className="space-y-3 rounded-xl border border-slate-200 bg-white p-4"
    >
      <h2 className="font-semibold text-slate-800">Thêm hồ sơ thiếu nhi</h2>
      <p className="text-xs text-slate-400">
        Mã thiếu nhi do hệ thống tự sinh, dạng TN2026001.
      </p>

      <div className="flex gap-2">
        <O nhan="Tên Thánh" giaTri={form.tenThanh ?? ''} dat={(v) => datForm({ ...form, tenThanh: v })} rong="w-32" />
        <O nhan="Họ và tên" giaTri={form.hoTen} dat={(v) => datForm({ ...form, hoTen: v })} loi={loi?.fieldErrors?.hoTen} rong="flex-1" />
      </div>

      <div className="flex gap-2">
        <O nhan="Ngày sinh" loai="date" giaTri={form.ngaySinh} dat={(v) => datForm({ ...form, ngaySinh: v })} loi={loi?.fieldErrors?.ngaySinh} rong="flex-1" />
        <label className="block w-28">
          <span className="mb-1 block text-sm font-medium text-slate-700">Giới tính</span>
          <select
            value={form.gioiTinh}
            onChange={(e) => datForm({ ...form, gioiTinh: e.target.value })}
            className="w-full rounded-lg border border-slate-300 px-2 py-2"
          >
            <option value="NAM">Nam</option>
            <option value="NU">Nữ</option>
          </select>
        </label>
      </div>

      <O nhan="Tên bố hoặc mẹ" giaTri={form.tenBo ?? ''} dat={(v) => datForm({ ...form, tenBo: v })} />
      <O nhan="SĐT phụ huynh" giaTri={form.sdtPhuHuynh ?? ''} dat={(v) => datForm({ ...form, sdtPhuHuynh: v })} loi={loi?.fieldErrors?.sdtPhuHuynh} />

      {loi && !loi.fieldErrors && (
        <p className="rounded-lg bg-red-50 p-2.5 text-sm text-red-700">{loi.message}</p>
      )}

      <div className="flex gap-2">
        <button type="button" onClick={dong} className="nut-cham flex-1 rounded-lg border border-slate-300 px-4 text-sm font-medium text-slate-700">
          Huỷ
        </button>
        <button type="submit" disabled={tao.isPending} className="nut-cham flex-1 rounded-lg bg-xudoan-600 px-4 text-sm font-semibold text-white disabled:opacity-50">
          {tao.isPending ? 'Đang lưu…' : 'Lưu hồ sơ'}
        </button>
      </div>
    </form>
  )
}

function O({
  nhan, giaTri, dat, loai = 'text', loi, rong = 'w-full',
}: {
  nhan: string
  giaTri: string
  dat: (v: string) => void
  loai?: 'text' | 'date'
  loi?: string
  rong?: string
}) {
  return (
    <label className={`block ${rong}`}>
      <span className="mb-1 block text-sm font-medium text-slate-700">{nhan}</span>
      <input
        type={loai}
        value={giaTri}
        onChange={(e) => dat(e.target.value)}
        className={`w-full rounded-lg border px-3 py-2 outline-none focus:ring-2 focus:ring-xudoan-500/40
                    ${loi ? 'border-red-400 bg-red-50' : 'border-slate-300'}`}
      />
      {loi && <span className="mt-1 block text-xs text-red-600">{loi}</span>}
    </label>
  )
}

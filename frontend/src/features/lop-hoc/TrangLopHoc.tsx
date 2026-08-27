import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { namHocService, type NamHoc } from '@/services/namHocService'
import {
  lopHocService,
  nganhService,
  type LopHoc,
  type LuuLopHocRequest,
  type Nganh,
} from '@/services/lopHocService'
import { ApiError } from '@/services/types'

/**
 * Màn hình quản lý lớp học — phần còn lại của Sprint 2.
 *
 * <b>Vì sao phải chọn năm học trước mọi thứ khác?</b> docs/02 bước 1: mọi dữ
 * liệu vận hành đều gắn với một năm học, và lớp học không sống qua năm. Nếu
 * màn hình không bắt chọn năm ngay từ đầu, người dùng sẽ có lúc sửa lớp của
 * năm 2026 trong khi tưởng mình đang ở năm 2029.
 */

const KHOA_LOP = 'lop-hoc'

export function TrangLopHoc() {
  const [namHocId, datNamHocId] = useState<string | null>(null)
  const [locNganhId, datLocNganhId] = useState<string>('')

  const { data: dsNamHoc } = useQuery({
    queryKey: ['nam-hoc'],
    queryFn: namHocService.layTatCa,
  })
  const { data: dsNganh } = useQuery({
    queryKey: ['nganh'],
    queryFn: nganhService.layTatCa,
    // Ngành là dữ liệu gốc, cả năm không đổi. Giữ "tươi" 1 giờ thay vì 1 phút
    // mặc định để khỏi gọi lại mỗi lần mở màn hình trên mạng 3G.
    staleTime: 60 * 60_000,
  })

  // Chưa chọn gì thì lấy năm đang hoạt động, vì đó là năm người dùng cần 99%
  // số lần. Không dùng useEffect để set state: tính thẳng ra giá trị đang
  // dùng thì không có khoảnh khắc nào màn hình ở trạng thái nửa vời.
  const namDangChay = dsNamHoc?.find((n) => n.trangThai === 'DANG_HOAT_DONG')
  const namDangChon = namHocId ?? namDangChay?.id ?? dsNamHoc?.[0]?.id ?? null
  const namHoc = dsNamHoc?.find((n) => n.id === namDangChon)
  const chiDoc = namHoc?.trangThai === 'DA_KET_THUC'

  if (dsNamHoc?.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-slate-300 bg-white p-6 text-center">
        <p className="text-sm text-slate-500">
          Chưa có năm học nào. Phải tạo năm học trước khi tạo lớp.
        </p>
        <a
          href="/nam-hoc"
          className="nut-cham mt-4 inline-flex items-center justify-center rounded-lg
                     bg-xudoan-600 px-4 text-sm font-medium text-white"
        >
          Sang trang Năm học
        </a>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <ChonNamHoc
        danhSach={dsNamHoc ?? []}
        dangChon={namDangChon}
        datChon={(id) => {
          datNamHocId(id)
          datLocNganhId('')
        }}
      />

      {chiDoc && (
        <p className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-sm text-amber-800">
          Năm học này đã kết thúc. Dữ liệu chỉ đọc, không thêm sửa xoá được.
        </p>
      )}

      {namDangChon && !chiDoc && (
        <FormLopHoc namHocId={namDangChon} dsNganh={dsNganh ?? []} />
      )}

      {namDangChon && (
        <DanhSachLop
          namHocId={namDangChon}
          dsNganh={dsNganh ?? []}
          locNganhId={locNganhId}
          datLocNganhId={datLocNganhId}
          chiDoc={chiDoc}
        />
      )}
    </div>
  )
}

// ---------------------------------------------------------------------

function ChonNamHoc({
  danhSach,
  dangChon,
  datChon,
}: {
  danhSach: NamHoc[]
  dangChon: string | null
  datChon: (id: string) => void
}) {
  return (
    <label className="block rounded-xl border border-slate-200 bg-white p-4">
      <span className="mb-1 block text-sm font-medium text-slate-700">Năm học</span>
      <select
        value={dangChon ?? ''}
        onChange={(e) => datChon(e.target.value)}
        className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none
                   focus:ring-2 focus:ring-xudoan-500/40"
      >
        {danhSach.map((n) => (
          <option key={n.id} value={n.id}>
            {n.tenNamHoc}
            {n.trangThai === 'DANG_HOAT_DONG' ? ' — đang hoạt động' : ''}
            {n.trangThai === 'DA_KET_THUC' ? ' — đã kết thúc' : ''}
          </option>
        ))}
      </select>
    </label>
  )
}

// ---------------------------------------------------------------------

function DanhSachLop({
  namHocId,
  dsNganh,
  locNganhId,
  datLocNganhId,
  chiDoc,
}: {
  namHocId: string
  dsNganh: Nganh[]
  locNganhId: string
  datLocNganhId: (v: string) => void
  chiDoc: boolean
}) {
  const { data: dsLop, isPending, error } = useQuery({
    // namHocId và locNganhId nằm TRONG queryKey: đổi bộ lọc là TanStack Query
    // coi như một truy vấn khác và tự gọi lại, ta không phải viết useEffect.
    queryKey: [KHOA_LOP, namHocId, locNganhId],
    queryFn: () => lopHocService.layTheoNamHoc(namHocId, locNganhId || undefined),
  })

  return (
    <section>
      <div className="mb-2 flex items-center gap-2 px-1">
        <h2 className="text-sm font-semibold text-slate-500">
          Lớp học {dsLop ? `(${dsLop.length})` : ''}
        </h2>
        <select
          value={locNganhId}
          onChange={(e) => datLocNganhId(e.target.value)}
          className="ml-auto rounded-lg border border-slate-300 bg-white px-2 py-1 text-sm"
        >
          <option value="">Tất cả ngành</option>
          {dsNganh.map((n) => (
            <option key={n.id} value={n.id}>
              {n.tenNganh}
            </option>
          ))}
        </select>
      </div>

      {isPending && <p className="text-sm text-slate-500">Đang tải…</p>}

      {error && (
        <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">
          {error instanceof Error ? error.message : 'Không tải được danh sách'}
        </p>
      )}

      {dsLop?.length === 0 && (
        <p className="rounded-xl border border-dashed border-slate-300 bg-white p-6 text-center text-sm text-slate-500">
          Chưa có lớp nào trong năm học này.
        </p>
      )}

      <ul className="space-y-2">
        {dsLop?.map((lop) => (
          <li key={lop.id}>
            <TheLopHoc lop={lop} dsNganh={dsNganh} chiDoc={chiDoc} />
          </li>
        ))}
      </ul>
    </section>
  )
}

// ---------------------------------------------------------------------

function TheLopHoc({
  lop,
  dsNganh,
  chiDoc,
}: {
  lop: LopHoc
  dsNganh: Nganh[]
  chiDoc: boolean
}) {
  const queryClient = useQueryClient()
  const [dangSua, datDangSua] = useState(false)

  const xoa = useMutation({
    mutationFn: () => lopHocService.xoa(lop.id),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: [KHOA_LOP] }),
  })

  if (dangSua) {
    return (
      <FormLopHoc
        namHocId={lop.namHocId}
        dsNganh={dsNganh}
        lopDangSua={lop}
        huy={() => datDangSua(false)}
      />
    )
  }

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="font-semibold text-slate-800">{lop.tenLop}</p>
          <p className="mt-0.5 text-xs text-slate-500">
            {lop.tenNganh} · Cấp {lop.capDo}
          </p>
          {lop.ghiChu && (
            <p className="mt-1 text-xs text-slate-400">{lop.ghiChu}</p>
          )}
        </div>

        {!chiDoc && (
          <div className="flex shrink-0 gap-1">
            <button
              type="button"
              onClick={() => datDangSua(true)}
              className="nut-cham rounded-lg px-3 text-sm font-medium text-xudoan-600
                         active:bg-slate-100"
            >
              Sửa
            </button>
            <button
              type="button"
              onClick={() => {
                // Backend đã chặn xoá lớp có ghi danh, nhưng lớp trống thì xoá
                // thật và không hoàn tác được — vẫn phải hỏi.
                if (window.confirm(`Xoá lớp ${lop.tenLop}?`)) xoa.mutate()
              }}
              disabled={xoa.isPending}
              className="nut-cham rounded-lg px-3 text-sm font-medium text-red-600
                         active:bg-red-50 disabled:opacity-50"
            >
              Xoá
            </button>
          </div>
        )}
      </div>

      {xoa.error && (
        <p className="mt-2 rounded-lg bg-red-50 p-2 text-xs text-red-700">
          {xoa.error instanceof Error ? xoa.error.message : 'Xoá thất bại'}
        </p>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------

/**
 * Form dùng chung cho cả TẠO và SỬA.
 *
 * Một form cho hai việc vì backend cũng nhận đúng một DTO
 * (`LuuLopHocRequest`) cho cả hai. Tách đôi ở frontend trong khi backend
 * không tách chỉ tạo ra hai chỗ phải sửa mỗi lần thêm field.
 */
function FormLopHoc({
  namHocId,
  dsNganh,
  lopDangSua,
  huy,
}: {
  namHocId: string
  dsNganh: Nganh[]
  lopDangSua?: LopHoc
  huy?: () => void
}) {
  const queryClient = useQueryClient()
  const rong: LuuLopHocRequest = {
    tenLop: '',
    nganhId: dsNganh[0]?.id ?? '',
    namHocId,
    capDo: 1,
    ghiChu: '',
  }
  const [form, datForm] = useState<LuuLopHocRequest>(
    lopDangSua
      ? {
          tenLop: lopDangSua.tenLop,
          nganhId: lopDangSua.nganhId,
          namHocId: lopDangSua.namHocId,
          capDo: lopDangSua.capDo,
          ghiChu: lopDangSua.ghiChu ?? '',
        }
      : rong,
  )

  const luu = useMutation({
    mutationFn: (body: LuuLopHocRequest) =>
      lopDangSua ? lopHocService.sua(lopDangSua.id, body) : lopHocService.tao(body),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [KHOA_LOP] })
      if (huy) huy()
      else datForm({ ...rong, nganhId: form.nganhId })
    },
  })

  const loi = luu.error instanceof ApiError ? luu.error : null
  const loiField = loi?.fieldErrors

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-4">
      <h2 className="mb-3 font-semibold text-slate-800">
        {lopDangSua ? `Sửa lớp ${lopDangSua.tenLop}` : 'Thêm lớp học'}
      </h2>

      <form
        className="space-y-3"
        onSubmit={(e) => {
          e.preventDefault()
          luu.mutate(form)
        }}
      >
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-slate-700">Tên lớp</span>
          <input
            value={form.tenLop}
            placeholder="Ấu 1A"
            onChange={(e) => datForm({ ...form, tenLop: e.target.value })}
            className={`w-full rounded-lg border px-3 py-2 outline-none
                        focus:ring-2 focus:ring-xudoan-500/40
                        ${loiField?.tenLop ? 'border-red-400 bg-red-50' : 'border-slate-300'}`}
          />
          {loiField?.tenLop && (
            <span className="mt-1 block text-xs text-red-600">{loiField.tenLop}</span>
          )}
        </label>

        <div className="flex gap-3">
          <label className="block flex-1">
            <span className="mb-1 block text-sm font-medium text-slate-700">Ngành</span>
            <select
              value={form.nganhId}
              onChange={(e) => datForm({ ...form, nganhId: e.target.value })}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none
                         focus:ring-2 focus:ring-xudoan-500/40"
            >
              {dsNganh.map((n) => (
                <option key={n.id} value={n.id}>
                  {n.tenNganh}
                </option>
              ))}
            </select>
          </label>

          <label className="block w-24">
            <span className="mb-1 block text-sm font-medium text-slate-700">Cấp</span>
            <input
              type="number"
              min={1}
              max={10}
              value={form.capDo}
              onChange={(e) => datForm({ ...form, capDo: Number(e.target.value) })}
              className={`w-full rounded-lg border px-3 py-2 outline-none
                          focus:ring-2 focus:ring-xudoan-500/40
                          ${loiField?.capDo ? 'border-red-400 bg-red-50' : 'border-slate-300'}`}
            />
          </label>
        </div>
        {loiField?.capDo && (
          <span className="block text-xs text-red-600">{loiField.capDo}</span>
        )}

        <label className="block">
          <span className="mb-1 block text-sm font-medium text-slate-700">
            Ghi chú <span className="font-normal text-slate-400">(không bắt buộc)</span>
          </span>
          <input
            value={form.ghiChu ?? ''}
            onChange={(e) => datForm({ ...form, ghiChu: e.target.value })}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none
                       focus:ring-2 focus:ring-xudoan-500/40"
          />
        </label>

        {loi && !loiField && (
          <p className="rounded-lg bg-red-50 p-2.5 text-sm text-red-700">{loi.message}</p>
        )}

        <div className="flex gap-2">
          {huy && (
            <button
              type="button"
              onClick={huy}
              className="nut-cham flex-1 rounded-lg border border-slate-300 px-4 text-sm
                         font-medium text-slate-700 active:bg-slate-100"
            >
              Huỷ
            </button>
          )}
          <button
            type="submit"
            disabled={luu.isPending}
            className="nut-cham flex-1 rounded-lg bg-xudoan-600 px-4 text-sm font-semibold
                       text-white active:bg-xudoan-700 disabled:opacity-50"
          >
            {luu.isPending ? 'Đang lưu…' : lopDangSua ? 'Lưu' : 'Thêm lớp'}
          </button>
        </div>
      </form>
    </section>
  )
}

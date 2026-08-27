import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { healthService } from '@/services/healthService'
import { BieuTuong, type TenBieuTuong } from '@/components/BieuTuong'

/**
 * Trang chủ — màn hình đầu tiên huynh trưởng nhìn thấy.
 *
 * Hai việc nó làm:
 *   1. Cho biết hệ thống có đang sống không (dải trạng thái trên cùng).
 *   2. Là cửa vào các module. Thanh điều hướng đáy chỉ chứa 4 mục dùng hằng
 *      ngày; những mục còn lại nằm ở đây.
 */

interface LoiTat {
  duongDan: string
  nhan: string
  moTa: string
  icon: TenBieuTuong
  sprint: number
  daLam: boolean
}

const CAC_LOI_TAT: LoiTat[] = [
  {
    duongDan: '/diem-danh',
    nhan: 'Điểm danh',
    moTa: 'Ghi đi lễ, đi học từng buổi',
    icon: 'diem-danh',
    sprint: 5,
    daLam: false,
  },
  {
    duongDan: '/thieu-nhi',
    nhan: 'Hồ sơ thiếu nhi',
    moTa: 'Thông tin cá nhân và bí tích',
    icon: 'thieu-nhi',
    sprint: 4,
    daLam: false,
  },
  {
    duongDan: '/lop-hoc',
    nhan: 'Lớp học',
    moTa: 'Năm học, ngành, danh sách lớp',
    icon: 'lop-hoc',
    sprint: 2,
    daLam: false,
  },
  {
    duongDan: '/kiem-tra',
    nhan: 'Kiểm tra hệ thống',
    moTa: 'Trạng thái backend và database',
    icon: 'nhip-tim',
    sprint: 0,
    daLam: true,
  },
]

export function TrangChu() {
  return (
    <div className="space-y-5">
      <DaiTrangThai />

      <section>
        <h2 className="mb-2 px-1 text-sm font-semibold text-slate-500">
          Chức năng
        </h2>
        <ul className="space-y-2">
          {CAC_LOI_TAT.map((loiTat) => (
            <li key={loiTat.duongDan}>
              <TheLoiTat loiTat={loiTat} />
            </li>
          ))}
        </ul>
      </section>

      <p className="px-1 text-center text-xs leading-relaxed text-slate-400">
        Hệ thống đang ở giai đoạn xây dựng. Các chức năng chưa mở sẽ lần lượt
        có theo lộ trình trong <code>docs/05-lo-trinh.md</code>.
      </p>
    </div>
  )
}

/**
 * Dải trạng thái hệ thống.
 *
 * Vì sao đặt ngay trên cùng Trang chủ? Vì lỗi hay gặp nhất ở hiện trường
 * không phải lỗi lập trình, mà là "mạng nhà thờ chập chờn" hoặc "server
 * chưa bật". Thấy ngay dòng đỏ ở đây thì huynh trưởng biết vấn đề không
 * nằm ở thao tác của mình, đỡ bấm đi bấm lại.
 */
function DaiTrangThai() {
  const { data: health, isPending, isError } = useQuery({
    queryKey: ['health'],
    queryFn: healthService.kiemTra,
    refetchInterval: 60_000,
    retry: 1,
  })

  // queryKey trùng với màn hình /kiem-tra là CỐ Ý: TanStack Query dùng chung
  // một bản cache cho cùng một key, nên chuyển qua lại giữa hai màn hình
  // không tạo thêm request nào.

  if (isPending) {
    return <Dai mau="trung tinh" chu="Đang kiểm tra kết nối…" />
  }

  if (isError || health?.database !== 'UP') {
    return <Dai mau="loi" chu="Mất kết nối tới máy chủ hoặc cơ sở dữ liệu" />
  }

  return <Dai mau="tot" chu="Hệ thống hoạt động bình thường" />
}

function Dai({ mau, chu }: { mau: 'tot' | 'loi' | 'trung tinh'; chu: string }) {
  const kieu = {
    tot: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    loi: 'bg-red-50 text-red-700 border-red-200',
    'trung tinh': 'bg-slate-100 text-slate-500 border-slate-200',
  }[mau]

  return (
    <div className={`flex items-center gap-2 rounded-lg border px-3 py-2.5 text-sm ${kieu}`}>
      <BieuTuong ten="nhip-tim" className="h-4 w-4 shrink-0" />
      <span className="font-medium">{chu}</span>
    </div>
  )
}

/** Một thẻ lối tắt trong danh sách chức năng. */
function TheLoiTat({ loiTat }: { loiTat: LoiTat }) {
  return (
    <Link
      to={loiTat.duongDan}
      className="nut-cham flex items-center gap-3 rounded-xl border border-slate-200
                 bg-white px-4 py-3 transition active:bg-slate-50"
    >
      <span
        className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-lg ${
          loiTat.daLam
            ? 'bg-xudoan-50 text-xudoan-600'
            : 'bg-slate-100 text-slate-400'
        }`}
      >
        <BieuTuong ten={loiTat.icon} className="h-5 w-5" />
      </span>

      <span className="min-w-0 flex-1">
        <span className="flex items-center gap-2">
          <span className="truncate font-medium text-slate-800">{loiTat.nhan}</span>
          {!loiTat.daLam && (
            <span className="shrink-0 rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-semibold text-slate-500">
              Sprint {loiTat.sprint}
            </span>
          )}
        </span>
        <span className="mt-0.5 block truncate text-xs text-slate-500">
          {loiTat.moTa}
        </span>
      </span>

      <BieuTuong ten="mui-ten-phai" className="h-4 w-4 shrink-0 text-slate-300" />
    </Link>
  )
}

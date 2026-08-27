import { NavLink } from 'react-router-dom'
import { BieuTuong, type TenBieuTuong } from '@/components/BieuTuong'

/**
 * Thanh điều hướng chính, đặt ở ĐÁY màn hình.
 *
 * <b>Vì sao đáy chứ không phải menu hamburger ở đỉnh?</b>
 * Người dùng chính của app này là huynh trưởng đứng giữa sân nhà thờ, cầm
 * điện thoại một tay, bấm bằng ngón cái. Vùng ngón cái với tới thoải mái là
 * nửa dưới màn hình — góc trên bên trái (chỗ hamburger thường nằm) là chỗ
 * khó với nhất trên máy màn hình lớn. Ngoài ra menu hamburger giấu mọi thứ
 * sau một cú bấm; thanh đáy cho thấy luôn app có những gì.
 *
 * <b>Vì sao đúng 4 mục?</b> Mỗi ô phải rộng tối thiểu 44px và còn chỗ cho
 * nhãn chữ tiếng Việt có dấu. Trên máy hẹp (360px) thì 5 mục là chữ bắt đầu
 * bị cắt. Thà 4 mục dùng hằng ngày, phần còn lại vào trong Trang chủ.
 */

interface MucDieuHuong {
  duongDan: string
  nhan: string
  icon: TenBieuTuong
}

// Thứ tự theo tần suất dùng thật, không theo thứ tự sprint: mỗi Chủ Nhật
// huynh trưởng mở app ra là để điểm danh.
const CAC_MUC: MucDieuHuong[] = [
  { duongDan: '/', nhan: 'Trang chủ', icon: 'trang-chu' },
  { duongDan: '/truc-cong', nhan: 'Trực cổng', icon: 'diem-danh' },
  { duongDan: '/thieu-nhi', nhan: 'Thiếu nhi', icon: 'thieu-nhi' },
  { duongDan: '/lop-hoc', nhan: 'Lớp học', icon: 'lop-hoc' },
]

export function ThanhDieuHuongDuoi() {
  return (
    <nav
      aria-label="Điều hướng chính"
      className="sticky bottom-0 z-10 border-t border-slate-200 bg-white/95
                 backdrop-blur pb-[env(safe-area-inset-bottom)]"
    >
      <ul className="mx-auto flex max-w-lg">
        {CAC_MUC.map((muc) => (
          <li key={muc.duongDan} className="flex-1">
            <NavLink
              to={muc.duongDan}
              // `end` chỉ đặt cho đường dẫn gốc "/". Không có nó thì "/" khớp
              // với MỌI đường dẫn (vì mọi đường dẫn đều bắt đầu bằng "/"),
              // và Trang chủ sẽ luôn sáng dù đang ở màn hình khác.
              end={muc.duongDan === '/'}
              className={({ isActive }) =>
                [
                  'nut-cham flex flex-col items-center gap-0.5 py-2',
                  'text-[11px] font-medium transition-colors',
                  isActive
                    ? 'text-xudoan-600'
                    : 'text-slate-500 active:text-slate-700',
                ].join(' ')
              }
            >
              {({ isActive }) => (
                <>
                  <BieuTuong
                    ten={muc.icon}
                    className={isActive ? 'h-6 w-6' : 'h-6 w-6 opacity-70'}
                  />
                  {muc.nhan}
                </>
              )}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}

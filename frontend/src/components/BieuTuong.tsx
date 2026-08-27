/**
 * Bộ biểu tượng của ứng dụng.
 *
 * Vì sao tự vẽ mà không cài thư viện icon (lucide, heroicons)?
 * Ở giai đoạn này ta cần đúng 6 icon. Một thư viện icon kéo theo vài trăm KB
 * dependency và một lớp API phải học, đổi lấy thứ ta gõ tay trong 20 dòng.
 * Khi nào cần tới ~20 icon thì hẵng cài — lúc đó nó mới đáng.
 *
 * Tất cả đều vẽ bằng nét (stroke) chứ không tô đặc, và dùng `currentColor`
 * nên icon tự đổi màu theo màu chữ của thẻ cha. Nhờ vậy một icon dùng được
 * cả ở trạng thái thường lẫn trạng thái đang chọn mà không cần biến thể.
 */

const DUONG_VE = {
  /** Ngôi nhà — Trang chủ */
  'trang-chu': (
    <>
      <path d="M3 10.5 12 3l9 7.5" />
      <path d="M5.5 9.5V20a1 1 0 0 0 1 1h11a1 1 0 0 0 1-1V9.5" />
    </>
  ),

  /** Ô vuông có dấu tích — Điểm danh */
  'diem-danh': (
    <>
      <rect x="4" y="5" width="16" height="15" rx="2" />
      <path d="M4 9h16" />
      <path d="m8.5 14 2.5 2.5 4.5-5" />
    </>
  ),

  /** Hai người — Thiếu nhi */
  'thieu-nhi': (
    <>
      <circle cx="9.5" cy="8" r="3.2" />
      <path d="M4 20c0-3 2.5-5.5 5.5-5.5S15 17 15 20" />
      <circle cx="17.5" cy="9.5" r="2.3" />
      <path d="M16.5 14.6c2.2.4 3.5 2.5 3.5 5.4" />
    </>
  ),

  /** Bảng trên chân — Lớp học */
  'lop-hoc': (
    <>
      <rect x="3.5" y="4" width="17" height="12" rx="1.5" />
      <path d="M7.5 8.5h9M7.5 12h5" />
      <path d="M12 16v4M8.5 20h7" />
    </>
  ),

  /** Nhịp tim — Kiểm tra hệ thống */
  'nhip-tim': <path d="M3 12h3.5L9 6l4 12 2.5-6H21" />,

  /** Mũi tên sang phải — nút đi tiếp */
  'mui-ten-phai': (
    <>
      <path d="M5 12h14" />
      <path d="m13 6 6 6-6 6" />
    </>
  ),
} as const

/** Tên icon hợp lệ. TypeScript sẽ báo lỗi nếu gõ sai tên. */
export type TenBieuTuong = keyof typeof DUONG_VE

export function BieuTuong({
  ten,
  className = 'h-6 w-6',
}: {
  ten: TenBieuTuong
  className?: string
}) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      // Icon ở đây luôn đi kèm nhãn chữ, nên với trình đọc màn hình nó là
      // trang trí. Khai báo aria-hidden để nó không đọc thừa.
      aria-hidden="true"
    >
      {DUONG_VE[ten]}
    </svg>
  )
}

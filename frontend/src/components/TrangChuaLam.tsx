import { Link } from 'react-router-dom'

/**
 * Trang giữ chỗ cho màn hình chưa được xây.
 *
 * <b>Vì sao không dựng giao diện giả với dữ liệu bịa?</b>
 * Giao diện giả trông y như thật, và sau vài tuần chính ta cũng quên mất
 * màn hình nào là thật màn hình nào là vỏ. Tệ hơn: nếu có người trong ban
 * điều hành xem thử, họ sẽ tưởng phần đó đã xong. Một trang nói thẳng
 * "chưa làm, thuộc Sprint mấy" thì không ai hiểu nhầm được.
 */
export function TrangChuaLam({
  tenManHinh,
  sprint,
  moTa,
}: {
  tenManHinh: string
  sprint: number
  moTa: string
}) {
  return (
    <div className="rounded-xl border border-dashed border-slate-300 bg-white p-6 text-center">
      <span
        className="inline-block rounded-full bg-slate-100 px-3 py-1 text-xs
                   font-semibold text-slate-500"
      >
        Sprint {sprint}
      </span>

      <h2 className="mt-3 text-lg font-semibold text-slate-800">{tenManHinh}</h2>

      <p className="mt-2 text-sm leading-relaxed text-slate-500">{moTa}</p>

      <p className="mt-4 text-sm text-slate-400">Màn hình này chưa được xây.</p>

      <Link
        to="/"
        className="nut-cham mt-5 inline-flex items-center justify-center rounded-lg
                   bg-slate-100 px-4 text-sm font-medium text-slate-700
                   active:bg-slate-200"
      >
        Về trang chủ
      </Link>
    </div>
  )
}

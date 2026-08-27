import { Link, useLocation } from 'react-router-dom'

/**
 * Trang 404.
 *
 * <p>Trước đây route <code>*</code> lặng lẽ chuyển hướng về trang chủ. Nghe
 * thì thân thiện, thực ra có hại: người gõ sai địa chỉ tưởng mình bấm nhầm
 * nút, còn ta thì không bao giờ phát hiện ra một đường link hỏng đã gửi cho
 * nhau qua Zalo. Nói thẳng "không có trang này" rồi chỉ đường về là trung
 * thực hơn.
 */
export function TrangKhongTimThay() {
  const viTri = useLocation()

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-8 text-center">
      <p className="text-3xl font-bold text-slate-300">404</p>

      <h2 className="mt-2 font-semibold text-slate-800">Không có trang này</h2>

      <p className="mt-2 break-all text-sm text-slate-500">
        Đường dẫn <code className="text-slate-600">{viTri.pathname}</code> không
        tồn tại trong hệ thống.
      </p>

      <Link
        to="/"
        className="nut-cham mt-5 inline-flex items-center justify-center rounded-lg
                   bg-xudoan-600 px-5 text-sm font-medium text-white active:bg-xudoan-700"
      >
        Về trang chủ
      </Link>
    </div>
  )
}

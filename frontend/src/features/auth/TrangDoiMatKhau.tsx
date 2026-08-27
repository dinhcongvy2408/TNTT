import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from './authContext'
import { authService } from '@/services/authService'
import { ApiError } from '@/services/types'

/**
 * Đổi mật khẩu.
 *
 * Màn hình này phục vụ hai tình huống:
 *   1. Bắt buộc — tài khoản mới, `canDoiMatKhau = true`. Backend chặn mọi
 *      endpoint khác cho tới khi đổi xong, nên không lách được bằng cách gõ
 *      thẳng URL.
 *   2. Tự nguyện — người dùng vào từ menu.
 */
export function TrangDoiMatKhau() {
  const { nguoiDung, dangXuat } = useAuth()
  const dieuHuong = useNavigate()
  const batBuoc = nguoiDung?.canDoiMatKhau ?? false

  const [matKhauCu, datMatKhauCu] = useState('')
  const [matKhauMoi, datMatKhauMoi] = useState('')
  const [nhapLai, datNhapLai] = useState('')
  const [loi, datLoi] = useState<string | null>(null)
  const [dangGui, datDangGui] = useState(false)
  const [xong, datXong] = useState(false)

  async function guiForm(e: React.FormEvent) {
    e.preventDefault()
    datLoi(null)

    // Kiểm tra hai ô khớp nhau ở FRONTEND vì backend không thể làm việc này:
    // nó chỉ nhận một mật khẩu mới, còn "gõ lại cho chắc" là chuyện của giao diện.
    if (matKhauMoi !== nhapLai) {
      datLoi('Hai ô mật khẩu mới không khớp nhau')
      return
    }

    datDangGui(true)
    try {
      await authService.doiMatKhau(matKhauCu, matKhauMoi)
      datXong(true)
    } catch (ex) {
      datLoi(ex instanceof ApiError ? ex.message : 'Đổi mật khẩu thất bại')
    } finally {
      datDangGui(false)
    }
  }

  if (xong) {
    return (
      <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-6 text-center">
        <p className="font-semibold text-emerald-800">Đã đổi mật khẩu</p>
        <p className="mt-2 text-sm leading-relaxed text-emerald-700">
          Mọi phiên đăng nhập trên các thiết bị khác đã bị thu hồi. Hãy đăng
          nhập lại bằng mật khẩu mới.
        </p>
        <button
          type="button"
          onClick={() => void dangXuat().then(() => dieuHuong('/dang-nhap'))}
          className="nut-cham mt-4 rounded-lg bg-xudoan-600 px-5 text-sm font-semibold text-white"
        >
          Đăng nhập lại
        </button>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {batBuoc && (
        <p className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-sm text-amber-800">
          Đây là mật khẩu tạm do ban điều hành cấp. Bạn phải đổi trước khi dùng
          hệ thống.
        </p>
      )}

      <form
        onSubmit={guiForm}
        className="space-y-4 rounded-xl border border-slate-200 bg-white p-5"
      >
        <h2 className="font-semibold text-slate-800">Đổi mật khẩu</h2>

        <O nhan="Mật khẩu hiện tại" giaTri={matKhauCu} datGiaTri={datMatKhauCu} tuDien="current-password" />
        <O nhan="Mật khẩu mới" giaTri={matKhauMoi} datGiaTri={datMatKhauMoi} tuDien="new-password" />
        <O nhan="Nhập lại mật khẩu mới" giaTri={nhapLai} datGiaTri={datNhapLai} tuDien="new-password" />

        <p className="text-xs text-slate-400">Mật khẩu mới phải từ 8 ký tự trở lên.</p>

        {loi && <p className="rounded-lg bg-red-50 p-2.5 text-sm text-red-700">{loi}</p>}

        <button
          type="submit"
          disabled={dangGui || !matKhauCu || !matKhauMoi || !nhapLai}
          className="nut-cham w-full rounded-lg bg-xudoan-600 px-4 text-sm font-semibold
                     text-white active:bg-xudoan-700 disabled:opacity-50"
        >
          {dangGui ? 'Đang lưu…' : 'Đổi mật khẩu'}
        </button>
      </form>
    </div>
  )
}

function O({
  nhan,
  giaTri,
  datGiaTri,
  tuDien,
}: {
  nhan: string
  giaTri: string
  datGiaTri: (v: string) => void
  tuDien: string
}) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-slate-700">{nhan}</span>
      <input
        type="password"
        value={giaTri}
        autoComplete={tuDien}
        onChange={(e) => datGiaTri(e.target.value)}
        className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none
                   focus:ring-2 focus:ring-xudoan-500/40"
      />
    </label>
  )
}

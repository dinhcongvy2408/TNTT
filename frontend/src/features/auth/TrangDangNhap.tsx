import { useState } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './authContext'
import { ApiError } from '@/services/types'

/**
 * Màn hình đăng nhập.
 *
 * Nằm NGOÀI bố cục chính: chưa đăng nhập thì chưa được đi đâu, nên thanh
 * điều hướng dưới đáy không có lý do tồn tại ở đây.
 */
export function TrangDangNhap() {
  const { trangThai, dangNhap } = useAuth()
  const viTri = useLocation()
  const [dinhDanh, datDinhDanh] = useState('')
  const [matKhau, datMatKhau] = useState('')
  const [loi, datLoi] = useState<string | null>(null)
  const [dangGui, datDangGui] = useState(false)

  if (trangThai === 'da-dang-nhap') {
    // Quay lại đúng trang người dùng định vào trước khi bị chặn.
    const quayVe = (viTri.state as { tuDau?: string } | null)?.tuDau ?? '/'
    return <Navigate to={quayVe} replace />
  }

  async function guiForm(e: React.FormEvent) {
    e.preventDefault()
    datLoi(null)
    datDangGui(true)
    try {
      await dangNhap(dinhDanh.trim(), matKhau)
    } catch (ex) {
      datLoi(
        ex instanceof ApiError ? ex.message : 'Đăng nhập thất bại, thử lại sau',
      )
    } finally {
      datDangGui(false)
    }
  }

  return (
    <main className="flex min-h-dvh flex-col justify-center px-5 py-10">
      <div className="mx-auto w-full max-w-sm">
        <header className="mb-8 text-center">
          <h1 className="text-xl font-bold text-xudoan-700">
            Xứ đoàn Thiếu Nhi Thánh Thể
          </h1>
          <p className="mt-1 text-sm text-slate-500">Hệ thống quản lý</p>
        </header>

        <form
          onSubmit={guiForm}
          className="space-y-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm"
        >
          <label className="block">
            <span className="mb-1 block text-sm font-medium text-slate-700">
              Email hoặc số điện thoại
            </span>
            <input
              value={dinhDanh}
              onChange={(e) => datDinhDanh(e.target.value)}
              autoComplete="username"
              // inputMode="email" cho bàn phím điện thoại hiện sẵn ký tự @
              inputMode="email"
              autoCapitalize="none"
              className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none
                         focus:ring-2 focus:ring-xudoan-500/40"
            />
          </label>

          <label className="block">
            <span className="mb-1 block text-sm font-medium text-slate-700">
              Mật khẩu
            </span>
            <input
              type="password"
              value={matKhau}
              onChange={(e) => datMatKhau(e.target.value)}
              autoComplete="current-password"
              className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none
                         focus:ring-2 focus:ring-xudoan-500/40"
            />
          </label>

          {loi && (
            <p className="rounded-lg bg-red-50 p-2.5 text-sm text-red-700">{loi}</p>
          )}

          <button
            type="submit"
            disabled={dangGui || !dinhDanh || !matKhau}
            className="nut-cham w-full rounded-lg bg-xudoan-600 px-4 text-sm font-semibold
                       text-white active:bg-xudoan-700 disabled:opacity-50"
          >
            {dangGui ? 'Đang đăng nhập…' : 'Đăng nhập'}
          </button>
        </form>

        <p className="mt-6 text-center text-xs leading-relaxed text-slate-400">
          Quên mật khẩu thì liên hệ ban điều hành để được cấp lại.
        </p>
      </div>
    </main>
  )
}

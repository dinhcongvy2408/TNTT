import axios, { AxiosError, type AxiosInstance } from 'axios'
import { ApiError, type ApiResponse } from './types'

/**
 * Client HTTP duy nhất của ứng dụng.
 *
 * CLAUDE.md mục 5: "Gọi API qua một lớp services/ duy nhất, không fetch rải
 * rác trong component". Đây chính là lớp đó. Component KHÔNG import axios.
 *
 * Ba việc file này gánh, để nơi khác khỏi phải làm lại:
 *   1. Bóc lớp vỏ ApiResponse — nơi gọi nhận thẳng `data`.
 *   2. Chuẩn hoá mọi kiểu lỗi về một lớp ApiError duy nhất.
 *   3. Gắn/gỡ access token (Sprint 1).
 */

const client: AxiosInstance = axios.create({
  // Rỗng ở dev → đường dẫn tương đối "/api/v1/..." → Vite proxy lo phần còn lại.
  // Trên Vercel thì đặt VITE_API_BASE_URL = https://api.tenmien.vn
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',

  // Cho phép gửi kèm cookie (refresh token ở Sprint 1)
  withCredentials: true,

  // Mạng ở nhà thờ chậm, nhưng chờ quá 20 giây thì huynh trưởng đã bấm lại rồi.
  timeout: 20_000,

  headers: { 'Content-Type': 'application/json' },
})

// ---------------------------------------------------------------------
// Interceptor request — gắn access token
// ---------------------------------------------------------------------

let accessToken: string | null = null

/**
 * Sprint 1 sẽ gọi hàm này sau khi đăng nhập thành công.
 *
 * Token nằm trong BIẾN JAVASCRIPT, cố ý không dùng localStorage: bất kỳ
 * đoạn script nào bị chèn vào trang (XSS) cũng đọc được localStorage và
 * lấy mất token. Biến trong module thì mất khi F5 — nhưng đó lại đúng, vì
 * ta sẽ lấy token mới bằng refresh-token cookie (HttpOnly, JS không đọc được).
 */
export function datAccessToken(token: string | null): void {
  accessToken = token
}

client.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

// ---------------------------------------------------------------------
// Interceptor response — chuẩn hoá lỗi
// ---------------------------------------------------------------------

client.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    // Không có response = không tới được server (mất mạng / server chưa bật)
    if (!error.response) {
      return Promise.reject(
        new ApiError(
          'Không kết nối được tới máy chủ. Kiểm tra lại đường truyền.',
          0,
        ),
      )
    }

    const { status, data } = error.response
    return Promise.reject(
      new ApiError(
        data?.message ?? 'Đã có lỗi xảy ra',
        status,
        data?.errorCode,
        data?.fieldErrors,
      ),
    )
  },
)

// ---------------------------------------------------------------------
// Hàm gọi API — đã bóc sẵn lớp vỏ ApiResponse
// ---------------------------------------------------------------------

/**
 * Lấy `data` ra khỏi vỏ ApiResponse.
 *
 * Backend có thể trả success = true kèm data = null (VD: sau khi xoá).
 * Ở đây ta coi đó là lỗi, vì các hàm bên dưới hứa trả về T. Endpoint nào
 * không có dữ liệu trả về thì dùng `del`/`post` với T = void.
 */
function bocData<T>(body: ApiResponse<T>): T {
  if (!body.success) {
    throw new ApiError(body.message ?? 'Yêu cầu thất bại', 400, body.errorCode)
  }
  return body.data as T
}

export const http = {
  async get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
    const res = await client.get<ApiResponse<T>>(url, { params })
    return bocData(res.data)
  },

  async post<T>(url: string, body?: unknown): Promise<T> {
    const res = await client.post<ApiResponse<T>>(url, body)
    return bocData(res.data)
  },

  async put<T>(url: string, body?: unknown): Promise<T> {
    const res = await client.put<ApiResponse<T>>(url, body)
    return bocData(res.data)
  },

  async patch<T>(url: string, body?: unknown): Promise<T> {
    const res = await client.patch<ApiResponse<T>>(url, body)
    return bocData(res.data)
  },

  async del<T>(url: string): Promise<T> {
    const res = await client.delete<ApiResponse<T>>(url)
    return bocData(res.data)
  },
}

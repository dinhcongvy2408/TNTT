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

  // KHÔNG đặt Content-Type ở đây. Header khai báo tại instance áp cho MỌI
  // request, kể cả khi body là FormData — lúc đó axios lẽ ra phải tự đặt
  // 'multipart/form-data; boundary=...', mà boundary là thứ server dùng để
  // tách các phần của file. Đè lên nó thì Sprint 4 import Excel sẽ hỏng với
  // một lỗi rất khó truy. Body là object thường thì axios đã tự đặt
  // application/json rồi, ta không cần làm gì.
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

/**
 * Đọc access token hiện tại.
 *
 * Chỉ dùng cho WebSocket: trình duyệt không cho đặt header HTTP khi mở
 * WebSocket, nên token phải đi trong khung STOMP CONNECT và lớp đó cần tự lấy.
 * Mọi lời gọi HTTP thông thường đã có interceptor gắn hộ, KHÔNG gọi hàm này.
 */
export function layAccessToken(): string | null {
  return accessToken
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

// ---------------------------------------------------------------------
// Tự làm mới access token khi gặp 401
// ---------------------------------------------------------------------

/**
 * Access token chỉ sống 30 phút. Không có cơ chế này thì cứ nửa tiếng một
 * lần, huynh trưởng đang điểm danh dở lại bị đá về màn hình đăng nhập.
 *
 * Cách chạy: gặp 401 → gọi /auth/refresh (cookie tự đính kèm) → lấy token
 * mới → gửi lại đúng request vừa hỏng. Người dùng không thấy gì cả.
 */

/** Nơi ứng dụng đăng ký hành động "phiên đã chết thật, cho về đăng nhập". */
let khiPhienHetHan: (() => void) | null = null

export function datXuLyPhienHetHan(xuLy: (() => void) | null): void {
  khiPhienHetHan = xuLy
}

/**
 * Lời hứa của lần làm mới ĐANG chạy, hoặc null.
 *
 * Vì sao cần biến này? Màn hình điểm danh có thể bắn 5 request cùng lúc.
 * Nếu token vừa hết hạn thì cả 5 cùng nhận 401 và cùng gọi /auth/refresh.
 * Backend có XOAY VÒNG token: lần refresh đầu tiên thu hồi token cũ, nên 4
 * lần còn lại thất bại và người dùng bị đăng xuất oan.
 *
 * Giữ chung một lời hứa thì 5 request cùng chờ MỘT lần làm mới.
 */
let dangLamMoi: Promise<string> | null = null

async function lamMoiToken(): Promise<string> {
  dangLamMoi ??= client
    .post<ApiResponse<{ accessToken: string }>>('/api/v1/auth/refresh')
    .then((res) => {
      const token = res.data.data?.accessToken
      if (!res.data.success || !token) {
        throw new ApiError('Không làm mới được phiên', 401)
      }
      accessToken = token
      return token
    })
    .finally(() => {
      dangLamMoi = null
    })
  return dangLamMoi
}

/** Các endpoint KHÔNG được thử làm mới, tránh vòng lặp vô tận. */
const KHONG_TU_LAM_MOI = ['/api/v1/auth/refresh', '/api/v1/auth/login']

client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
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
    const config = error.config as (typeof error.config & { daThuLai?: boolean }) | undefined

    // `daThuLai` chặn vòng lặp: nếu request đã gửi lại một lần mà vẫn 401 thì
    // vấn đề không nằm ở token hết hạn nữa.
    if (
      status === 401 &&
      config &&
      !config.daThuLai &&
      !KHONG_TU_LAM_MOI.some((duong) => config.url?.includes(duong))
    ) {
      config.daThuLai = true
      try {
        await lamMoiToken()
        return await client.request(config)
      } catch {
        // Refresh token cũng chết: phiên hết hạn thật.
        accessToken = null
        khiPhienHetHan?.()
      }
    }

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
 * Kiểm tra cờ `success` trong vỏ ApiResponse.
 *
 * Cần thiết vì HTTP 200 chưa chắc là thành công về mặt nghiệp vụ: theo
 * docs/04-api.md, backend có thể trả 200 kèm `success: false`.
 */
function kiemTraThanhCong(body: ApiResponse<unknown>): void {
  if (!body.success) {
    throw new ApiError(body.message ?? 'Yêu cầu thất bại', 400, body.errorCode)
  }
}

/**
 * Bóc `data` cho endpoint CÓ trả dữ liệu.
 *
 * `data` rỗng ở đây là vi phạm hợp đồng API, nên ta ném lỗi thay vì trả
 * null. Lý do: hàm này hứa trả về `T`. Nếu nó lặng lẽ trả null rồi ép kiểu
 * `as T`, TypeScript sẽ tin là có dữ liệu và lỗi chỉ nổ sau đó vài lớp, ở
 * một dòng `health.database` chẳng liên quan gì — đúng loại lỗi mà strict
 * mode sinh ra để chặn, và một dấu `as` là đủ vô hiệu hoá nó.
 *
 * Endpoint không có dữ liệu trả về thì dùng `httpVoid` bên dưới.
 */
function bocData<T>(body: ApiResponse<T>): T {
  kiemTraThanhCong(body)
  if (body.data === null || body.data === undefined) {
    throw new ApiError(
      'Máy chủ không trả về dữ liệu cho một yêu cầu đáng lẽ phải có',
      500,
      'EMPTY_DATA',
    )
  }
  return body.data
}

/** Gọi API và mong đợi có dữ liệu trả về. */
export const http = {
  async get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
    const res = await client.get<ApiResponse<T>>(url, { params })
    return bocData(res.data)
  },

  /**
   * Cho endpoint mà `data` rỗng là câu trả lời HỢP LỆ, không phải lỗi.
   *
   * VD: GET /nam-hoc/hien-tai trả rỗng khi chưa kích hoạt năm học nào —
   * hệ thống mới cài thì đó là trạng thái bình thường, không phải 404.
   *
   * Tách riêng khỏi `get` là có chủ đích: kiểu trả về `T | null` buộc nơi
   * gọi phải xử lý trường hợp rỗng, TypeScript không cho quên.
   */
  async getNullable<T>(
    url: string,
    params?: Record<string, unknown>,
  ): Promise<T | null> {
    const res = await client.get<ApiResponse<T>>(url, { params })
    kiemTraThanhCong(res.data)
    return res.data.data ?? null
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

/**
 * Gọi API KHÔNG mong đợi dữ liệu trả về: xoá, đổi trạng thái, xác nhận
 * phiếu ra cổng... Backend trả `ApiResponse.ok(message)` với `data` rỗng.
 *
 * Tách riêng khỏi `http` là có chủ đích: nhìn vào lời gọi là biết ngay
 * endpoint đó có dữ liệu hay không, và `http` giữ được lời hứa "trả về T"
 * mà không phải nói dối kiểu.
 */
export const httpVoid = {
  async post(url: string, body?: unknown): Promise<void> {
    const res = await client.post<ApiResponse<unknown>>(url, body)
    kiemTraThanhCong(res.data)
  },

  async put(url: string, body?: unknown): Promise<void> {
    const res = await client.put<ApiResponse<unknown>>(url, body)
    kiemTraThanhCong(res.data)
  },

  async patch(url: string, body?: unknown): Promise<void> {
    const res = await client.patch<ApiResponse<unknown>>(url, body)
    kiemTraThanhCong(res.data)
  },

  async del(url: string): Promise<void> {
    const res = await client.delete<ApiResponse<unknown>>(url)
    kiemTraThanhCong(res.data)
  },
}

/**
 * Các kiểu dùng chung khi giao tiếp với backend.
 * Phải khớp với ApiResponse / PageResponse bên Java (docs/04-api.md).
 */

/** Vỏ bọc chuẩn của mọi response. */
export interface ApiResponse<T> {
  success: boolean
  data: T | null
  message: string | null
  errorCode?: string
  /** Lỗi theo từng field, dùng để bôi đỏ ô nhập trên form. */
  fieldErrors?: Record<string, string>
}

/** Dạng phân trang trả về từ backend. */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

/**
 * Lỗi API đã được chuẩn hoá.
 *
 * Vì sao tự định nghĩa lớp lỗi thay vì ném thẳng AxiosError?
 * Vì component không nên phải biết ta đang dùng axios hay fetch. Nếu mai
 * này đổi thư viện HTTP, chỉ file services/ phải sửa — mọi màn hình vẫn
 * bắt đúng `ApiError` như cũ.
 */
export class ApiError extends Error {
  /** HTTP status. 0 nghĩa là không nối được tới server. */
  readonly status: number
  readonly errorCode?: string
  readonly fieldErrors?: Record<string, string>

  // Viết field tường minh thay vì "constructor(readonly status: number)".
  // Cú pháp rút gọn đó bị cấm bởi cờ erasableSyntaxOnly của Vite: nó SINH RA
  // code lúc biên dịch chứ không chỉ xoá kiểu đi, nên không dùng được khi
  // trình chạy tự bóc kiểu (Node --experimental-strip-types, esbuild).
  constructor(
    message: string,
    status: number,
    errorCode?: string,
    fieldErrors?: Record<string, string>,
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.errorCode = errorCode
    this.fieldErrors = fieldErrors
  }

  /** Mất mạng, server chưa bật, hoặc bị CORS chặn. */
  get laLoiKetNoi(): boolean {
    return this.status === 0
  }

  /** Hết hạn đăng nhập — dùng ở Sprint 1 để tự gọi refresh token. */
  get laChuaDangNhap(): boolean {
    return this.status === 401
  }
}

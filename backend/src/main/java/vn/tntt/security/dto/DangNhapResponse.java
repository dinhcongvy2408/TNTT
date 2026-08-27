package vn.tntt.security.dto;

/**
 * Trả về sau khi đăng nhập hoặc làm mới token.
 *
 * <p>KHÔNG chứa refresh token: nó đi bằng cookie HttpOnly, JavaScript không
 * đọc được. Nếu trả trong body thì frontend phải cất đâu đó, và mọi chỗ cất
 * được bằng JavaScript đều đọc được bằng JavaScript — tức là đọc được bởi
 * bất kỳ đoạn script nào lọt vào trang (XSS).
 *
 * @param accessToken   token đính vào header Authorization
 * @param hetHanSauGiay còn bao nhiêu giây nữa hết hạn, để frontend chủ động
 *                      làm mới trước khi người dùng gặp lỗi 401
 * @param nguoiDung     thông tin để hiển thị ngay, khỏi gọi thêm /auth/me
 */
public record DangNhapResponse(
        String accessToken,
        long hetHanSauGiay,
        ThongTinToiResponse nguoiDung
) {
}

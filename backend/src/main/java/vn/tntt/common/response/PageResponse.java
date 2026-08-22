package vn.tntt.common.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Dạng phân trang rút gọn trả ra cho frontend.
 *
 * <p>Vì sao không trả thẳng {@link Page} của Spring Data? Vì JSON của nó
 * chứa cả cấu trúc {@code pageable}, {@code sort} lồng nhiều tầng, thay đổi
 * giữa các version Spring — frontend sẽ vỡ khi ta nâng version. Tự định nghĩa
 * hợp đồng của mình thì ta kiểm soát được.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    /** Chuyển một {@link Page} entity sang {@code PageResponse} DTO. */
    public static <E, D> PageResponse<D> from(Page<E> page, Function<E, D> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}

package vn.tntt.organization.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tntt.common.exception.BusinessRuleException;
import vn.tntt.common.exception.ConflictException;
import vn.tntt.organization.dto.NganhResponse;
import vn.tntt.organization.dto.TaoNganhRequest;
import vn.tntt.organization.entity.Nganh;
import vn.tntt.organization.repository.NganhRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NganhService {

    private final NganhRepository nganhRepository;

    @Transactional(readOnly = true)
    public List<NganhResponse> layTatCa() {
        return nganhRepository.findAllByOrderByThuTuAsc()
                .stream()
                .map(NganhResponse::tu)
                .toList();
    }

    @Transactional
    public NganhResponse taoMoi(TaoNganhRequest request) {
        if (request.tuoiToiDa() < request.tuoiToiThieu()) {
            throw new BusinessRuleException(
                    "Tuổi tối đa phải lớn hơn hoặc bằng tuổi tối thiểu",
                    "KHOANG_TUOI_KHONG_HOP_LE");
        }
        if (nganhRepository.existsByMaNganh(request.maNganh())) {
            throw new ConflictException(
                    "Mã ngành %s đã tồn tại".formatted(request.maNganh()),
                    "MA_NGANH_DA_TON_TAI");
        }
        if (nganhRepository.existsByTenNganh(request.tenNganh())) {
            throw new ConflictException(
                    "Tên ngành %s đã tồn tại".formatted(request.tenNganh()),
                    "TEN_NGANH_DA_TON_TAI");
        }
        // thu_tu quyết định đường đi của việc chuyển cấp (docs/02 mục 6).
        // Trùng thứ tự thì thuật toán chuyển cấp không biết đẩy các em sang
        // ngành nào — hỏng âm thầm, tận cuối năm học mới phát hiện.
        if (nganhRepository.existsByThuTu(request.thuTu())) {
            throw new ConflictException(
                    "Đã có ngành mang thứ tự %d".formatted(request.thuTu()),
                    "THU_TU_DA_TON_TAI");
        }

        Nganh nganh = new Nganh();
        nganh.setTenNganh(request.tenNganh());
        nganh.setMaNganh(request.maNganh());
        nganh.setTuoiToiThieu(request.tuoiToiThieu());
        nganh.setTuoiToiDa(request.tuoiToiDa());
        nganh.setThuTu(request.thuTu());

        Nganh daLuu = nganhRepository.save(nganh);
        log.info("Đã tạo ngành {}", daLuu.getMaNganh());
        return NganhResponse.tu(daLuu);
    }
}

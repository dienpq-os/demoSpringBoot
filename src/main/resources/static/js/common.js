// =============================================
// common.js - Script chung cho toàn bộ dự án
// =============================================

document.addEventListener('DOMContentLoaded', function() {

    // ==================== TỰ ĐỘNG ẨN THÔNG BÁO ====================
    const successAlert = document.getElementById('successAlert');
    if (successAlert) {
        setTimeout(() => {
            successAlert.style.transition = 'opacity 0.5s ease';
            successAlert.style.opacity = '0';
            setTimeout(() => {
                successAlert.style.display = 'none';
            }, 600);
        }, 5000);
    }

    const errorAlert = document.getElementById('errorAlert');
    if (errorAlert) {
        setTimeout(() => {
            errorAlert.style.transition = 'opacity 0.5s ease';
            errorAlert.style.opacity = '0';
            setTimeout(() => {
                errorAlert.style.display = 'none';
            }, 600);
        }, 7000);
    }

    // ==================== HỘP THOẠI XÁC NHẬN XÓA ====================
    const confirmDialog = document.getElementById('confirmDialog');
    const confirmCancel = document.getElementById('confirmCancel');
    const confirmOk = document.getElementById('confirmOk');

    let currentDeleteUrl = '';

    // Hàm hiển thị hộp thoại xác nhận xóa
    window.showConfirmDelete = function(element) {
        const maSP = element.getAttribute('data-maSP');
        const tenModel = element.getAttribute('data-tenModel');

        const message = `Bạn có chắc chắn muốn xóa điện thoại <strong>${tenModel}</strong> (Mã: ${maSP}) không?`;

        document.getElementById('confirmMessage').innerHTML = message;
        confirmDialog.style.display = 'flex';

        currentDeleteUrl = `/products/delete/${maSP}`;
    };

    // Nút Hủy
    if (confirmCancel) {
        confirmCancel.addEventListener('click', function() {
            confirmDialog.style.display = 'none';
        });
    }

    // Nút Xóa
    if (confirmOk) {
        confirmOk.addEventListener('click', function() {
            if (currentDeleteUrl) {
                window.location.href = currentDeleteUrl;
            }
        });
    }
});
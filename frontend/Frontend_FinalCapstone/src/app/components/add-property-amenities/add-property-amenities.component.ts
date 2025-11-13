import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { AmenityService } from '../../services/amenity.service';
import { Amenity, AmenityRequest } from '../../models/amenity';

@Component({
  selector: 'app-add-property-amenities',
  templateUrl: './add-property-amenities.component.html',
  styleUrls: ['./add-property-amenities.component.scss']
})
export class AddPropertyAmenitiesComponent implements OnInit {
  propertyId: number = 0;
  amenities: Amenity[] = [];
  selectedAmenityIds: Set<number> = new Set();
  isLoading: boolean = false;
  isSubmitting: boolean = false;

  // Modal state
  showModal: boolean = false;
  modalTitle: string = '';
  modalMessage: string = '';
  modalType: 'success' | 'error' = 'success';

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private amenityService: AmenityService
  ) {}

  ngOnInit(): void {
    // Get propertyId from query params
    this.route.queryParams.subscribe(params => {
      this.propertyId = +params['propertyId'] || 0;
      if (this.propertyId === 0) {
        this.showErrorModal('Lỗi!', 'Không tìm thấy property ID. Vui lòng thử lại.');
        return;
      }
      this.loadAmenities();
    });
  }

  loadAmenities(): void {
    this.isLoading = true;
    this.amenityService.getAllAmenities().subscribe({
      next: (response: any) => {
        this.isLoading = false;
        if (response && response.code === 200 && response.data) {
          this.amenities = response.data;
        } else {
          this.showErrorModal('Lỗi!', 'Không thể tải danh sách tiện nghi.');
        }
      },
      error: (error: any) => {
        this.isLoading = false;
        console.error('Lỗi khi tải amenities:', error);
        this.showErrorModal('Lỗi kết nối!', 'Không thể tải danh sách tiện nghi. Vui lòng thử lại.');
      }
    });
  }

  toggleAmenity(amenityId: number): void {
    if (this.selectedAmenityIds.has(amenityId)) {
      this.selectedAmenityIds.delete(amenityId);
    } else {
      this.selectedAmenityIds.add(amenityId);
    }
  }

  isSelected(amenityId: number): boolean {
    return this.selectedAmenityIds.has(amenityId);
  }

  getIconUrl(iconUrl: string): string {
    return this.amenityService.getIconUrl(iconUrl);
  }

  canSubmit(): boolean {
    return this.selectedAmenityIds.size > 0 && !this.isSubmitting;
  }

  submitAmenities(): void {
    if (!this.canSubmit()) {
      this.showErrorModal('Lưu ý!', 'Vui lòng chọn ít nhất một tiện nghi.');
      return;
    }

    this.isSubmitting = true;
    const request: AmenityRequest = {
      ids: Array.from(this.selectedAmenityIds),
      idProperty: this.propertyId
    };

    this.amenityService.addAmenitiesToProperty(request).subscribe({
      next: (response: any) => {
        this.isSubmitting = false;
        if (response && response.code === 200) {
          this.showSuccessModal(
            'Thành công!',
            'Đã thêm tiện nghi cho property thành công. Tiếp tục bước tiếp theo.'
          );
        } else {
          const errorMessage = response?.message || 'Không thể thêm tiện nghi. Vui lòng thử lại.';
          this.showErrorModal('Có lỗi xảy ra!', errorMessage);
        }
      },
      error: (error: any) => {
        this.isSubmitting = false;
        console.error('Lỗi khi thêm amenities:', error);
        const errorMessage = error.error?.message || 'Không thể thêm tiện nghi. Vui lòng thử lại.';
        this.showErrorModal('Lỗi kết nối!', errorMessage);
      }
    });
  }

  skip(): void {
    if (confirm('Bạn có muốn bỏ qua bước này không?')) {
      // Navigate to next step (will be implemented later)
      this.router.navigate(['/user-profile']); // Temporary
    }
  }

  goBack(): void {
    if (confirm('Bạn có chắc muốn quay lại? Tiện nghi đã chọn sẽ không được lưu.')) {
      this.router.navigate(['/add-property-images'], {
        queryParams: { propertyId: this.propertyId }
      });
    }
  }

  showSuccessModal(title: string, message: string): void {
    this.modalTitle = title;
    this.modalMessage = message;
    this.modalType = 'success';
    this.showModal = true;
  }

  showErrorModal(title: string, message: string): void {
    this.modalTitle = title;
    this.modalMessage = message;
    this.modalType = 'error';
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    if (this.modalType === 'success') {
      // Navigate to facilities step
      this.router.navigate(['/host/add-property-facilities'], {
        queryParams: { propertyId: this.propertyId }
      });
    }
  }
}

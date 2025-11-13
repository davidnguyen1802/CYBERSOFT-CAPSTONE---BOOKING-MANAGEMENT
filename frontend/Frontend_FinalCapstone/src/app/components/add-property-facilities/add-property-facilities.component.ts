import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FacilityService } from '../../services/facility.service';
import { Facility, FacilityRequest } from '../../models/facility';

@Component({
  selector: 'app-add-property-facilities',
  templateUrl: './add-property-facilities.component.html',
  styleUrls: ['./add-property-facilities.component.scss']
})
export class AddPropertyFacilitiesComponent implements OnInit {
  facilities: Facility[] = [];
  selectedFacilityIds: Set<number> = new Set();
  propertyId: number = 0;
  isLoading: boolean = false;
  isSubmitting: boolean = false;

  // Modal state
  showModal: boolean = false;
  modalTitle: string = '';
  modalMessage: string = '';
  modalType: 'success' | 'error' = 'success';

  constructor(
    private facilityService: FacilityService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.propertyId = params['propertyId'] ? parseInt(params['propertyId']) : 0;
      if (!this.propertyId) {
        this.showErrorModal('Lỗi', 'Không tìm thấy thông tin property. Vui lòng thử lại.');
        setTimeout(() => {
          this.router.navigate(['/host/add-property']);
        }, 2000);
        return;
      }
      this.loadFacilities();
    });
  }

  loadFacilities(): void {
    this.isLoading = true;
    this.facilityService.getAllFacilities().subscribe({
      next: (response) => {
        if (response.code === 200 && response.data) {
          this.facilities = response.data;
        }
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading facilities:', error);
        this.showErrorModal('Lỗi', 'Không thể tải danh sách cơ sở vật chất. Vui lòng thử lại.');
        this.isLoading = false;
      }
    });
  }

  toggleFacility(facilityId: number): void {
    if (this.selectedFacilityIds.has(facilityId)) {
      this.selectedFacilityIds.delete(facilityId);
    } else {
      this.selectedFacilityIds.add(facilityId);
    }
  }

  isFacilitySelected(facilityId: number): boolean {
    return this.selectedFacilityIds.has(facilityId);
  }

  getFacilityIconUrl(iconUrl: string): string {
    return this.facilityService.getIconUrl(iconUrl);
  }

  goBack(): void {
    this.router.navigate(['/host/add-property-amenities'], {
      queryParams: { propertyId: this.propertyId }
    });
  }

  skip(): void {
    // TODO: Navigate to next step
    this.showSuccessModal('Thông báo', 'Đã bỏ qua bước chọn cơ sở vật chất.');
  }

  submit(): void {
    if (this.selectedFacilityIds.size === 0) {
      this.showErrorModal('Lỗi', 'Vui lòng chọn ít nhất 1 cơ sở vật chất.');
      return;
    }

    this.isSubmitting = true;
    const request: FacilityRequest = {
      ids: Array.from(this.selectedFacilityIds),
      propertyId: this.propertyId
    };

    this.facilityService.addFacilitiesToProperty(request).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        if (response.code === 200) {
          this.showSuccessModal('Thành công', 'Đã thêm cơ sở vật chất thành công!');
        } else {
          this.showErrorModal('Lỗi', 'Có lỗi xảy ra. Vui lòng thử lại.');
        }
      },
      error: (error) => {
        console.error('Error adding facilities:', error);
        this.isSubmitting = false;
        this.showErrorModal('Lỗi', 'Không thể thêm cơ sở vật chất. Vui lòng thử lại.');
      }
    });
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
      // TODO: Navigate to next step after success
      console.log('✅ Facilities added successfully. Ready for next step.');
    }
  }
}

import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { PropertyService } from '../../services/property.service';
import { AmenityService } from '../../services/amenity.service';
import { FacilityService } from '../../services/facility.service';
import { CityService } from '../../services/city.service';
import { LocationService } from '../../services/location.service';
import { TokenService } from '../../services/token.service';
import { UserService } from '../../services/user.service';
import { SimpleModalService } from '../../services/simple-modal.service';
import { City } from '../../models/city';
import { Location } from '../../models/location';
import { Amenity } from '../../models/amenity';
import { Facility } from '../../models/facility';
import { PropertyCompleteRequestDTO } from '../../dtos/property/property-complete.request.dto';
import { environment } from '../../../environments/environment';

interface UploadedImage {
  file: File;
  preview: string;
  description: string;
  isMainImage: boolean;
}

@Component({
  selector: 'app-add-property',
  templateUrl: './add-property.component.html',
  styleUrls: ['./add-property.component.scss']
})
export class AddPropertyComponent implements OnInit {
  // Step management
  currentStep: number = 1;
  totalSteps: number = 4;
  
  // Forms
  propertyForm: FormGroup;
  
  // Reference data
  cities: City[] = [];
  locations: Location[] = [];
  allLocations: Location[] = [];
  amenities: Amenity[] = [];
  facilities: Facility[] = [];
  
  // Property types with icons
  propertyTypes = [
    { value: 0, label: 'Apartment', icon: '' },
    { value: 1, label: 'House', icon: '' },
    { value: 2, label: 'Hotel', icon: '' }
  ];
  
  // Step 3: Images data
  uploadedImages: UploadedImage[] = [];
  
  // Step 4: Amenities data
  selectedAmenityIds: Set<number> = new Set();
  
  // Step 5: Facilities data
  selectedFacilityIds: Set<number> = new Set();
  
  // Loading states
  isLoading: boolean = false;
  isSubmitting: boolean = false;
  isLoadingUserId: boolean = true;
  
  // User info
  currentUserId: number = 0;
  
  // Modal state
  showModal: boolean = false;
  modalTitle: string = '';
  modalMessage: string = '';
  modalType: 'success' | 'error' = 'success';
  
  // API base URL
  private baseUrl = (environment.apiBaseUrl || 'http://localhost:8080').replace(/\/$/, '');

  constructor(
    private formBuilder: FormBuilder,
    private router: Router,
    private propertyService: PropertyService,
    private amenityService: AmenityService,
    private facilityService: FacilityService,
    private cityService: CityService,
    private locationService: LocationService,
    private tokenService: TokenService,
    private userService: UserService,
    private modalService: SimpleModalService
  ) {
    // Initialize form with all fields
    this.propertyForm = this.formBuilder.group({
      // Step 1 fields
      propertyName: ['', [Validators.required, Validators.minLength(3)]],
      selectedCity: [null, Validators.required],
      locationId: [null, Validators.required],
      fullAddress: ['', [Validators.required, Validators.minLength(5)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      propertyType: [null, Validators.required],
      
      // Step 2 fields
      pricePerNight: [null, [Validators.required, Validators.min(1)]],
      numberOfBedrooms: [1, [Validators.required, Validators.min(1)]],
      numberOfBathrooms: [1, [Validators.required, Validators.min(1)]],
      maxAdults: [1, [Validators.required, Validators.min(1)]],
      maxChildren: [0, [Validators.required, Validators.min(0)]],
      maxInfants: [0, [Validators.required, Validators.min(0)]],
      maxPets: [0, [Validators.required, Validators.min(0)]]
    });
  }

  ngOnInit(): void {
    // Check authentication
    if (!this.tokenService.getToken()) {
      this.modalService.showError('Vui lòng đăng nhập để thêm property');
      this.router.navigate(['/login']);
      return;
    }

    // Load user info to get hostId
    this.loadUserInfo();
    
    // Load reference data
    this.loadCities();
    this.loadAllLocations();
    this.loadPropertyTypeIcons();
    this.loadAmenities();
    this.loadFacilities();
  }

  // ========================================
  // LOAD REFERENCE DATA
  // ========================================

  loadUserInfo(): void {
    console.log('=======================================');
    console.log('📥 ADD-PROPERTY: Loading User Info');
    console.log('   Method: Decode JWT (NO API CALL)');
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    
    // ✅ REFACTORED: Decode JWT instead of calling API or checking cache
    // This is faster and more reliable than cache lookup
    console.log('🔍 Step 1: Decoding JWT to get userId...');
    const startTime = performance.now();
    const userId = this.tokenService.getUserId();
    const decodeTime = performance.now() - startTime;
    
    if (userId > 0) {
      console.log(`✅ Step 1 SUCCESS: User ID decoded in ${decodeTime.toFixed(2)}ms`);
      console.log('   User ID:', userId);
      this.currentUserId = userId;
      this.isLoadingUserId = false;
      console.log('=======================================');
      console.log('✅ ADD-PROPERTY: User Info Loaded');
      console.log('   currentUserId:', this.currentUserId);
      console.log('=======================================');
    } else {
      console.error('❌ Step 1 FAILED: Unable to get user ID from token');
      console.log('   → Showing alert to user');
      console.log('   → Redirecting to /login');
      this.isLoadingUserId = false;
      console.log('=======================================');
      this.modalService.showError('Không thể xác định user ID. Vui lòng đăng nhập lại.');
      this.router.navigate(['/login']);
    }
  }

  loadCities(): void {
    this.isLoading = true;
    this.cityService.getAll().subscribe({
      next: (response: any) => {
        this.cities = response.data || response || [];
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Error loading cities:', error);
        this.isLoading = false;
      }
    });
  }

  loadAllLocations(): void {
    this.locationService.getAll().subscribe({
      next: (response: any) => {
        this.allLocations = response.data || response || [];
      },
      error: (error: any) => {
        console.error('Error loading locations:', error);
      }
    });
  }

  loadPropertyTypeIcons(): void {
    this.propertyTypes.forEach((type) => {
      type.icon = `${this.baseUrl}/files/property_type_${type.value}.png`;
    });
  }

  loadAmenities(): void {
    this.amenityService.getAllAmenities().subscribe({
      next: (response: any) => {
        if (response && response.code === 200 && response.data) {
          this.amenities = response.data;
        }
      },
      error: (error: any) => {
        console.error('Error loading amenities:', error);
      }
    });
  }

  loadFacilities(): void {
    this.facilityService.getAllFacilities().subscribe({
      next: (response: any) => {
        if (response && response.code === 200 && response.data) {
          this.facilities = response.data;
        }
      },
      error: (error: any) => {
        console.error('Error loading facilities:', error);
      }
    });
  }

  // ========================================
  // STEP 1: BASIC INFO HANDLERS
  // ========================================

  onCityChange(): void {
    const selectedCityName = this.propertyForm.get('selectedCity')?.value;
    if (selectedCityName) {
      this.locations = this.allLocations.filter(
        loc => loc.cityName === selectedCityName
      );
      this.propertyForm.patchValue({ locationId: null });
    } else {
      this.locations = [];
    }
  }

  onLocationChange(locationId: number): void {
    this.propertyForm.patchValue({ locationId });
  }

  selectPropertyType(type: number): void {
    this.propertyForm.patchValue({ propertyType: type });
  }

  // ========================================
  // STEP 2: DETAILS HANDLERS
  // ========================================

  incrementCounter(fieldName: string): void {
    const currentValue = this.propertyForm.get(fieldName)?.value || 0;
    this.propertyForm.patchValue({ [fieldName]: currentValue + 1 });
  }

  decrementCounter(fieldName: string): void {
    const currentValue = this.propertyForm.get(fieldName)?.value || 0;
    const minValue = fieldName.startsWith('max') ? 0 : 1;
    if (currentValue > minValue) {
      this.propertyForm.patchValue({ [fieldName]: currentValue - 1 });
    }
  }

  formatPrice(value: number): string {
    if (!value) return '';
    return new Intl.NumberFormat('vi-VN').format(value);
  }

  onPriceInput(event: any): void {
    const input = event.target;
    let value = input.value.replace(/\D/g, '');
    
    if (value) {
      const numValue = parseInt(value);
      this.propertyForm.patchValue({ pricePerNight: numValue }, { emitEvent: false });
      input.value = this.formatPrice(numValue);
    } else {
      this.propertyForm.patchValue({ pricePerNight: null }, { emitEvent: false });
    }
  }

  onPriceFocus(event: any): void {
    const input = event.target;
    const value = this.propertyForm.get('pricePerNight')?.value;
    if (value) {
      input.value = value.toString();
    }
  }

  onPriceBlur(event: any): void {
    const input = event.target;
    const value = this.propertyForm.get('pricePerNight')?.value;
    if (value) {
      input.value = this.formatPrice(value) + ' VND';
    }
  }

  // ========================================
  // STEP 3: IMAGES HANDLERS
  // ========================================

  onFilesSelected(event: any): void {
    const files: FileList = event.target.files;
    if (!files || files.length === 0) return;

    this.processFiles(files);

    // Reset input
    event.target.value = '';
  }

  // Drag & Drop Handlers
  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    const target = event.currentTarget as HTMLElement;
    target.classList.add('drag-over');
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    const target = event.currentTarget as HTMLElement;
    target.classList.remove('drag-over');
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    const target = event.currentTarget as HTMLElement;
    target.classList.remove('drag-over');

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.processFiles(files);
    }
  }

  // Process uploaded files
  private processFiles(files: FileList): void {
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      
      // Validate file type
      if (!file.type.startsWith('image/')) {
        this.modalService.showError(`File ${file.name} không phải là ảnh`);
        continue;
      }

      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.modalService.showError(`File ${file.name} quá lớn (tối đa 5MB)`);
        continue;
      }

      // Create preview
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.uploadedImages.push({
          file: file,
          preview: e.target.result,
          description: '',
          isMainImage: this.uploadedImages.length === 0 // First image is main
        });
      };
      reader.readAsDataURL(file);
    }
  }

  removeImage(index: number): void {
    if (confirm('Bạn có chắc muốn xóa ảnh này?')) {
      const wasMainImage = this.uploadedImages[index].isMainImage;
      this.uploadedImages.splice(index, 1);
      
      // If we removed the main image, set first image as main
      if (wasMainImage && this.uploadedImages.length > 0) {
        this.uploadedImages[0].isMainImage = true;
      }
    }
  }

  setMainImage(index: number): void {
    this.uploadedImages.forEach((img, i) => {
      img.isMainImage = (i === index);
    });
  }

  // ========================================
  // STEP 4: AMENITIES HANDLERS
  // ========================================

  toggleAmenity(amenityId: number): void {
    if (this.selectedAmenityIds.has(amenityId)) {
      this.selectedAmenityIds.delete(amenityId);
    } else {
      this.selectedAmenityIds.add(amenityId);
    }
  }

  isAmenitySelected(amenityId: number): boolean {
    return this.selectedAmenityIds.has(amenityId);
  }

  getAmenityIconUrl(iconUrl: string): string {
    return this.amenityService.getIconUrl(iconUrl);
  }

  // ========================================
  // STEP 5: FACILITIES HANDLERS
  // ========================================

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

  // ========================================
  // NAVIGATION BETWEEN STEPS
  // ========================================

  nextStep(): void {
    if (this.currentStep === 1 && !this.isStep1Valid()) {
      this.modalService.showError('Vui lòng điền đầy đủ thông tin bắt buộc ở Bước 1');
      return;
    }
    
    if (this.currentStep === 2 && !this.isStep2Valid()) {
      this.modalService.showError('Vui lòng điền đầy đủ thông tin bắt buộc ở Bước 2');
      return;
    }

    if (this.currentStep === 3 && !this.isStep3Valid()) {
      // Button will be disabled, no alert needed
      return;
    }

    if (this.currentStep < this.totalSteps) {
      this.currentStep++;
      window.scrollTo(0, 0);
    }
  }

  previousStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
      window.scrollTo(0, 0);
    }
  }

  goToStep(step: number): void {
    // Check if step is accessible
    if (!this.canAccessStep(step)) {
      // Show warning message (steps are disabled in UI, but keep for safety)
      if (step === 2 && !this.isStep1Valid()) {
        this.modalService.showError('Vui lòng hoàn thành Bước 1 trước khi tiếp tục');
      } else if (step === 3 && (!this.isStep1Valid() || !this.isStep2Valid())) {
        this.modalService.showError('Vui lòng hoàn thành các bước trước đó');
      } else if ((step === 4 || step === 5) && (!this.isStep1Valid() || !this.isStep2Valid() || !this.isStep3Valid())) {
        // Don't show alert - step indicators will be disabled in UI
        return;
      }
      return;
    }
    
    this.currentStep = step;
    window.scrollTo(0, 0);
  }

  canAccessStep(step: number): boolean {
    // Step 1 is always accessible
    if (step === 1) return true;
    
    // Step 2 requires Step 1 to be valid
    if (step === 2) return this.isStep1Valid();
    
    // Step 3 requires Step 1 and Step 2 to be valid
    if (step === 3) return this.isStep1Valid() && this.isStep2Valid();
    
    // Step 4 requires Step 1, 2, and 3 to be valid
    if (step === 4) return this.isStep1Valid() && this.isStep2Valid() && this.isStep3Valid();
    
    // Step 5 requires Step 1, 2, and 3 to be valid
    if (step === 5) return this.isStep1Valid() && this.isStep2Valid() && this.isStep3Valid();
    
    return false;
  }

  // ========================================
  // VALIDATION
  // ========================================

  isStep1Valid(): boolean {
    const requiredFields = ['propertyName', 'selectedCity', 'locationId', 'fullAddress', 'description', 'propertyType'];
    return requiredFields.every(field => {
      const control = this.propertyForm.get(field);
      return control && control.valid;
    });
  }

  isStep2Valid(): boolean {
    const requiredFields = ['pricePerNight', 'numberOfBedrooms', 'numberOfBathrooms', 'maxAdults', 'maxChildren', 'maxInfants', 'maxPets'];
    return requiredFields.every(field => {
      const control = this.propertyForm.get(field);
      return control && control.valid;
    });
  }

  isStep3Valid(): boolean {
    // Require minimum 4 images
    if (this.uploadedImages.length < 4) {
      return false;
    }
    
    // Require at least one main image
    return this.hasMainImage();
  }

  hasMainImage(): boolean {
    return this.uploadedImages.some(img => img.isMainImage);
  }

  canSubmit(): boolean {
    return this.isStep1Valid() && 
           this.isStep2Valid() && 
           this.isStep3Valid() &&
           !this.isSubmitting &&
           this.currentUserId > 0;
  }

  // ========================================
  // SUBMIT - COMPLETE PROPERTY
  // ========================================

  onSubmit(): void {
    if (!this.canSubmit()) {
      // Navigate to the incomplete step
      if (!this.isStep1Valid()) {
        this.currentStep = 1;
      } else if (!this.isStep2Valid()) {
        this.currentStep = 2;
      } else if (!this.isStep3Valid()) {
        this.currentStep = 3;
      }
      window.scrollTo(0, 0);
      return;
    }

    const formValue = this.propertyForm.value;

    // Build complete request
    const request: PropertyCompleteRequestDTO = {
      property: {
        hostId: this.currentUserId,
        propertyName: formValue.propertyName,
        locationId: formValue.locationId,
        fullAddress: formValue.fullAddress,
        description: formValue.description,
        propertyType: formValue.propertyType,
        pricePerNight: formValue.pricePerNight,
        numberOfBedrooms: formValue.numberOfBedrooms,
        numberOfBathrooms: formValue.numberOfBathrooms,
        maxAdults: formValue.maxAdults,
        maxChildren: formValue.maxChildren,
        maxInfants: formValue.maxInfants,
        maxPets: formValue.maxPets
      },
      images: this.uploadedImages.length > 0 
        ? this.uploadedImages.map(img => img.file) 
        : undefined,
      // Convert descriptions to array of strings, empty string if not provided (avoid null)
      imageDescriptions: this.uploadedImages.length > 0
        ? this.uploadedImages.map(img => img.description?.trim() || '')
        : undefined,
      amenityIds: this.selectedAmenityIds.size > 0
        ? Array.from(this.selectedAmenityIds)
        : undefined,
      facilityIds: this.selectedFacilityIds.size > 0
        ? Array.from(this.selectedFacilityIds)
        : undefined
    };

    this.isSubmitting = true;

    this.propertyService.createCompleteProperty(request).subscribe({
      next: (response: any) => {
        this.isSubmitting = false;
        console.log('✅ CREATE COMPLETE PROPERTY SUCCESS:', response);
        console.log('📦 Response data:', response.data);
        
        if (response && response.code === 200 && response.data && response.data.propertyId) {
          const propertyId = response.data.propertyId;
          console.log(`🏠 Property created with ID: ${propertyId}`);
          console.log(`🔗 Redirecting to: /properties/${propertyId}`);
          
          this.showSuccessModal(
            'Thành công!',
            `Property đã được tạo thành công! ID: ${propertyId}`
          );
          
          // Navigate to property detail page
          setTimeout(() => {
            this.router.navigate(['/properties', propertyId]);
          }, 2000);
        } else {
          console.warn('⚠️ Unexpected response format:', response);
          const errorMessage = response?.message || 'Không thể tạo property';
          this.showErrorModal('Có lỗi xảy ra!', errorMessage);
        }
      },
      error: (error: any) => {
        this.isSubmitting = false;
        console.error('❌ CREATE COMPLETE PROPERTY FAILED:', error);
        console.error('📛 Error details:', error.error);
        const errorMessage = error.error?.message || 'Không thể tạo property. Vui lòng thử lại.';
        this.showErrorModal('Lỗi kết nối!', errorMessage);
      }
    });
  }

  cancel(): void {
    if (confirm('Bạn có chắc muốn hủy? Dữ liệu đã nhập sẽ không được lưu.')) {
      this.router.navigate(['/user-profile']);
    }
  }

  // ========================================
  // MODAL METHODS
  // ========================================

  showSuccessModal(title: string, message: string): void {
    this.modalType = 'success';
    this.modalTitle = title;
    this.modalMessage = message;
    this.showModal = true;
  }

  showErrorModal(title: string, message: string): void {
    this.modalType = 'error';
    this.modalTitle = title;
    this.modalMessage = message;
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  // ========================================
  // PROGRESS CALCULATION
  // ========================================

  getProgressPercentage(): number {
    return (this.currentStep / this.totalSteps) * 100;
  }
}

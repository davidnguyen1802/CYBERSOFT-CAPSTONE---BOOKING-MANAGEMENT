import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { Property } from '../../models/property';
import { PropertyService } from '../../services/property.service';
import { TokenService } from '../../services/token.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-edit-property',
  templateUrl: './edit-property.component.html',
  styleUrls: ['./edit-property.component.scss']
})
export class EditPropertyComponent implements OnInit {
  property?: Property;
  propertyId: number = 0;
  currentImageIndex: number = 0;
  loading: boolean = false;
  errorMsg: string = '';
  
  // Edit mode for each section
  editMode = {
    basicInfo: false,
    images: false,
    amenities: false,
    facilities: false
  };
  
  // Image modal state
  isImageModalOpen: boolean = false;
  
  constructor(
    private propertyService: PropertyService,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private tokenService: TokenService
  ) {}
    
  ngOnInit() {
    // Check if user is logged in and is HOST
    const token = this.tokenService.getToken();
    if (!token) {
      alert('Vui lòng đăng nhập để chỉnh sửa chỗ ở!');
      this.router.navigate(['/login']);
      return;
    }
    
    // Get propertyId from URL
    const idParam = this.activatedRoute.snapshot.paramMap.get('id');
    if (idParam !== null) {
      this.propertyId = +idParam;
    }
    if (!isNaN(this.propertyId)) {
      this.loadPropertyDetail();
    }
    
    // Scroll to top when component loads
    window.scrollTo(0, 0);
  }
  
  loadPropertyDetail() {
    this.loading = true;
    this.errorMsg = '';
    
    console.log('🔵 [EDIT-PROPERTY] Loading property detail, ID:', this.propertyId);
    
    this.propertyService.getPropertyDetail(this.propertyId).subscribe({
      next: (response) => {
        console.log('✅ [EDIT-PROPERTY] Property loaded successfully:', response.data);
        this.property = response.data;
        this.loading = false;
      },
      error: (error: any) => {
        console.error('❌ [EDIT-PROPERTY] Error fetching property detail:', error);
        this.errorMsg = 'Không thể tải thông tin chi tiết property';
        this.loading = false;
      }
    });
  }

  // ================ IMAGE GALLERY METHODS ================
  showImage(index: number): void {
    if (this.property && this.property.images && this.property.images.length > 0) {
      if (index < 0) {
        index = 0;
      } else if (index >= this.property.images.length) {
        index = this.property.images.length - 1;
      }        
      this.currentImageIndex = index;
    }
  }

  thumbnailClick(index: number) {
    this.currentImageIndex = index;
  }  

  nextImage(): void {
    this.showImage(this.currentImageIndex + 1);
  }

  previousImage(): void {
    this.showImage(this.currentImageIndex - 1);
  }

  getPropertyImage(index: number = 0): string {
    if (this.property && this.property.images && this.property.images.length > index) {
      const imageUrl = this.property.images[index].imageUrl;
      
      if (imageUrl.startsWith('http')) {
        return imageUrl;
      }
      const cleanPath = imageUrl.startsWith('/') ? imageUrl : `/${imageUrl}`;
      const fullUrl = `${environment.apiBaseUrl || 'http://localhost:8080'}${cleanPath}`;
      return fullUrl;
    }
    return '/assets/img/placeholder.svg';
  }

  getCurrentImage(): string {
    return this.getPropertyImage(this.currentImageIndex);
  }

  // Get icon URL from API
  getIconUrl(iconPath: string): string {
    if (!iconPath) return '';
    if (iconPath.startsWith('http')) {
      return iconPath;
    }
    const cleanPath = iconPath.startsWith('/') ? iconPath : `/${iconPath}`;
    return `${environment.apiBaseUrl || 'http://localhost:8080'}${cleanPath}`;
  }

  // ================ PROPERTY INFO METHODS ================
  getPropertyTypeLabel(): string {
    const types: { [key: number]: string } = {
      0: 'Apartment',
      1: 'House', 
      2: 'Hotel'
    };
    return types[this.property?.propertyType || 0] || 'Property';
  }

  // ================ EDIT MODE METHODS ================
  toggleEditMode(section: 'basicInfo' | 'images' | 'amenities' | 'facilities'): void {
    this.editMode[section] = !this.editMode[section];
    console.log(`✏️ Edit mode for ${section}:`, this.editMode[section]);
  }

  saveSection(section: 'basicInfo' | 'images' | 'amenities' | 'facilities'): void {
    console.log(`💾 Saving ${section}...`);
    // TODO: Implement save logic for each section
    alert(`Chức năng lưu ${section} sẽ được triển khai sau!`);
    this.editMode[section] = false;
  }

  cancelEdit(section: 'basicInfo' | 'images' | 'amenities' | 'facilities'): void {
    console.log(`❌ Cancel edit for ${section}`);
    this.editMode[section] = false;
    // Reload data to reset changes
    this.loadPropertyDetail();
  }

  // ================ UTILITY METHODS ================
  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN').format(price);
  }

  goBack(): void {
    this.router.navigate(['/user/profile'], { queryParams: { section: 'manage-properties' } });
  }

  // ================ IMAGE MODAL METHODS ================
  openImageModal(index: number): void {
    this.currentImageIndex = index;
    this.isImageModalOpen = true;
    document.body.style.overflow = 'hidden';
  }

  closeImageModal(): void {
    this.isImageModalOpen = false;
    document.body.style.overflow = 'auto';
  }

  selectImage(index: number): void {
    this.currentImageIndex = index;
  }
}


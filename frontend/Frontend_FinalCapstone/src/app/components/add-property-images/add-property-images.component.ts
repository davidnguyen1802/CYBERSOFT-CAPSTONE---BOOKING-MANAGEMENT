import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { ImageService } from '../../services/image.service';

interface ImageSlot {
  id: number;
  file: File | null;
  preview: string | null;
  description: string;
  isMainImage: boolean;
  uploadProgress?: number; // For upload progress indicator
}

@Component({
  selector: 'app-add-property-images',
  templateUrl: './add-property-images.component.html',
  styleUrls: ['./add-property-images.component.scss']
})
export class AddPropertyImagesComponent implements OnInit {
  propertyId: number = 0;
  imageSlots: ImageSlot[] = [];
  isSubmitting: boolean = false;
  minImages: number = 4;  // Minimum required images
  maxImages: number = 10; // Maximum allowed images
  
  // Modal state
  showModal: boolean = false;
  modalTitle: string = '';
  modalMessage: string = '';
  modalType: 'success' | 'error' = 'success';
  
  // Drag & Drop state
  isDragging: boolean = false;
  
  // Preview Gallery state
  showGallery: boolean = false;
  currentGalleryIndex: number = 0;
  
  // Reordering state
  draggedSlotIndex: number | null = null;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private imageService: ImageService
  ) {}

  ngOnInit(): void {
    // Get propertyId from query params
    this.route.queryParams.subscribe(params => {
      this.propertyId = +params['propertyId'] || 0;
      console.log('Property ID nhận được:', this.propertyId);
      
      if (this.propertyId <= 0) {
        console.error('Property ID không hợp lệ:', this.propertyId);
        alert('Property ID không hợp lệ!');
        this.router.navigate(['/add-property']);
      }
    });

    // Initialize minimum 4 image slots
    this.initializeImageSlots();
  }

  initializeImageSlots(): void {
    this.imageSlots = [];
    for (let i = 0; i < this.minImages; i++) {
      this.imageSlots.push({
        id: i + 1,
        file: null,
        preview: null,
        description: '',
        isMainImage: i === 0  // First image is main image
      });
    }
  }

  addMoreSlots(): void {
    if (this.imageSlots.length >= this.maxImages) {
      alert(`Chỉ được upload tối đa ${this.maxImages} ảnh!`);
      return;
    }
    
    const newId = this.imageSlots.length + 1;
    this.imageSlots.push({
      id: newId,
      file: null,
      preview: null,
      description: '',
      isMainImage: false
    });
    console.log(`Đã thêm slot ảnh thứ ${newId}`);
  }

  canAddMore(): boolean {
    return this.imageSlots.length < this.maxImages;
  }

  // ===== FEATURE 4: Multiple Selection =====
  onFileSelected(event: any, slotIndex: number): void {
    const files = event.target.files;
    if (!files || files.length === 0) return;
    
    // Multiple file selection
    if (files.length > 1) {
      this.handleMultipleFiles(files, slotIndex);
    } else {
      this.handleSingleFile(files[0], slotIndex);
    }
  }
  
  handleSingleFile(file: File, slotIndex: number): void {
    if (!this.validateFile(file)) return;
    
    console.log(`Đang chọn ảnh slot ${slotIndex + 1}:`, file.name, `(${(file.size / 1024 / 1024).toFixed(2)}MB)`);
    this.previewFile(file, slotIndex);
  }
  
  handleMultipleFiles(files: FileList, startIndex: number): void {
    const filesArray = Array.from(files);
    let currentIndex = startIndex;
    
    filesArray.forEach((file, i) => {
      if (!this.validateFile(file)) return;
      
      // Find next empty slot or add new slot
      while (currentIndex < this.imageSlots.length && this.imageSlots[currentIndex].file !== null) {
        currentIndex++;
      }
      
      if (currentIndex >= this.imageSlots.length) {
        if (this.imageSlots.length < this.maxImages) {
          this.addMoreSlots();
        } else {
          alert(`Chỉ có thể upload tối đa ${this.maxImages} ảnh!`);
          return;
        }
      }
      
      console.log(`Đang chọn ảnh slot ${currentIndex + 1}:`, file.name, `(${(file.size / 1024 / 1024).toFixed(2)}MB)`);
      this.previewFile(file, currentIndex);
      currentIndex++;
    });
  }
  
  validateFile(file: File): boolean {
    if (!file.type.startsWith('image/')) {
      alert('Vui lòng chọn file ảnh hợp lệ!');
      return false;
    }
    
    if (file.size > 5 * 1024 * 1024) {
      alert('Kích thước ảnh tối đa là 5MB!');
      return false;
    }
    
    return true;
  }
  
  previewFile(file: File, slotIndex: number): void {
    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.imageSlots[slotIndex].file = file;
      this.imageSlots[slotIndex].preview = e.target.result;
      this.imageSlots[slotIndex].uploadProgress = 0;
    };
    reader.readAsDataURL(file);
  }

  // ===== FEATURE 2: Drag & Drop Upload =====
  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }
  
  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }
  
  onDrop(event: DragEvent, slotIndex?: number): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
    
    const files = event.dataTransfer?.files;
    if (!files || files.length === 0) return;
    
    // If dropped on specific slot
    if (slotIndex !== undefined) {
      if (files.length === 1) {
        this.handleSingleFile(files[0], slotIndex);
      } else {
        this.handleMultipleFiles(files, slotIndex);
      }
    } else {
      // If dropped on general area, fill empty slots
      const firstEmptyIndex = this.imageSlots.findIndex(slot => slot.file === null);
      if (firstEmptyIndex !== -1) {
        this.handleMultipleFiles(files, firstEmptyIndex);
      } else {
        alert('Vui lòng xóa ảnh cũ hoặc thêm slot mới để upload!');
      }
    }
  }

  removeImage(slotIndex: number): void {
    console.log(`Xóa ảnh slot ${slotIndex + 1}`);
    this.imageSlots[slotIndex].file = null;
    this.imageSlots[slotIndex].preview = null;
    this.imageSlots[slotIndex].description = '';
    this.imageSlots[slotIndex].uploadProgress = 0;
  }

  // ===== FEATURE 3: Image Reordering =====
  onSlotDragStart(event: DragEvent, slotIndex: number): void {
    this.draggedSlotIndex = slotIndex;
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
      event.dataTransfer.setData('text/html', slotIndex.toString());
    }
  }
  
  onSlotDragOver(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
  }
  
  onSlotDrop(event: DragEvent, targetIndex: number): void {
    event.preventDefault();
    event.stopPropagation();
    
    if (this.draggedSlotIndex === null || this.draggedSlotIndex === targetIndex) {
      this.draggedSlotIndex = null;
      return;
    }
    
    // Swap slots
    const temp = this.imageSlots[this.draggedSlotIndex];
    this.imageSlots[this.draggedSlotIndex] = this.imageSlots[targetIndex];
    this.imageSlots[targetIndex] = temp;
    
    // Update IDs
    this.imageSlots.forEach((slot, index) => {
      slot.id = index + 1;
    });
    
    // Update main image flag
    this.imageSlots.forEach((slot, index) => {
      slot.isMainImage = index === 0;
    });
    
    console.log(`Đã hoán đổi slot ${this.draggedSlotIndex + 1} với slot ${targetIndex + 1}`);
    this.draggedSlotIndex = null;
  }
  
  setAsMainImage(slotIndex: number): void {
    if (slotIndex === 0) return; // Already main image
    
    // Swap with first slot
    const temp = this.imageSlots[0];
    this.imageSlots[0] = this.imageSlots[slotIndex];
    this.imageSlots[slotIndex] = temp;
    
    // Update IDs and flags
    this.imageSlots.forEach((slot, index) => {
      slot.id = index + 1;
      slot.isMainImage = index === 0;
    });
    
    console.log(`Đã đặt slot ${slotIndex + 1} làm ảnh đại diện`);
  }

  getUploadedCount(): number {
    return this.imageSlots.filter(slot => slot.file !== null).length;
  }

  canSubmit(): boolean {
    return this.getUploadedCount() >= this.minImages;
  }

  // ===== FEATURE 7: Preview Gallery =====
  openGallery(index: number): void {
    const uploadedImages = this.imageSlots.filter(slot => slot.preview !== null);
    if (uploadedImages.length === 0) return;
    
    // Find the actual index in uploaded images
    const uploadedIndex = this.imageSlots
      .slice(0, index + 1)
      .filter(slot => slot.preview !== null).length - 1;
    
    this.currentGalleryIndex = uploadedIndex >= 0 ? uploadedIndex : 0;
    this.showGallery = true;
    console.log('Mở gallery tại ảnh:', this.currentGalleryIndex + 1);
  }
  
  closeGallery(): void {
    this.showGallery = false;
  }
  
  nextGalleryImage(): void {
    const uploadedImages = this.imageSlots.filter(slot => slot.preview !== null);
    this.currentGalleryIndex = (this.currentGalleryIndex + 1) % uploadedImages.length;
  }
  
  prevGalleryImage(): void {
    const uploadedImages = this.imageSlots.filter(slot => slot.preview !== null);
    this.currentGalleryIndex = (this.currentGalleryIndex - 1 + uploadedImages.length) % uploadedImages.length;
  }
  
  getUploadedImages(): ImageSlot[] {
    return this.imageSlots.filter(slot => slot.preview !== null);
  }

  // ===== FEATURE 9: Upload Progress Indicator =====
  onSubmit(): void {
    if (!this.canSubmit()) {
      alert(`Vui lòng upload tối thiểu ${this.minImages} ảnh!`);
      return;
    }

    // Collect files and descriptions
    const files: File[] = [];
    const descriptions: string[] = [];

    this.imageSlots.forEach(slot => {
      if (slot.file) {
        files.push(slot.file);
        descriptions.push(slot.description || (slot.isMainImage ? 'Main image' : ''));
      }
    });

    this.isSubmitting = true;
    console.log('Bắt đầu upload ảnh cho property:', this.propertyId);
    console.log('Số lượng ảnh:', files.length);
    
    // Simulate upload progress for each image
    this.simulateUploadProgress(files.length);

    this.imageService.addImagesToProperty(this.propertyId, files, descriptions).subscribe({
      next: (response: any) => {
        console.log('Upload ảnh thành công:', response);
        this.isSubmitting = false;
        
        // Set all progress to 100%
        this.imageSlots.forEach(slot => {
          if (slot.file) slot.uploadProgress = 100;
        });
        
        if (response && response.code === 200) {
          this.showSuccessModal(
            'Thành công!',
            'Đã thêm ảnh cho property thành công. Bạn có thể xem property trong danh sách của mình.'
          );
        } else {
          const errorMessage = response?.message || 'Không thể thêm ảnh. Vui lòng thử lại.';
          this.showErrorModal('Có lỗi xảy ra!', errorMessage);
        }
      },
      error: (error: any) => {
        this.isSubmitting = false;
        console.error('Lỗi khi upload ảnh:', error);
        
        // Reset progress on error
        this.imageSlots.forEach(slot => {
          if (slot.file) slot.uploadProgress = 0;
        });
        
        const errorMessage = error.error?.message || 'Không thể thêm ảnh. Vui lòng thử lại.';
        this.showErrorModal('Lỗi kết nối!', errorMessage);
      }
    });
  }
  
  simulateUploadProgress(totalFiles: number): void {
    let uploadedSlots = this.imageSlots.filter(slot => slot.file !== null);
    
    uploadedSlots.forEach((slot, index) => {
      let progress = 0;
      const interval = setInterval(() => {
        progress += Math.random() * 15;
        if (progress >= 95) {
          progress = 95; // Keep at 95% until actual upload completes
          clearInterval(interval);
        }
        slot.uploadProgress = Math.min(progress, 95);
      }, 200);
    });
  }

  goBack(): void {
    if (confirm('Bạn có chắc muốn quay lại? Ảnh đã chọn sẽ không được lưu.')) {
      this.router.navigate(['/add-property']);
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
      // Navigate to amenities step
      this.router.navigate(['/host/add-property-amenities'], {
        queryParams: { propertyId: this.propertyId }
      });
    }
  }
}

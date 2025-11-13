import { Component, OnInit } from '@angular/core';
import { PromotionService } from '../../services/promotion.service';
import { TokenService } from '../../services/token.service';
import { UserPromotionDTO, getPromotionStatus } from '../../models/user-promotion.dto';
import { ApiResponse } from '../../models/api-response';
import { PageContent } from '../../models/api-response';

@Component({
  selector: 'app-my-promotions',
  templateUrl: './my-promotions.component.html',
  styleUrls: ['./my-promotions.component.scss']
})
export class MyPromotionsComponent implements OnInit {
  promotions: UserPromotionDTO[] = [];
  activePromotions: UserPromotionDTO[] = [];
  usedPromotions: UserPromotionDTO[] = [];
  expiredPromotions: UserPromotionDTO[] = [];
  loading: boolean = true;
  error: string = '';
  
  // Pagination
  currentPage: number = 0;
  totalPages: number = 0;
  totalElements: number = 0;
  pageSize: number = 9;

  constructor(
    private promotionService: PromotionService,
    private tokenService: TokenService
  ) {}

  ngOnInit(): void {
    console.log('=======================================');
    console.log('🎫 MY PROMOTIONS COMPONENT - Initializing');
    console.log('   Timestamp:', new Date().toISOString());
    console.log('=======================================');
    this.loadPromotions();
  }

  loadPromotions(page: number = 0): void {
    console.log('🔄 Loading promotions... page:', page);
    
    console.log('📡 Calling PromotionService.getMyPromotions()...');
    this.promotionService.getMyPromotions(page, this.pageSize).subscribe({
      next: (response: ApiResponse<PageContent<UserPromotionDTO>>) => {
        console.log('📥 Received response from PromotionService');
        console.log('   Code:', response.code);
        console.log('   Message:', response.message);
        
        if (response.code === 200 && response.data) {
          this.promotions = response.data.content;
          this.currentPage = response.data.pageNumber;
          this.totalPages = response.data.totalPages;
          this.totalElements = response.data.totalElements;
          console.log('✅ Promotions loaded successfully');
          console.log(`   Total promotions: ${this.totalElements} (page ${this.currentPage + 1}/${this.totalPages})`);
          this.categorizePromotions();
        } else {
          console.error('❌ Unexpected response code:', response.code);
          this.error = response.message || 'Không thể tải mã giảm giá';
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('❌ Error loading promotions');
        console.error('   Status:', err.status);
        console.error('   Message:', err.message);
        console.error('   Full error:', err);
        this.error = 'Đã có lỗi xảy ra khi tải mã giảm giá';
        this.loading = false;
      }
    });
  }

  onPageChange(page: number): void {
    this.loadPromotions(page);
  }

  categorizePromotions(): void {
    console.log('📊 Categorizing promotions...');
    
    // ✅ FIXED: Use helper function to compute status from API data
    // - ACTIVE: active=true + not expired + not used
    // - USED: has promotionUsages records
    // - EXPIRED: active=false OR past expiresDate
    
    this.activePromotions = this.promotions.filter(p => getPromotionStatus(p) === 'ACTIVE');

    this.usedPromotions = this.promotions.filter(p => getPromotionStatus(p) === 'USED');

    this.expiredPromotions = this.promotions.filter(p => getPromotionStatus(p) === 'EXPIRED');
    
    console.log('   Active:', this.activePromotions.length);
    console.log('   Used:', this.usedPromotions.length);
    console.log('   Expired:', this.expiredPromotions.length);
    console.log('=======================================');
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  formatCurrency(amount: number): string {
    return amount.toLocaleString('vi-VN') + 'đ';
  }

  isExpiringSoon(expiresDate: string): boolean {
    const expires = new Date(expiresDate);
    const now = new Date();
    const daysUntilExpiry = Math.floor((expires.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
    return daysUntilExpiry <= 7 && daysUntilExpiry > 0;
  }

  copyPromotionCode(code: string): void {
    navigator.clipboard.writeText(code).then(() => {
      alert('Đã sao chép mã: ' + code);
    }).catch(err => {
      console.error('Failed to copy code:', err);
    });
  }
}

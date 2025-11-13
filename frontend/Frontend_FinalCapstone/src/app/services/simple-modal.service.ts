import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';

export interface ModalConfig {
  title: string;
  message: string;
  primaryButton: string;
  showCancelButton?: boolean;
  cancelButton?: string;
  redirectUrl?: string;
  showInput?: boolean;
  inputPlaceholder?: string;
  inputLabel?: string;
  isDanger?: boolean; // For destructive actions (red color)
}

export interface ModalState {
  isVisible: boolean;
  config: ModalConfig | null;
  inputValue?: string;
  onConfirm?: (inputValue?: string) => void;
  onCancel?: () => void;
}

/**
 * Simple Modal Service for authentication-related popups
 * - Login Required (anonymous user accessing protected feature)
 * - Session Expired (403/410 from backend)
 * 
 * NOTE: Lightweight implementation, no external dependencies
 */
@Injectable({
  providedIn: 'root'
})
export class SimpleModalService {
  private modalSubject = new Subject<ModalState>();
  public modalState$ = this.modalSubject.asObservable();

  constructor(private router: Router) {}

  /**
   * Show "Login Required" modal for anonymous users
   * - Message: "Bạn cần đăng nhập để trải nghiệm chức năng này."
   * - Button: "Đăng nhập" → redirect to /login
   */
  showLoginRequired(): void {
    console.log('🔐 SimpleModalService: Showing login required modal');
    
    this.modalSubject.next({
      isVisible: true,
      config: {
        title: 'Yêu cầu đăng nhập',
        message: 'Bạn cần đăng nhập để trải nghiệm chức năng này.',
        primaryButton: 'Đăng nhập',
        showCancelButton: true,
        cancelButton: 'Hủy',
        redirectUrl: '/login'
      }
    });
  }

  /**
   * Show "Session Expired" modal for 419 error (token expired)
   * - Message: "Phiên đăng nhập đã hết. Vui lòng đăng nhập lại."
   * - Primary Button: "Đăng nhập" → redirect to /login
   * - Cancel Button: "Hủy" → close modal, stay on current page
   */
  showSessionExpired(): void {
    console.log('⏰ SimpleModalService: Showing session expired modal');
    
    this.modalSubject.next({
      isVisible: true,
      config: {
        title: 'Phiên đăng nhập đã hết',
        message: 'Phiên đăng nhập đã hết. Vui lòng đăng nhập lại.',
        primaryButton: 'Đăng nhập',
        showCancelButton: true,
        cancelButton: 'Hủy',
        redirectUrl: '/login'
      }
    });
  }

  /**
   * Handle primary button click
   * - If redirectUrl provided → navigate
   * - Close modal
   */
  handlePrimaryAction(config: ModalConfig | null): void {
    if (config?.redirectUrl) {
      console.log(`➡️ SimpleModalService: Redirecting to ${config.redirectUrl}`);
      this.router.navigate([config.redirectUrl]);
    }
    this.close();
  }

  /**
   * Handle cancel button click
   * - Just close modal
   */
  handleCancelAction(): void {
    console.log('❌ SimpleModalService: Modal cancelled');
    this.close();
  }

  /**
   * Show error message modal
   */
  showError(message: string, title: string = 'Lỗi'): void {
    console.log('❌ SimpleModalService: Showing error modal');
    
    this.modalSubject.next({
      isVisible: true,
      config: {
        title: title,
        message: message,
        primaryButton: 'Đóng',
        showCancelButton: false
      }
    });
  }

  /**
   * Show success message modal
   */
  showSuccess(message: string, title: string = 'Thành công'): void {
    console.log('✅ SimpleModalService: Showing success modal');
    
    this.modalSubject.next({
      isVisible: true,
      config: {
        title: title,
        message: message,
        primaryButton: 'Đóng',
        showCancelButton: false
      }
    });
  }

  /**
   * Show info message modal
   */
  showInfo(message: string, title: string = 'Thông báo'): void {
    console.log('ℹ️ SimpleModalService: Showing info modal');
    
    this.modalSubject.next({
      isVisible: true,
      config: {
        title: title,
        message: message,
        primaryButton: 'Đóng',
        showCancelButton: false
      }
    });
  }

  /**
   * Show confirmation modal with optional input field
   */
  showConfirm(
    title: string, 
    message: string, 
    options?: {
      primaryButton?: string;
      cancelButton?: string;
      showInput?: boolean;
      inputPlaceholder?: string;
      inputLabel?: string;
      isDanger?: boolean;
    }
  ): Promise<string | null> {
    console.log('❓ SimpleModalService: Showing confirmation modal');
    
    return new Promise((resolve) => {
      this.modalSubject.next({
        isVisible: true,
        config: {
          title: title,
          message: message,
          primaryButton: options?.primaryButton || 'Xác nhận',
          showCancelButton: true,
          cancelButton: options?.cancelButton || 'Hủy',
          showInput: options?.showInput || false,
          inputPlaceholder: options?.inputPlaceholder || '',
          inputLabel: options?.inputLabel || '',
          isDanger: options?.isDanger || false
        },
        inputValue: '',
        onConfirm: (inputValue?: string) => {
          resolve(inputValue || '');
          this.close();
        },
        onCancel: () => {
          resolve(null);
          this.close();
        }
      });
    });
  }

  /**
   * Close modal
   */
  close(): void {
    this.modalSubject.next({
      isVisible: false,
      config: null
    });
  }
}

import { Component, OnInit } from '@angular/core';
import { BookingService } from '../../services/booking.service';
import { TokenService } from '../../services/token.service';
import { SimpleModalService } from '../../services/simple-modal.service';
import { Booking, BookingStatus, ApprovalPreviewDTO, HostStatistics } from '../../models/booking';
import { ConflictInfo } from '../shared/confirm-modal/confirm-modal.component';

@Component({
  selector: 'app-host-dashboard',
  templateUrl: './host-dashboard.component.html',
  styleUrls: ['./host-dashboard.component.scss']
})
export class HostDashboardComponent implements OnInit {
  bookings: Booking[] = [];
  selectedStatuses: string[] = []; // Changed to array for multi-select
  loading = false;
  currentPage = 0;
  pageSize = 20;
  totalPages = 0;
  totalElements = 0;
  hostId: number = 0;
  
  // View mode
  viewMode: 'table' | 'calendar' = 'table';
  
  // Statistics
  statistics: HostStatistics | null = null;
  loadingStats = false;

  statusOptions = [
    { value: 'PENDING', label: 'Pending', color: '#9C27B0' },
    { value: 'CONFIRMED', label: 'Confirmed', color: '#2196F3' },
    { value: 'PAID', label: 'Paid', color: '#4CAF50' },
    { value: 'COMPLETED', label: 'Completed', color: '#9E9E9E' },
    { value: 'CANCELLED', label: 'Cancelled', color: '#F44336' },
    { value: 'REJECTED', label: 'Rejected', color: '#FF5722' }
  ];

  // Modal states
  showConfirmModal = false;
  confirmModalData = {
    title: '',
    message: '',
    warning: '',
    conflicts: [] as ConflictInfo[],
    confirmText: '',
    cancelText: 'Cancel',
    confirmButtonClass: 'btn-success',
    onConfirm: () => {}
  };

  showInputModal = false;
  inputModalData = {
    title: '',
    message: '',
    inputLabel: '',
    inputPlaceholder: '',
    confirmText: '',
    cancelText: 'Cancel',
    onConfirm: (value: string) => {}
  };

  BookingStatus = BookingStatus;

  constructor(
    private bookingService: BookingService,
    private tokenService: TokenService,
    private modalService: SimpleModalService
  ) {}

  ngOnInit() {
    this.hostId = this.tokenService.getUserId();
    if (this.hostId > 0) {
      this.loadBookings();
      this.loadStatistics();
    }
  }

  loadStatistics() {
    this.loadingStats = true;
    this.bookingService.getHostStatistics(this.hostId)
      .subscribe({
        next: (response) => {
          if (response.code === 200 && response.data) {
            this.statistics = response.data;
          }
          this.loadingStats = false;
        },
        error: (err) => {
          console.error('Failed to load statistics:', err);
          this.loadingStats = false;
        }
      });
  }

  toggleViewMode() {
    this.viewMode = this.viewMode === 'table' ? 'calendar' : 'table';
  }

  loadBookings() {
    this.loading = true;
    
    // Get all bookings or filter by multiple statuses
    const apiCall = this.selectedStatuses.length === 0
      ? this.bookingService.getBookingsByHost(this.hostId, this.currentPage, this.pageSize)
      : this.bookingService.filterHostBookings(this.hostId, this.selectedStatuses, this.currentPage, this.pageSize);
    
    apiCall.subscribe({
      next: (response) => {
        if (response.code === 200 && response.data) {
          this.bookings = response.data.content;
          this.totalPages = response.data.totalPages;
          this.totalElements = response.data.totalElements;
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load bookings:', err);
        this.loading = false;
        this.modalService.showError('Không thể tải danh sách booking. Vui lòng thử lại.');
      }
    });
  }

  onStatusFilterChange(status: string) {
    if (status === 'ALL') {
      // Clear all filters
      this.selectedStatuses = [];
    } else {
      // Toggle status in array
      const index = this.selectedStatuses.indexOf(status);
      if (index > -1) {
        // Remove if already selected
        this.selectedStatuses.splice(index, 1);
      } else {
        // Add if not selected
        this.selectedStatuses.push(status);
      }
    }
    
    this.currentPage = 0;
    this.loadBookings();
  }

  isStatusSelected(status: string): boolean {
    if (status === 'ALL') {
      return this.selectedStatuses.length === 0;
    }
    return this.selectedStatuses.includes(status);
  }

  onApprove(booking: Booking) {
    if (!booking.id) return;

    // Step 1: Preview conflicts
    this.bookingService.previewApproval(booking.id, this.hostId)
      .subscribe({
        next: (response) => {
          if (response.code === 200 && response.data) {
            const preview = response.data;

            if (preview.totalConflicts > 0) {
              // Show confirmation modal with conflicts
              const conflicts: ConflictInfo[] = preview.willBeAutoRejected.map(c => ({
                id: c.id,
                guestName: c.guestName,
                checkIn: c.checkIn,
                checkOut: c.checkOut
              }));

              this.confirmModalData = {
                title: 'Approve Booking?',
                message: `Are you sure you want to approve Booking #${booking.id}?`,
                warning: preview.warning || `${preview.totalConflicts} conflicting booking(s) will be automatically rejected.`,
                conflicts: conflicts,
                confirmText: 'Yes, Approve (and reject conflicts)',
                cancelText: 'Cancel',
                confirmButtonClass: 'btn-success',
                onConfirm: () => {
                  this.confirmApproval(booking.id!);
                  this.showConfirmModal = false;
                }
              };
              this.showConfirmModal = true;
            } else {
              // No conflicts, show simple confirmation
              this.confirmModalData = {
                title: 'Approve Booking?',
                message: `Are you sure you want to approve Booking #${booking.id}?`,
                warning: '',
                conflicts: [],
                confirmText: 'Yes, Approve',
                cancelText: 'Cancel',
                confirmButtonClass: 'btn-success',
                onConfirm: () => {
                  this.confirmApproval(booking.id!);
                  this.showConfirmModal = false;
                }
              };
              this.showConfirmModal = true;
            }
          }
        },
        error: (err) => {
          console.error('Error previewing approval:', err);
          this.modalService.showError(err.error?.message || 'Không thể xem trước approval');
        }
      });
  }

  confirmApproval(bookingId: number) {
    this.bookingService.approveBooking(bookingId, this.hostId)
      .subscribe({
        next: (response) => {
          if (response.code === 200) {
            this.modalService.showSuccess(response.message || 'Đã duyệt booking thành công');
            this.loadBookings();
          } else {
            this.modalService.showError(response.message || 'Không thể duyệt booking');
          }
        },
        error: (err) => {
          console.error('Error approving booking:', err);
          this.modalService.showError(err.error?.message || 'Không thể duyệt booking');
        }
      });
  }

  onReject(booking: Booking) {
    if (!booking.id) return;

    // Show input modal for rejection reason
    this.inputModalData = {
      title: 'Reject Booking?',
      message: `Are you sure you want to reject Booking #${booking.id}?`,
      inputLabel: 'Reason for rejection (optional)',
      inputPlaceholder: 'Enter reason for rejection...',
      confirmText: 'Yes, Reject',
      cancelText: 'Cancel',
      onConfirm: (reason: string) => {
        this.bookingService.rejectBooking(booking.id!, this.hostId, reason || undefined)
          .subscribe({
            next: (response) => {
              if (response.code === 200) {
                this.modalService.showSuccess(response.message || 'Đã từ chối booking thành công');
                this.loadBookings();
              } else {
                this.modalService.showError(response.message || 'Không thể từ chối booking');
              }
            },
            error: (err) => {
              console.error('Error rejecting booking:', err);
              this.modalService.showError(err.error?.message || 'Không thể từ chối booking');
            }
          });
        this.showInputModal = false;
      }
    };
    this.showInputModal = true;
  }

  onPageChange(page: number) {
    this.currentPage = page;
    this.loadBookings();
  }

  getStatusClass(status: string | BookingStatus | undefined): string {
    if (!status) return '';
    
    const statusStr: string = typeof status === 'string' 
      ? status 
      : (status as BookingStatus).toString();
    const normalized = statusStr === statusStr.toUpperCase() 
      ? statusStr.charAt(0) + statusStr.slice(1).toLowerCase()
      : statusStr;
    
    return `status-${normalized.toLowerCase()}`;
  }

  isPendingStatus(booking: Booking): boolean {
    if (!booking.status) return false;
    const statusStr = booking.status.toString();
    return statusStr === 'PENDING' || statusStr === 'Pending' || 
           statusStr === BookingStatus.PENDING;
  }

  formatDate(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatCurrency(amount: number | undefined): string {
    if (!amount) return '0 VND';
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  }

  // Modal handlers
  handleConfirmModalCancel() {
    this.showConfirmModal = false;
  }

  handleInputModalCancel() {
    this.showInputModal = false;
  }

  handleCalendarDateClick(date: Date) {
    console.log('Calendar date clicked:', date);
    // Could filter bookings by this date or show details
  }
}


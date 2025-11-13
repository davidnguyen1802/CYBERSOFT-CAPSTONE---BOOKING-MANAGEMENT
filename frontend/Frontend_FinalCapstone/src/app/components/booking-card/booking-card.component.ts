import { Component, Input, OnInit } from '@angular/core';
import { Booking } from '../../models/booking';

@Component({
  selector: 'app-booking-card',
  templateUrl: './booking-card.component.html',
  styleUrls: ['./booking-card.component.scss']
})
export class BookingCardComponent implements OnInit {
  @Input() booking!: Booking;
  @Input() onViewDetail!: (id: number) => void;
  @Input() onCancel?: (id: number) => void;
  @Input() onDelete?: (id: number) => void;
  @Input() onPay?: (id: number) => void;
  @Input() getCountdown?: (id: number) => string;

  ngOnInit() {
    console.log('🎴 BookingCard initialized:', {
      bookingId: this.booking.id,
      status: this.booking.status,
      hasOnCancel: !!this.onCancel,
      hasOnDelete: !!this.onDelete,
      hasOnPay: !!this.onPay,
      promotionCode: this.booking.promotionCode
    });
  }

  get statusInfo() {
    const status = this.booking.status?.toUpperCase();
    switch (status) {
      case 'PENDING':
        return { text: 'Chờ xác nhận', color: 'status-pending' };
      case 'CONFIRMED':
        return { text: 'Đã xác nhận', color: 'status-confirmed' };
      case 'PAID':
        return { text: 'Đã thanh toán', color: 'status-paid' };
      case 'COMPLETED':
        return { text: 'Hoàn thành', color: 'status-completed' };
      case 'CANCELLED':
        return { text: 'Đã hủy', color: 'status-cancelled' };
      case 'REJECTED':
        return { text: 'Bị từ chối', color: 'status-rejected' };
      default:
        return { text: 'Pending Approval', color: 'status-pending' };
    }
  }

  get hasPromotion(): boolean {
    return !!this.booking.promotionCode;
  }

  get originalAmount(): number {
    return this.booking.originalAmount || 0;
  }

  get discount(): number {
    return (this.booking as any).discountAmount ?? (this.booking as any).discountApplied ?? 0;
  }

  get finalPrice(): number {
    return this.booking.totalPrice || 0;
  }

  get nights(): number {
    if (!this.booking.checkIn || !this.booking.checkOut) return 0;
    const checkIn = new Date(this.booking.checkIn);
    const checkOut = new Date(this.booking.checkOut);
    const diffTime = Math.abs(checkOut.getTime() - checkIn.getTime());
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  }

  get paymentDeadlineCountdown(): string {
    if (!this.booking.paymentDeadline || this.booking.status !== 'CONFIRMED') {
      return '';
    }

    const deadline = new Date(this.booking.paymentDeadline);
    const now = new Date();
    const diff = deadline.getTime() - now.getTime();

    if (diff <= 0) {
      return 'Hết hạn';
    }

    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));

    if (days > 0) {
      return `Còn ${days} ngày ${hours}h`;
    } else if (hours > 0) {
      return `Còn ${hours}h ${minutes}m`;
    } else {
      return `Còn ${minutes}m`;
    }
  }

  get isNearDeadline(): boolean {
    if (!this.booking.paymentDeadline || this.booking.status !== 'CONFIRMED') {
      return false;
    }

    const deadline = new Date(this.booking.paymentDeadline);
    const now = new Date();
    const diff = deadline.getTime() - now.getTime();
    const hoursRemaining = diff / (1000 * 60 * 60);

    return hoursRemaining <= 2; // Warning if < 2 hours
  }

  handleViewDetail(): void {
    if (this.booking.id) {
      this.onViewDetail(this.booking.id);
    }
  }

  handleCancel(): void {
    if (this.onCancel && this.booking.id) {
      this.onCancel(this.booking.id);
    }
  }

  handleDelete(): void {
    if (this.onDelete && this.booking.id) {
      this.onDelete(this.booking.id);
    }
  }

  handlePay(): void {
    if (this.onPay && this.booking.id) {
      this.onPay(this.booking.id);
    }
  }

  getCancelledByText(cancelledBy: string): string {
    switch (cancelledBy) {
      case 'guest':
        return 'Khách hàng';
      case 'host':
        return 'Chủ nhà';
      case 'system':
        return 'Hệ thống';
      default:
        return cancelledBy;
    }
  }
}

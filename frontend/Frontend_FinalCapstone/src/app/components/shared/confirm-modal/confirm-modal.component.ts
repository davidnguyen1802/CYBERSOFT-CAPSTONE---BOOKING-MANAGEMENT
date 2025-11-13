import { Component, EventEmitter, Input, Output } from '@angular/core';

export interface ConflictInfo {
  id: number;
  guestName: string;
  checkIn: string;
  checkOut: string;
}

@Component({
  selector: 'app-confirm-modal',
  templateUrl: './confirm-modal.component.html',
  styleUrls: ['./confirm-modal.component.scss']
})
export class ConfirmModalComponent {
  @Input() isVisible: boolean = false;
  @Input() title: string = 'Confirm Action';
  @Input() message: string = '';
  @Input() warning: string = '';
  @Input() conflicts: ConflictInfo[] = [];
  @Input() confirmText: string = 'Confirm';
  @Input() cancelText: string = 'Cancel';
  @Input() confirmButtonClass: string = 'btn-primary';
  
  @Output() onConfirm = new EventEmitter<void>();
  @Output() onCancel = new EventEmitter<void>();

  handleConfirm() {
    this.onConfirm.emit();
  }

  handleCancel() {
    this.onCancel.emit();
  }

  handleBackdropClick(event: MouseEvent) {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.handleCancel();
    }
  }

  formatDate(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
}


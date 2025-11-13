import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-input-modal',
  templateUrl: './input-modal.component.html',
  styleUrls: ['./input-modal.component.scss']
})
export class InputModalComponent {
  @Input() isVisible: boolean = false;
  @Input() title: string = 'Input Required';
  @Input() message: string = '';
  @Input() inputLabel: string = 'Input';
  @Input() inputPlaceholder: string = 'Enter text...';
  @Input() inputType: 'text' | 'textarea' = 'text';
  @Input() maxLength: number = 500;
  @Input() confirmText: string = 'Confirm';
  @Input() cancelText: string = 'Cancel';
  
  @Output() onConfirm = new EventEmitter<string>();
  @Output() onCancel = new EventEmitter<void>();

  inputValue: string = '';

  handleConfirm() {
    this.onConfirm.emit(this.inputValue);
    this.inputValue = ''; // Reset
  }

  handleCancel() {
    this.onCancel.emit();
    this.inputValue = ''; // Reset
  }

  handleBackdropClick(event: MouseEvent) {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.handleCancel();
    }
  }

  get remainingChars(): number {
    return this.maxLength - this.inputValue.length;
  }
}














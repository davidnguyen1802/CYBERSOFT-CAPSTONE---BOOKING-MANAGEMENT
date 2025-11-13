import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { SimpleModalService, ModalState, ModalConfig } from '../../../services/simple-modal.service';

@Component({
  selector: 'app-simple-modal',
  templateUrl: './simple-modal.component.html',
  styleUrls: ['./simple-modal.component.scss']
})
export class SimpleModalComponent implements OnInit, OnDestroy {
  isVisible = false;
  config: ModalConfig | null = null;
  inputValue: string = '';
  
  private currentState: ModalState | null = null;
  private subscription?: Subscription;

  constructor(private modalService: SimpleModalService) {}

  ngOnInit(): void {
    this.subscription = this.modalService.modalState$.subscribe(
      (state: ModalState) => {
        this.isVisible = state.isVisible;
        this.config = state.config;
        this.inputValue = state.inputValue || '';
        this.currentState = state;
      }
    );
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  onPrimaryClick(): void {
    if (this.currentState?.onConfirm) {
      // New flow: Use callback
      this.currentState.onConfirm(this.inputValue);
    } else {
      // Old flow: Use service method
      this.modalService.handlePrimaryAction(this.config);
    }
  }

  onCancelClick(): void {
    if (this.currentState?.onCancel) {
      // New flow: Use callback
      this.currentState.onCancel();
    } else {
      // Old flow: Use service method
      this.modalService.handleCancelAction();
    }
  }

  onBackdropClick(): void {
    // Only close on backdrop click if cancel button is shown
    if (this.config?.showCancelButton) {
      this.onCancelClick();
    }
  }
}

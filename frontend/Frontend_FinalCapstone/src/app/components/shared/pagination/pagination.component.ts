import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-pagination',
  templateUrl: './pagination.component.html',
  styleUrls: ['./pagination.component.scss']
})
export class PaginationComponent {
  @Input() currentPage: number = 0;
  @Input() totalPages: number = 0;
  @Input() totalElements: number = 0;
  @Input() pageSize: number = 9;
  @Input() isFirst: boolean = true;
  @Input() isLast: boolean = false;
  
  @Output() pageChange = new EventEmitter<number>();

  onPageChange(page: number): void {
    if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
      this.pageChange.emit(page);
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPagesToShow = 5;
    
    if (this.totalPages <= maxPagesToShow) {
      // Show all pages if total is small
      for (let i = 0; i < this.totalPages; i++) {
        pages.push(i);
      }
    } else {
      // Show smart pagination
      let startPage = Math.max(0, this.currentPage - 2);
      let endPage = Math.min(this.totalPages - 1, this.currentPage + 2);
      
      // Adjust if we're near the beginning
      if (this.currentPage < 2) {
        endPage = Math.min(this.totalPages - 1, maxPagesToShow - 1);
      }
      
      // Adjust if we're near the end
      if (this.currentPage > this.totalPages - 3) {
        startPage = Math.max(0, this.totalPages - maxPagesToShow);
      }
      
      for (let i = startPage; i <= endPage; i++) {
        pages.push(i);
      }
    }
    
    return pages;
  }

  get startItem(): number {
    return this.currentPage * this.pageSize + 1;
  }

  get endItem(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.totalElements);
  }
}



















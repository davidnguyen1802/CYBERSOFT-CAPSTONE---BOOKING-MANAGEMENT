import { Component, OnInit, OnDestroy, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { PropertyService } from '../../services/property.service';
import getBaseUrl from '../../utils/url.util';

@Component({
  selector: 'app-recommended',
  templateUrl: './recommended.component.html',
  styleUrls: ['./recommended.component.scss']
})
export class RecommendedComponent implements OnInit, AfterViewInit, OnDestroy {
  items: any[] = [];
  loading = false;
  activeIndex = 0;
  autoplayInterval: any;

  @ViewChild('track', { static: false }) trackRef!: ElementRef<HTMLElement>;

  constructor(private propertyService: PropertyService) { }

  ngOnInit(): void {
    this.loading = true;
    this.propertyService.getTop7Properties().subscribe({
      next: (res: any) => {
        const list = res && (res.data || res);
        this.items = Array.isArray(list) ? list : [];
        this.loading = false;
      },
      error: (err: any) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  ngAfterViewInit(): void {
    // start autoplay after view init
    setTimeout(() => this.startAutoplay(), 600);
  }

  ngOnDestroy(): void {
    this.stopAutoplay();
  }

  startAutoplay(): void {
    this.stopAutoplay();
    if (!this.items || this.items.length <= 1) return;
    this.autoplayInterval = setInterval(() => {
      this.activeIndex = (this.activeIndex + 1) % this.items.length;
      this.scrollToActive();
    }, 3500);
  }

  stopAutoplay(): void {
    if (this.autoplayInterval) {
      clearInterval(this.autoplayInterval);
      this.autoplayInterval = null;
    }
  }

  goTo(index: number): void {
    this.activeIndex = index;
    this.scrollToActive();
    this.startAutoplay();
  }

  private scrollToActive(): void {
    const host = this.trackRef?.nativeElement;
    if (!host) return;
    const cards = host.querySelectorAll('.card') as NodeListOf<HTMLElement>;
    const el = cards[this.activeIndex];
    if (!el) return;
    const parentWidth = host.clientWidth;
    const left = el.offsetLeft - (parentWidth - el.clientWidth) / 2;
    host.scrollTo({ left, behavior: 'smooth' });
  }

  getImageUrl(path: string): string {
    if (!path) return '/assets/img/default.jpg';
    if (path.startsWith('http')) return path;
    const base = getBaseUrl();
    return `${base}${path.startsWith('/') ? '' : '/'}${path}`;
  }
}

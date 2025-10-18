import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Property, PropertyListItem } from '../../models/property';
import { PropertyService } from '../../services/property.service';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit, OnDestroy {
  // Top 7 Properties - Carousel
  topProperties: Property[] = [];
  loadingTop7: boolean = false;
  currentSlide: number = 0;
  carouselInterval: any;

  // Popular Properties by Type
  popularHotels: Property[] = [];
  popularApartments: Property[] = [];
  popularHouses: Property[] = [];
  loadingHotels: boolean = false;
  loadingApartments: boolean = false;
  loadingHouses: boolean = false;

  // Search functionality
  searchKeyword: string = '';
  searchSuggestions: PropertyListItem[] = [];
  showSuggestions: boolean = false;
  private searchSubject = new Subject<string>();

  constructor(
    private propertyService: PropertyService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadTop7Properties();
    this.loadPopularHotels();
    this.loadPopularApartments();
    this.loadPopularHouses();
    this.setupSearchSuggestions();
    this.startCarousel();
  }

  ngOnDestroy(): void {
    this.stopCarousel();
    this.searchSubject.complete();
  }

  // ==================== TOP 7 CAROUSEL ====================
  loadTop7Properties(): void {
    this.loadingTop7 = true;
    this.propertyService.getTop7Properties().subscribe({
      next: (response) => {
        if (response.code === 200 && response.data) {
          this.topProperties = response.data;
        }
        this.loadingTop7 = false;
      },
      error: (error) => {
        console.error('Error loading top 7 properties:', error);
        this.loadingTop7 = false;
      }
    });
  }

  startCarousel(): void {
    this.carouselInterval = setInterval(() => {
      this.nextSlide();
    }, 5000); // Auto-slide every 5 seconds
  }

  stopCarousel(): void {
    if (this.carouselInterval) {
      clearInterval(this.carouselInterval);
    }
  }

  nextSlide(): void {
    if (this.topProperties.length > 0) {
      const maxSlides = Math.min(5, this.topProperties.length);
      this.currentSlide = (this.currentSlide + 1) % maxSlides;
    }
  }

  previousSlide(): void {
    if (this.topProperties.length > 0) {
      const maxSlides = Math.min(5, this.topProperties.length);
      this.currentSlide = this.currentSlide === 0 
        ? maxSlides - 1 
        : this.currentSlide - 1;
    }
  }

  goToSlide(index: number): void {
    const maxSlides = Math.min(5, this.topProperties.length);
    if (index >= 0 && index < maxSlides) {
      this.currentSlide = index;
    }
  }

  // ==================== POPULAR PROPERTIES BY TYPE ====================
  loadPopularHotels(): void {
    this.loadingHotels = true;
    this.propertyService.getTop4ByType(2).subscribe({ // 2 = Hotel
      next: (response) => {
        if (response.code === 200 && response.data) {
          this.popularHotels = response.data;
        }
        this.loadingHotels = false;
      },
      error: (error) => {
        console.error('Error loading popular hotels:', error);
        this.loadingHotels = false;
      }
    });
  }

  loadPopularApartments(): void {
    this.loadingApartments = true;
    this.propertyService.getTop4ByType(0).subscribe({ // 0 = Apartment
      next: (response) => {
        if (response.code === 200 && response.data) {
          this.popularApartments = response.data;
        }
        this.loadingApartments = false;
      },
      error: (error) => {
        console.error('Error loading popular apartments:', error);
        this.loadingApartments = false;
      }
    });
  }

  loadPopularHouses(): void {
    this.loadingHouses = true;
    this.propertyService.getTop4ByType(1).subscribe({ // 1 = House
      next: (response) => {
        if (response.code === 200 && response.data) {
          this.popularHouses = response.data;
        }
        this.loadingHouses = false;
      },
      error: (error) => {
        console.error('Error loading popular houses:', error);
        this.loadingHouses = false;
      }
    });
  }

  // ==================== SEARCH FUNCTIONALITY ====================
  setupSearchSuggestions(): void {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(term => this.propertyService.searchPropertySuggestions(term))
    ).subscribe({
      next: (suggestions) => {
        this.searchSuggestions = suggestions;
        this.showSuggestions = suggestions.length > 0;
      },
      error: (error) => {
        console.error('Error fetching search suggestions:', error);
        this.searchSuggestions = [];
        this.showSuggestions = false;
      }
    });
  }

  onSearchInput(): void {
    this.searchSubject.next(this.searchKeyword);
  }

  onSearch(): void {
    if (this.searchKeyword.trim()) {
      this.showSuggestions = false;
      this.router.navigate(['/properties'], {
        queryParams: { search: this.searchKeyword.trim() }
      });
    }
  }

  onSuggestionClick(suggestion: PropertyListItem): void {
    this.showSuggestions = false;
    this.router.navigate(['/properties', suggestion.id]);
  }

  hideSuggestions(): void {
    setTimeout(() => {
      this.showSuggestions = false;
    }, 200);
  }

  goToEnhancedFilter(): void {
    this.router.navigate(['/properties']);
  }

  // ==================== UTILITIES ====================
  trackByPropertyId(index: number, property: Property | PropertyListItem): number {
    return property.id;
  }
}

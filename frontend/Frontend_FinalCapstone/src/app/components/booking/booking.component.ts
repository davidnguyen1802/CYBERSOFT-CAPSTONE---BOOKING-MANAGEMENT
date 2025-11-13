import { Component, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BookingService } from '../../services/booking.service';
import { TokenService } from '../../services/token.service';
import { PropertyService } from '../../services/property.service';
import { PromotionService } from '../../services/promotion.service'; // ⭐ NEW
import { BookingRequest } from '../../models/booking';
import { Property } from '../../models/property';
import { DateRange } from '../shared/date-range-picker/date-range-picker.component';
import { UserPromotionDTO, getPromotionStatus } from '../../models/user-promotion.dto'; // ⭐ NEW - Added helper
import { PromotionPreviewDTO } from '../../models/promotion-preview.dto'; // ⭐ NEW

@Component({
  selector: 'app-booking',
  templateUrl: './booking.component.html',
  styleUrls: ['./booking.component.scss']
})
export class BookingComponent implements OnInit, AfterViewInit {
  
  propertyId: number = 1;
  property: Property | null = null;
  token: string | null = '';
  loading: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';
  isPetChecked: boolean = false;

  // ⭐ NEW: Promotion-related properties
  myPromotions: UserPromotionDTO[] = [];
  selectedPromotionCode: string = '';
  promotionPreview: PromotionPreviewDTO | null = null;
  loadingPromotions: boolean = false;
  validatingPromotion: boolean = false;

  // Booking form data
  bookingData: BookingRequest = {
    propertyId: 1,
    checkIn: '',
    checkOut: '',
    numAdults: 2,
    numChildren: 0,
    num_infant: 0, // Changed from numInfant
    numPet: 0, // Added missing field
    notes: '',
    // ⭐ NEW FLOW: Promotion applied at booking creation (not during payment)
    promotionCode: '',
    originalAmount: 0
  };

  // Date range picker data
  today: Date;
  oneYearLater: Date;
  blockedDates: (Date | {start: Date; end: Date})[] = [];
  initialCheckIn?: Date;
  initialCheckOut?: Date;

  constructor(
    private bookingService: BookingService,
    private tokenService: TokenService,
    private propertyService: PropertyService,
    private promotionService: PromotionService, // ⭐ NEW
    private route: ActivatedRoute,
    private router: Router
  ) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    this.today = today;
    
    this.oneYearLater = new Date(today);
    this.oneYearLater.setFullYear(this.oneYearLater.getFullYear() + 1);
  }

  ngOnInit(): void {
    // Get property ID and other params from route if available
    this.route.queryParams.subscribe(params => {
      // Auto-fill propertyId (from detail-property, user doesn't need to input)
      if (params['propertyId']) {
        this.propertyId = parseInt(params['propertyId']);
        this.bookingData.propertyId = this.propertyId;
        
        // Load property details to get maxPets info
        this.loadPropertyDetails();
      }
      
      // Auto-fill check-in date (YYYY-MM-DD format from detail-property)
      if (params['checkIn']) {
        this.bookingData.checkIn = params['checkIn'];
        const checkInParts = params['checkIn'].split('-');
        if (checkInParts.length === 3) {
          const checkInDate = new Date(
            parseInt(checkInParts[0]),
            parseInt(checkInParts[1]) - 1,
            parseInt(checkInParts[2])
          );
          checkInDate.setHours(0, 0, 0, 0);
          this.initialCheckIn = checkInDate;
        }
      }
      
      // Auto-fill check-out date (YYYY-MM-DD format from detail-property)
      if (params['checkOut']) {
        this.bookingData.checkOut = params['checkOut'];
        const checkOutParts = params['checkOut'].split('-');
        if (checkOutParts.length === 3) {
          const checkOutDate = new Date(
            parseInt(checkOutParts[0]),
            parseInt(checkOutParts[1]) - 1,
            parseInt(checkOutParts[2])
          );
          checkOutDate.setHours(0, 0, 0, 0);
          this.initialCheckOut = checkOutDate;
        }
      }
      
      // Auto-fill guest counts from detail-property
      if (params['adults']) {
        this.bookingData.numAdults = parseInt(params['adults']) || 1;
      }
      if (params['children']) {
        this.bookingData.numChildren = parseInt(params['children']) || 0;
      }
      if (params['infants']) {
        this.bookingData.num_infant = parseInt(params['infants']) || 0;
      }
      if (params['pets']) {
        const petCount = parseInt(params['pets']) || 0;
        this.bookingData.numPet = petCount;
        // If pets > 0, auto-check the checkbox
        if (petCount > 0) {
          this.isPetChecked = true;
        }
      }
      
      // Legacy support: Parse dates from old format (DD/MM/YYYY - DD/MM/YYYY)
      if (params['dates'] && !this.bookingData.checkIn && !this.bookingData.checkOut) {
        const dates = params['dates'].split(' - ');
        if (dates.length === 2) {
          // Parse DD/MM/YYYY to YYYY-MM-DD for date input
          this.bookingData.checkIn = this.parseDateToYYYYMMDD(dates[0]);
          this.bookingData.checkOut = this.parseDateToYYYYMMDD(dates[1]);
          
          // Set initial dates for date range picker
          const checkInParts = this.bookingData.checkIn.split('-');
          const checkOutParts = this.bookingData.checkOut.split('-');
          if (checkInParts.length === 3) {
            const checkInDate = new Date(
              parseInt(checkInParts[0]),
              parseInt(checkInParts[1]) - 1,
              parseInt(checkInParts[2])
            );
            checkInDate.setHours(0, 0, 0, 0);
            this.initialCheckIn = checkInDate;
          }
          if (checkOutParts.length === 3) {
            const checkOutDate = new Date(
              parseInt(checkOutParts[0]),
              parseInt(checkOutParts[1]) - 1,
              parseInt(checkOutParts[2])
            );
            checkOutDate.setHours(0, 0, 0, 0);
            this.initialCheckOut = checkOutDate;
          }
        }
      }
    });

    // ⭐ NEW: Load user's promotions
    this.loadMyPromotions();

    // Get token from service
    this.token = this.tokenService.getToken();

    // Set default dates if not provided (tomorrow to day after tomorrow + 2 days)
    if (!this.bookingData.checkIn || !this.bookingData.checkOut) {
      const today = new Date();
      const tomorrow = new Date(today);
      tomorrow.setDate(tomorrow.getDate() + 1);
      tomorrow.setHours(0, 0, 0, 0);
      
      const dayAfter = new Date(today);
      dayAfter.setDate(dayAfter.getDate() + 3);
      dayAfter.setHours(0, 0, 0, 0);

      // Format as YYYY-MM-DD for date input (not ISO DateTime yet)
      const year = tomorrow.getFullYear();
      const month = String(tomorrow.getMonth() + 1).padStart(2, '0');
      const day = String(tomorrow.getDate()).padStart(2, '0');
      this.bookingData.checkIn = `${year}-${month}-${day}`;
      this.initialCheckIn = tomorrow;
      
      const year2 = dayAfter.getFullYear();
      const month2 = String(dayAfter.getMonth() + 1).padStart(2, '0');
      const day2 = String(dayAfter.getDate()).padStart(2, '0');
      this.bookingData.checkOut = `${year2}-${month2}-${day2}`;
      this.initialCheckOut = dayAfter;
    }

    // Load blocked dates for this property (optional - can be empty if API not available)
    this.loadBlockedDates();
  }

  ngAfterViewInit(): void {
    // Component initialization complete
  }

  loadPropertyDetails(): void {
    this.propertyService.getPropertyDetail(this.propertyId).subscribe({
      next: (response) => {
        if (response.code === 200 && response.data) {
          this.property = response.data;
          console.log('✅ Property loaded:', this.property);
        }
      },
      error: (error) => {
        console.error('❌ Error loading property:', error);
      }
    });
  }

  togglePetCheckbox(): void {
    if (!this.isPetChecked) {
      // If unchecking, reset pet count to 0
      this.bookingData.numPet = 0;
    } else {
      // If checking, set to 1 by default
      this.bookingData.numPet = 1;
    }
  }

  loadBlockedDates(): void {
    // Optional: Load blocked dates from API
    // For now, using empty array - can be extended to call getPropertyBookings API
    // and map confirmed/booked dates to blockedDates array
    this.blockedDates = [];
  }

  onRangeSelected(range: DateRange): void {
    if (range && range.checkIn && range.checkOut) {
      // Convert Date objects to YYYY-MM-DD format
      const checkInYear = range.checkIn.getFullYear();
      const checkInMonth = String(range.checkIn.getMonth() + 1).padStart(2, '0');
      const checkInDay = String(range.checkIn.getDate()).padStart(2, '0');
      this.bookingData.checkIn = `${checkInYear}-${checkInMonth}-${checkInDay}`;

      const checkOutYear = range.checkOut.getFullYear();
      const checkOutMonth = String(range.checkOut.getMonth() + 1).padStart(2, '0');
      const checkOutDay = String(range.checkOut.getDate()).padStart(2, '0');
      this.bookingData.checkOut = `${checkOutYear}-${checkOutMonth}-${checkOutDay}`;

      // Update initial dates for picker
      this.initialCheckIn = range.checkIn;
      this.initialCheckOut = range.checkOut;
    }
  }

  // Helper to format Date to ISO DateTime without timezone: "2025-11-01T14:00:00"
  formatToISODateTime(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
  }

  // ⭐ NEW: Helper to convert Date + time string to ISO format
  convertToISODateTime(date: Date, timeStr: string): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}T${timeStr}`;
  }

  // Helper to parse date from "DD/MM/YYYY" to YYYY-MM-DD format (for date input)
  parseDateToYYYYMMDD(dateStr: string): string {
    const parts = dateStr.trim().split('/');
    if (parts.length === 3) {
      const [day, month, year] = parts;
      return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
    }
    return dateStr;
  }

  // Convert date from input (YYYY-MM-DD) to ISO DateTime
  convertDateInputToISO(dateStr: string, isCheckIn: boolean = true): string {
    if (!dateStr) return dateStr;
    
    // If already in ISO DateTime format, return as is
    if (dateStr.includes('T')) return dateStr;
    
    // Convert YYYY-MM-DD to DateTime
    const [year, month, day] = dateStr.split('-');
    const hour = isCheckIn ? 14 : 11; // Check-in at 14:00, check-out at 11:00
    const date = new Date(parseInt(year), parseInt(month) - 1, parseInt(day), hour, 0, 0);
    return this.formatToISODateTime(date);
  }


  onSubmit(): void {
    // Validate
    if (!this.token) {
      this.errorMessage = 'Please login to create a booking';
      this.router.navigate(['/login']);
      return;
    }

    if (!this.bookingData.checkIn || !this.bookingData.checkOut) {
      this.errorMessage = 'Check-in and check-out dates are required';
      return;
    }

    if (new Date(this.bookingData.checkIn) >= new Date(this.bookingData.checkOut)) {
      this.errorMessage = 'Check-out date must be after check-in date';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    // Convert dates to ISO DateTime format
    const checkInISO = this.convertDateInputToISO(this.bookingData.checkIn, true);
    const checkOutISO = this.convertDateInputToISO(this.bookingData.checkOut, false);

    // Check availability first (optional, but recommended)
    this.bookingService.checkAvailability(
      this.bookingData.propertyId,
      checkInISO,
      checkOutISO
    ).subscribe({
      next: (availabilityResponse) => {
        if (availabilityResponse.code === 200 && !availabilityResponse.data) {
          // Property not available
          this.loading = false;
          this.errorMessage = 'Property is not available for the selected dates. Please choose different dates.';
          return;
        }
        
        // ⭐ NEW FLOW: Navigate to promotion selection instead of creating booking
        this.navigateToPromotionSelection(checkInISO, checkOutISO);
      },
      error: (error) => {
        console.error('Error checking availability:', error);
        // Continue anyway - backend will also check
        this.navigateToPromotionSelection(checkInISO, checkOutISO);
      }
    });
  }

  /**
   * ⭐ NEW FLOW v2.0: Navigate to promotion selection page
   * Store booking data and let user choose promotion on separate page
   */
  private navigateToPromotionSelection(checkInISO: string, checkOutISO: string): void {
    const userId = this.tokenService.getUserId();
    if (!userId) {
      this.loading = false;
      this.errorMessage = 'Please login to create a booking';
      this.router.navigate(['/login']);
      return;
    }

    // Calculate original amount (pricePerNight * numberOfNights)
    const checkIn = new Date(checkInISO);
    const checkOut = new Date(checkOutISO);
    const nights = Math.ceil((checkOut.getTime() - checkIn.getTime()) / (1000 * 60 * 60 * 24));
    const originalAmount = (this.property?.pricePerNight || 0) * nights;

    // Store booking data for promotion selection page
    const bookingData = {
      propertyId: this.bookingData.propertyId,
      checkInDate: checkInISO,    // ✅ For promotion validation
      checkOutDate: checkOutISO,  // ✅ For promotion validation
      checkIn: checkInISO,        // ✅ For booking creation
      checkOut: checkOutISO,      // ✅ For booking creation
      numAdults: this.bookingData.numAdults,
      numChildren: this.bookingData.numChildren || 0,
      num_infant: this.bookingData.num_infant || 0,
      num_pet: this.isPetChecked ? (this.bookingData.numPet || 1) : 0,
      notes: this.bookingData.notes || '',
      originalAmount: originalAmount,
      nights: nights,
      pricePerNight: this.property?.pricePerNight || 0,
      propertyName: this.property?.name || '',
      queryParams: this.route.snapshot.queryParams
    };

    console.log('📦 Storing booking data:', bookingData);
    this.bookingService.setPendingBookingData(bookingData);

    this.loading = false;

    // Navigate to promotion selection page
    this.router.navigate(['/booking/select-promotion']);
  }

  /**
   * OLD FLOW (Kept for reference - can be removed later)
   * This was the old direct booking creation
   */
  private createBookingOldFlow(checkInISO: string, checkOutISO: string): void {
    const userId = this.tokenService.getUserId();
    if (!userId) {
      this.loading = false;
      this.errorMessage = 'Please login to create a booking';
      this.router.navigate(['/login']);
      return;
    }

    // Convert dates to ISO DateTime format before sending to backend
    const bookingRequest: BookingRequest = {
      userId: userId, // Added userId
      ...this.bookingData,
      numPet: this.isPetChecked ? (this.bookingData.numPet || 1) : 0, // Only send pet count if checkbox is checked
      checkIn: checkInISO,
      checkOut: checkOutISO,
      // ⭐ NEW FLOW: Include promotion code and original amount
      promotionCode: this.bookingData.promotionCode || undefined,
      originalAmount: this.bookingData.originalAmount || undefined
    };

    console.log('📤 Sending booking request:', bookingRequest);
    console.log('   With promotion:', bookingRequest.promotionCode || 'None');
    if (bookingRequest.promotionCode) {
      console.log('   Original amount:', bookingRequest.originalAmount);
      console.log('   Expected discount:', this.promotionPreview?.discountAmount);
    }

    // ⭐ NEW FLOW: Promotion is applied HERE (during booking creation)
    // Backend will calculate discount and return discounted totalPrice
    this.bookingService.createBooking(bookingRequest).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.code === 201 && response.data) {
          // ⭐ UPDATE success message based on promotion
          if (bookingRequest.promotionCode && this.promotionPreview) {
            this.successMessage = `🎉 Booking created with promotion ${bookingRequest.promotionCode}! ` +
              `You saved ${this.promotionPreview.discountAmount?.toLocaleString('vi-VN')} VND. ` +
              `Waiting for host approval. You have 24 hours to pay after approval.`;
          } else {
            this.successMessage = 'Booking request submitted successfully! ' +
              'Please wait for host approval. You will be notified and have 24 hours to complete payment after approval.';
          }
          
          console.log('✅ Booking created:', response.data);
          console.log('   Status:', response.data.booking?.status);
          console.log('   Booking ID:', response.data.booking?.id);
          console.log('   Total Price:', response.data.booking?.totalPrice, '(discounted if promotion applied)');

          // Request-to-Book flow: NO immediate payment redirect
          // User will pay later from My Bookings page after host approves
          
          // ✅ FIXED: Show success message for 2 seconds, then redirect immediately
          setTimeout(() => {
            this.router.navigate(['/my-bookings']);
          }, 2000);
        } else {
          this.errorMessage = response.message || 'Failed to create booking request';
        }
      },
      error: (error) => {
        this.loading = false;
        console.error('❌ Booking request error:', error);
        this.errorMessage = error.error?.message || 'An error occurred while creating booking request';
      }
    });
  }

  // ⭐ NEW: Load user's promotions
  loadMyPromotions(): void {
    const userId = this.tokenService.getUserId();
    if (!userId) {
      // User not logged in, skip loading promotions
      return;
    }

    this.loadingPromotions = true;
    this.promotionService.getMyPromotions(0, 100).subscribe({
      next: (response) => {
        if (response.code === 200) {
          // ✅ FIXED: Use helper function to filter ACTIVE promotions
          this.myPromotions = response.data.content.filter(p => getPromotionStatus(p) === 'ACTIVE');
          console.log('✅ Loaded', this.myPromotions.length, 'active promotions');
        }
        this.loadingPromotions = false;
      },
      error: (error) => {
        console.error('❌ Error loading promotions:', error);
        this.loadingPromotions = false;
      }
    });
  }

  // ⭐ OLD FLOW: Handle promotion selection (DEPRECATED - Now done in separate page)
  // This method is kept for reference but no longer used in v2.0 flow
  /*
  selectPromotion(code: string): void {
    this.selectedPromotionCode = code;
    
    if (!code) {
      // User deselected promotion
      this.promotionPreview = null;
      this.bookingData.promotionCode = undefined;
      this.bookingData.originalAmount = undefined;
      return;
    }

    // Validate dates are selected
    if (!this.bookingData.checkIn || !this.bookingData.checkOut) {
      this.errorMessage = 'Please select check-in and check-out dates first';
      this.selectedPromotionCode = '';
      return;
    }

    // Validate promotion
    this.validatePromotionPreview();
  }
  */

  // ⭐ OLD FLOW: Validate promotion and show preview (DEPRECATED)
  // Now validation is done in PromotionSelectionComponent
  /*
  validatePromotionPreview(): void {
    if (!this.selectedPromotionCode) return;
    if (!this.property) {
      this.errorMessage = 'Property information not loaded';
      return;
    }

    this.validatingPromotion = true;
    this.errorMessage = '';
    
    // Convert dates to ISO format for validation
    const checkInDate = new Date(this.bookingData.checkIn);
    const checkOutDate = new Date(this.bookingData.checkOut);
    const checkInISO = this.convertToISODateTime(checkInDate, '15:00:00');
    const checkOutISO = this.convertToISODateTime(checkOutDate, '11:00:00');
    
    this.promotionService.validatePromotionByCode(
      this.selectedPromotionCode,
      this.property.id,
      checkInISO,
      checkOutISO
    ).subscribe({
      next: (response) => {
        this.validatingPromotion = false;
        if (response.code === 200 && response.data && response.data.valid === true) {  // ✅ Changed from isValid to valid
          this.promotionPreview = response.data;
          
          // ⭐ SAVE for booking creation
          this.bookingData.promotionCode = this.selectedPromotionCode;
          this.bookingData.originalAmount = response.data.originalAmount;
          
          console.log('✅ Promotion validated:', response.data);
          console.log('   Original:', response.data.originalAmount);
          console.log('   Discount:', response.data.discountAmount);
          console.log('   Final:', response.data.finalAmount);
        } else {
          const errorMsg = response.data?.errorMessage || 'Promotion is invalid';
          this.errorMessage = errorMsg;
          this.promotionPreview = null;
          this.selectedPromotionCode = '';
          this.bookingData.promotionCode = undefined;
          this.bookingData.originalAmount = undefined;
        }
      },
      error: (error) => {
        this.validatingPromotion = false;
        console.error('❌ Validation error:', error);
        this.errorMessage = error.error?.message || 'Failed to validate promotion';
        this.promotionPreview = null;
        this.selectedPromotionCode = '';
      }
    });
  }
  */

  // ⭐ OLD FLOW: Clear promotion selection (DEPRECATED)
  /*
  clearPromotion(): void {
    this.selectedPromotionCode = '';
    this.promotionPreview = null;
    this.bookingData.promotionCode = undefined;
    this.bookingData.originalAmount = undefined;
  }
  */
}

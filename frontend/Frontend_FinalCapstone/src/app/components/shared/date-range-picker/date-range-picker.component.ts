import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy,
  ViewChild,
  ElementRef,
  forwardRef,
  HostListener,
  ChangeDetectorRef,
  AfterViewInit
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  FormControl,
  Validators,
  AbstractControl,
  ValidationErrors
} from '@angular/forms';
import { Subject, Subscription } from 'rxjs';

export interface DateRange {
  checkIn: Date;
  checkOut: Date;
}

@Component({
  selector: 'app-date-range-picker',
  templateUrl: './date-range-picker.component.html',
  styleUrls: ['./date-range-picker.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DateRangePickerComponent),
      multi: true
    }
  ]
})
export class DateRangePickerComponent implements OnInit, OnDestroy, AfterViewInit, ControlValueAccessor {
  @Input() minDate?: Date;
  @Input() maxDate?: Date;
  @Input() minNights: number = 1;
  @Input() maxNights: number = 30;
  @Input() blockedDates: (Date | {start: Date; end: Date})[] = [];
  @Input() initialCheckIn?: Date;
  @Input() initialCheckOut?: Date;

  @Output() rangeSelected = new EventEmitter<DateRange>();

  @ViewChild('calendarContainer', { static: false }) calendarContainer!: ElementRef;

  // Internal state
  isOpen = false;
  checkIn: Date | null = null;
  checkOut: Date | null = null;
  hoveredDate: Date | null = null;
  currentMonth1: Date;
  currentMonth2: Date;
  
  // Calendar data
  weekDays: string[] = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  calendarDays1: Date[] = [];
  calendarDays2: Date[] = [];

  // Quick picks
  quickPicks = [
    { label: 'Today', days: 0 },
    { label: '+7 days', days: 7 },
    { label: 'This Weekend', custom: true }
  ];

  // ControlValueAccessor
  private onChange = (value: DateRange | null) => {};
  private onTouched = () => {};
  disabled = false;

  private subscription = new Subscription();

  constructor(private cdr: ChangeDetectorRef) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    // Default min/max dates
    this.minDate = this.minDate || today;
    const oneYearLater = new Date(today);
    oneYearLater.setFullYear(oneYearLater.getFullYear() + 1);
    this.maxDate = this.maxDate || oneYearLater;

    // Initialize current months (2 months side by side)
    this.currentMonth1 = new Date(today);
    this.currentMonth2 = new Date(today);
    this.currentMonth2.setMonth(this.currentMonth2.getMonth() + 1);
  }

  ngOnInit(): void {
    // Set initial dates if provided
    if (this.initialCheckIn) {
      this.checkIn = new Date(this.initialCheckIn);
      this.checkIn.setHours(0, 0, 0, 0);
    }
    if (this.initialCheckOut) {
      this.checkOut = new Date(this.initialCheckOut);
      this.checkOut.setHours(0, 0, 0, 0);
    }

    // Build calendars
    this.buildCalendars();
  }

  ngAfterViewInit(): void {
    // Handle click outside to close
    document.addEventListener('click', this.handleClickOutside.bind(this));
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
    document.removeEventListener('click', this.handleClickOutside.bind(this));
  }

  private handleClickOutside(event: MouseEvent): void {
    if (this.calendarContainer && !this.calendarContainer.nativeElement.contains(event.target)) {
      if (this.isOpen && (!this.checkIn || (this.checkIn && this.checkOut))) {
        this.closeCalendar();
      }
    }
  }

  buildCalendars(): void {
    this.calendarDays1 = this.buildCalendarDays(this.currentMonth1);
    this.calendarDays2 = this.buildCalendarDays(this.currentMonth2);
  }

  buildCalendarDays(month: Date): Date[] {
    const year = month.getFullYear();
    const monthIndex = month.getMonth();
    
    // First day of month
    const firstDay = new Date(year, monthIndex, 1);
    const firstDayOfWeek = firstDay.getDay();
    
    // Last day of month
    const lastDay = new Date(year, monthIndex + 1, 0);
    const daysInMonth = lastDay.getDate();
    
    const days: Date[] = [];
    
    // Add previous month's trailing days
    const prevMonth = new Date(year, monthIndex, 0);
    const daysInPrevMonth = prevMonth.getDate();
    for (let i = firstDayOfWeek - 1; i >= 0; i--) {
      const date = new Date(year, monthIndex - 1, daysInPrevMonth - i);
      days.push(date);
    }
    
    // Add current month's days
    for (let day = 1; day <= daysInMonth; day++) {
      const date = new Date(year, monthIndex, day);
      days.push(date);
    }
    
    // Add next month's leading days to fill 6 weeks
    const remainingDays = 42 - days.length;
    for (let day = 1; day <= remainingDays; day++) {
      const date = new Date(year, monthIndex + 1, day);
      days.push(date);
    }
    
    return days;
  }

  prevMonth(): void {
    this.currentMonth1.setMonth(this.currentMonth1.getMonth() - 1);
    // currentMonth2 luôn là tháng sau currentMonth1
    const year1 = this.currentMonth1.getFullYear();
    const month1 = this.currentMonth1.getMonth();
    this.currentMonth2 = new Date(year1, month1 + 1, 1);
    this.buildCalendars();
  }

  nextMonth(): void {
    this.currentMonth1.setMonth(this.currentMonth1.getMonth() + 1);
    // currentMonth2 luôn là tháng sau currentMonth1
    const year1 = this.currentMonth1.getFullYear();
    const month1 = this.currentMonth1.getMonth();
    this.currentMonth2 = new Date(year1, month1 + 1, 1);
    this.buildCalendars();
  }

  getMonthYearString(date: Date): string {
    return date.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  }

  getMonthYearShortString(date: Date): string {
    return date.toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
  }

  getYearRange(): number[] {
    const minYear = this.minDate ? this.minDate.getFullYear() : new Date().getFullYear();
    const maxYear = this.maxDate ? this.maxDate.getFullYear() : new Date().getFullYear() + 2;
    const years: number[] = [];
    for (let i = minYear; i <= maxYear; i++) {
      years.push(i);
    }
    return years;
  }

  getMonthNames(): string[] {
    return [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ];
  }

  onMonthChange(monthIndex: number): void {
    this.currentMonth1.setMonth(monthIndex);
    // currentMonth2 luôn là tháng sau currentMonth1
    const year1 = this.currentMonth1.getFullYear();
    const month1 = this.currentMonth1.getMonth();
    this.currentMonth2 = new Date(year1, month1 + 1, 1);
    this.buildCalendars();
  }

  onYearChange(year: number): void {
    this.currentMonth1.setFullYear(year);
    // currentMonth2 luôn là tháng sau currentMonth1
    const month1 = this.currentMonth1.getMonth();
    this.currentMonth2 = new Date(year, month1 + 1, 1);
    this.buildCalendars();
  }

  isDateDisabled(date: Date): boolean {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    // Check if before min date
    if (this.minDate && date < this.minDate) {
      return true;
    }
    
    // Check if after max date
    if (this.maxDate && date > this.maxDate) {
      return true;
    }
    
    // Check if in blocked dates
    if (this.isDateBlocked(date)) {
      return true;
    }
    
    return false;
  }

  isDateBlocked(date: Date): boolean {
    for (const blocked of this.blockedDates) {
      if (blocked instanceof Date) {
        const blockedDate = new Date(blocked);
        blockedDate.setHours(0, 0, 0, 0);
        if (this.isSameDate(date, blockedDate)) {
          return true;
        }
      } else if (blocked.start && blocked.end) {
        const start = new Date(blocked.start);
        const end = new Date(blocked.end);
        start.setHours(0, 0, 0, 0);
        end.setHours(0, 0, 0, 0);
        if (date >= start && date <= end) {
          return true;
        }
      }
    }
    return false;
  }

  isSameDate(date1: Date, date2: Date): boolean {
    return date1.getFullYear() === date2.getFullYear() &&
           date1.getMonth() === date2.getMonth() &&
           date1.getDate() === date2.getDate();
  }

  onDateClick(date: Date): void {
    if (this.isDateDisabled(date)) {
      return;
    }

    if (!this.checkIn || (this.checkIn && this.checkOut)) {
      // Start new selection
      this.checkIn = new Date(date);
      this.checkIn.setHours(0, 0, 0, 0);
      this.checkOut = null;
      this.hoveredDate = null;
    } else if (this.checkIn && !this.checkOut) {
      // Select end date
      if (date <= this.checkIn) {
        // If clicked before check-in, reset to new check-in
        this.checkIn = new Date(date);
        this.checkIn.setHours(0, 0, 0, 0);
        this.checkOut = null;
      } else {
        // Validate min/max nights
        const nights = Math.floor((date.getTime() - this.checkIn.getTime()) / (1000 * 60 * 60 * 24));
        
        if (nights < this.minNights) {
          // Auto-snap to min nights
          this.checkOut = new Date(this.checkIn);
          this.checkOut.setDate(this.checkOut.getDate() + this.minNights);
          this.checkOut.setHours(0, 0, 0, 0);
        } else if (nights > this.maxNights) {
          // Auto-snap to max nights
          this.checkOut = new Date(this.checkIn);
          this.checkOut.setDate(this.checkOut.getDate() + this.maxNights);
          this.checkOut.setHours(0, 0, 0, 0);
        } else {
          // Validate no blocked dates in range
          if (this.hasBlockedDatesInRange(this.checkIn, date)) {
            // Find next available date
            let nextDate = new Date(this.checkIn);
            nextDate.setDate(nextDate.getDate() + this.minNights);
            while (this.isDateBlocked(nextDate) && nextDate <= date) {
              nextDate.setDate(nextDate.getDate() + 1);
            }
            if (nextDate <= this.maxDate!) {
              this.checkOut = nextDate;
              this.checkOut.setHours(0, 0, 0, 0);
            }
          } else {
            this.checkOut = new Date(date);
            this.checkOut.setHours(0, 0, 0, 0);
          }
        }
        
        // Emit and close if valid
        if (this.checkIn && this.checkOut) {
          this.emitRange();
          this.closeCalendar();
        }
      }
    }
    
    this.cdr.detectChanges();
  }

  onDateHover(date: Date): void {
    if (this.checkIn && !this.checkOut && !this.isDateDisabled(date)) {
      this.hoveredDate = new Date(date);
      this.hoveredDate.setHours(0, 0, 0, 0);
    }
  }

  onDateLeave(): void {
    this.hoveredDate = null;
  }

  hasBlockedDatesInRange(start: Date, end: Date): boolean {
    const checkDate = new Date(start);
    checkDate.setDate(checkDate.getDate() + 1); // Skip check-in day
    
    while (checkDate < end) {
      if (this.isDateBlocked(checkDate)) {
        return true;
      }
      checkDate.setDate(checkDate.getDate() + 1);
    }
    return false;
  }

  isInRange(date: Date): boolean {
    if (!this.checkIn) return false;
    
    if (this.checkOut) {
      return date > this.checkIn && date < this.checkOut;
    }
    
    if (this.hoveredDate && this.hoveredDate > this.checkIn) {
      return date > this.checkIn && date < this.hoveredDate;
    }
    
    return false;
  }

  isRangeStart(date: Date): boolean {
    return this.checkIn !== null && this.isSameDate(date, this.checkIn);
  }

  isRangeEnd(date: Date): boolean {
    return this.checkOut !== null && this.isSameDate(date, this.checkOut);
  }

  isInOtherMonth(date: Date, month: Date): boolean {
    return date.getMonth() !== month.getMonth();
  }

  isToday(date: Date): boolean {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const checkDate = new Date(date);
    checkDate.setHours(0, 0, 0, 0);
    return this.isSameDate(checkDate, today);
  }

  isSelectionValid(): boolean {
    if (!this.checkIn || !this.checkOut) {
      return false;
    }

    // Check min/max nights
    const nights = this.getNightsCount();
    if (nights < this.minNights || nights > this.maxNights) {
      return false;
    }

    // Check if there are blocked dates in range
    if (this.hasBlockedDatesInRange(this.checkIn, this.checkOut)) {
      return false;
    }

    // Check if check-in or check-out is blocked
    if (this.isDateBlocked(this.checkIn) || this.isDateBlocked(this.checkOut)) {
      return false;
    }

    // Check if dates are disabled
    if (this.isDateDisabled(this.checkIn) || this.isDateDisabled(this.checkOut)) {
      return false;
    }

    return true;
  }

  onConfirmClick(): void {
    if (this.isSelectionValid()) {
      this.emitRange();
      this.closeCalendar();
    }
  }

  getNightsCount(): number {
    if (!this.checkIn || !this.checkOut) return 0;
    return Math.floor((this.checkOut.getTime() - this.checkIn.getTime()) / (1000 * 60 * 60 * 24));
  }

  clearSelection(): void {
    this.checkIn = null;
    this.checkOut = null;
    this.hoveredDate = null;
    this.emitRange();
    this.cdr.detectChanges();
  }

  applyQuickPick(pick: any): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    if (pick.custom && pick.label === 'This Weekend') {
      const dayOfWeek = today.getDay();
      const daysUntilSaturday = 6 - dayOfWeek;
      const saturday = new Date(today);
      saturday.setDate(saturday.getDate() + daysUntilSaturday);
      
      this.checkIn = new Date(saturday);
      this.checkIn.setHours(0, 0, 0, 0);
      this.checkOut = new Date(saturday);
      this.checkOut.setDate(this.checkOut.getDate() + 1);
      this.checkOut.setHours(0, 0, 0, 0);
    } else if (pick.days !== undefined) {
      const startDate = new Date(today);
      startDate.setDate(startDate.getDate() + pick.days);
      startDate.setHours(0, 0, 0, 0);
      
      this.checkIn = startDate;
      this.checkOut = new Date(startDate);
      this.checkOut.setDate(this.checkOut.getDate() + this.minNights);
      this.checkOut.setHours(0, 0, 0, 0);
    }
    
    this.emitRange();
    this.closeCalendar();
    this.cdr.detectChanges();
  }

  private emitRange(): void {
    if (this.checkIn && this.checkOut) {
      const range: DateRange = {
        checkIn: new Date(this.checkIn),
        checkOut: new Date(this.checkOut)
      };
      
      this.onChange(range);
      this.rangeSelected.emit(range);
    } else {
      this.onChange(null);
    }
  }

  toggleCalendar(): void {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      // Reset months to show current selection or today
      if (this.checkIn) {
        this.currentMonth1 = new Date(this.checkIn);
        this.currentMonth2 = new Date(this.checkIn);
        this.currentMonth2.setMonth(this.currentMonth2.getMonth() + 1);
      } else {
        const today = new Date();
        this.currentMonth1 = new Date(today);
        this.currentMonth2 = new Date(today);
        this.currentMonth2.setMonth(this.currentMonth2.getMonth() + 1);
      }
      this.buildCalendars();
    }
  }

  closeCalendar(): void {
    this.isOpen = false;
    this.hoveredDate = null;
  }

  getDisplayText(): string {
    if (this.checkIn && this.checkOut) {
      const checkInStr = this.checkIn.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
      const checkOutStr = this.checkOut.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
      return `${checkInStr} - ${checkOutStr}`;
    } else if (this.checkIn) {
      return this.checkIn.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    }
    return '';
  }

  // ControlValueAccessor implementation
  writeValue(value: DateRange | null): void {
    if (value) {
      this.checkIn = value.checkIn ? new Date(value.checkIn) : null;
      this.checkOut = value.checkOut ? new Date(value.checkOut) : null;
      if (this.checkIn) this.checkIn.setHours(0, 0, 0, 0);
      if (this.checkOut) this.checkOut.setHours(0, 0, 0, 0);
    } else {
      this.checkIn = null;
      this.checkOut = null;
    }
    this.cdr.detectChanges();
  }

  registerOnChange(fn: (value: DateRange | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  @HostListener('keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent): void {
    if (!this.isOpen) return;

    switch (event.key) {
      case 'Escape':
        this.closeCalendar();
        event.preventDefault();
        break;
      case 'ArrowLeft':
        this.prevMonth();
        event.preventDefault();
        break;
      case 'ArrowRight':
        this.nextMonth();
        event.preventDefault();
        break;
    }
  }
}


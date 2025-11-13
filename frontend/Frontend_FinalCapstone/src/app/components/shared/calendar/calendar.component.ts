import { Component, EventEmitter, Input, OnInit, OnChanges, SimpleChanges, Output, ChangeDetectionStrategy } from '@angular/core';
import { Booking, BookingStatus } from '../../../models/booking';
import { format, isSameDay, isToday as isTodayFn, isSameMonth } from 'date-fns';
import {
  buildWeeksForMonth,
  getSegmentsForWeek,
  assignLanes,
  CalendarWeek,
  CalendarSegment,
  getCalendarDateRange
} from './booking-layout.util';

export type ViewMode = 'Day' | 'Week' | 'Month';

@Component({
  selector: 'app-calendar',
  templateUrl: './calendar.component.html',
  styleUrls: ['./calendar.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CalendarComponent implements OnInit, OnChanges {
  @Input() bookings: Booking[] = [];
  @Input() selectedStatuses: string[] = [];
  @Input() statusOptions: any[] = [];
  @Input() totalElements: number = 0;
  @Output() onDateClick = new EventEmitter<Date>();
  @Output() onStatusChange = new EventEmitter<string>();
  @Output() onBookingClick = new EventEmitter<Booking>();

  currentDate: Date = new Date();
  viewMode: ViewMode = 'Month';
  weekDays = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
  
  // Calendar weeks and segments
  weeks: CalendarWeek[] = [];
  segmentsByWeek: Map<string, CalendarSegment[]> = new Map();
  maxLanesPerWeek: Map<string, number> = new Map();
  
  // Month mini calendar for sidebar
  miniCalendarDate: Date = new Date();
  miniCalendarDays: Array<{
    date: Date;
    dayNumber: number;
    isCurrentMonth: boolean;
    isToday: boolean;
  }> = [];

  // Expose Math and utilities for template
  Math = Math;
  
  get currentMonthYear(): string {
    return format(this.currentDate, 'MMMM yyyy');
  }

  ngOnInit() {
    this.generateCalendar();
    this.generateMiniCalendar();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['bookings'] && !changes['bookings'].firstChange) {
      console.log('📅 Calendar bookings changed, regenerating layout');
      this.generateCalendar();
    }
  }

  generateCalendar() {
    console.log('🔄 Generating calendar for:', format(this.currentDate, 'MMMM yyyy'));
    console.log('📊 Total bookings:', this.bookings.length);

    // Build weeks for month view
    this.weeks = buildWeeksForMonth(this.currentDate);
    console.log('📅 Weeks in view:', this.weeks.length);

    // Clear previous data
    this.segmentsByWeek.clear();
    this.maxLanesPerWeek.clear();

    // Process each week
    for (const week of this.weeks) {
      const segments = getSegmentsForWeek(this.bookings, week);
      const segmentsWithLanes = assignLanes(segments);
      
      this.segmentsByWeek.set(week.weekStartStr, segmentsWithLanes);
      
      const maxLane = segmentsWithLanes.length > 0
        ? Math.max(...segmentsWithLanes.map(s => s.lane))
        : -1;
      this.maxLanesPerWeek.set(week.weekStartStr, maxLane + 1);
      
      console.log(`  Week ${week.weekStartStr}: ${segments.length} bookings, ${maxLane + 1} lanes`);
    }
  }

  getSegmentsForWeek(weekStartStr: string): CalendarSegment[] {
    return this.segmentsByWeek.get(weekStartStr) || [];
  }

  getMaxLanesForWeek(weekStartStr: string): number {
    return this.maxLanesPerWeek.get(weekStartStr) || 1;
  }

  getLaneRowsStyle(weekStartStr: string): string {
    const lanes = this.getMaxLanesForWeek(weekStartStr);
    return `repeat(${lanes}, 28px)`;
  }

  formatDate(dateStr: string | Date): string {
    const date = typeof dateStr === 'string' ? new Date(dateStr) : dateStr;
    return format(date, 'MMM dd, yyyy');
  }

  formatPrice(price?: number): string {
    if (!price) return 'N/A';
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(price);
  }

  isToday(date: Date): boolean {
    return isTodayFn(date);
  }

  isSameMonthAsView(date: Date): boolean {
    return isSameMonth(date, this.currentDate);
  }

  getDayNumber(date: Date): number {
    return date.getDate();
  }

  previousPeriod() {
    if (this.viewMode === 'Month') {
      this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() - 1, 1);
    } else if (this.viewMode === 'Week') {
      this.currentDate = new Date(this.currentDate.getTime() - 7 * 24 * 60 * 60 * 1000);
    } else {
      this.currentDate = new Date(this.currentDate.getTime() - 24 * 60 * 60 * 1000);
    }
    this.generateCalendar();
  }

  nextPeriod() {
    if (this.viewMode === 'Month') {
      this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() + 1, 1);
    } else if (this.viewMode === 'Week') {
      this.currentDate = new Date(this.currentDate.getTime() + 7 * 24 * 60 * 60 * 1000);
    } else {
      this.currentDate = new Date(this.currentDate.getTime() + 24 * 60 * 60 * 1000);
    }
    this.generateCalendar();
  }

  setViewMode(mode: ViewMode) {
    this.viewMode = mode;
    this.generateCalendar();
  }

  handleDateClick(date: Date) {
    this.onDateClick.emit(date);
  }

  handleStatusChange(status: string) {
    this.onStatusChange.emit(status);
  }

  handleBookingClick(segment: CalendarSegment) {
    this.onBookingClick.emit(segment.booking);
  }

  getBookingCountByStatus(statusValue: string): number {
    if (statusValue === 'ALL') {
      return this.selectedStatuses.length === 0 ? this.totalElements : this.bookings.length;
    }
    return this.bookings.filter(b => b.status === statusValue).length;
  }

  isStatusSelected(status: string): boolean {
    if (status === 'ALL') {
      return this.selectedStatuses.length === 0;
    }
    return this.selectedStatuses.includes(status);
  }

  // Mini calendar
  generateMiniCalendar() {
    this.miniCalendarDays = [];
    const year = this.miniCalendarDate.getFullYear();
    const month = this.miniCalendarDate.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const startDay = firstDay.getDay() === 0 ? 6 : firstDay.getDay() - 1;
    const daysInMonth = lastDay.getDate();

    // Previous month days
    const prevMonthLastDay = new Date(year, month, 0).getDate();
    for (let i = startDay - 1; i >= 0; i--) {
      const date = new Date(year, month - 1, prevMonthLastDay - i);
      this.miniCalendarDays.push({
        date,
        dayNumber: date.getDate(),
        isCurrentMonth: false,
        isToday: isTodayFn(date)
      });
    }

    // Current month days
    for (let day = 1; day <= daysInMonth; day++) {
      const date = new Date(year, month, day);
      this.miniCalendarDays.push({
        date,
        dayNumber: day,
        isCurrentMonth: true,
        isToday: isTodayFn(date)
      });
    }

    // Next month days
    const remainingDays = 42 - this.miniCalendarDays.length;
    for (let day = 1; day <= remainingDays; day++) {
      const date = new Date(year, month + 1, day);
      this.miniCalendarDays.push({
        date,
        dayNumber: day,
        isCurrentMonth: false,
        isToday: isTodayFn(date)
      });
    }
  }

  trackByWeek(index: number, week: CalendarWeek): string {
    return week.weekStartStr;
  }

  trackByDay(index: number, date: Date): string {
    return format(date, 'yyyy-MM-dd');
  }

  trackBySegment(index: number, segment: CalendarSegment): string {
    return `${segment.bookingId}-${segment.weekStart}`;
  }
}

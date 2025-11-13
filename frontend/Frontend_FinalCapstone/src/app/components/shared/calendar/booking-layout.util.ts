import { 
  startOfWeek, 
  endOfWeek, 
  eachWeekOfInterval, 
  startOfMonth, 
  endOfMonth,
  parseISO,
  differenceInDays,
  max as maxDate,
  min as minDate,
  addDays,
  getDay,
  isBefore,
  isAfter,
  isEqual,
  format
} from 'date-fns';
import { Booking, BookingStatus } from '../../../models/booking';

/**
 * Segment of a booking within a single week
 * Designed for CSS Grid positioning
 */
export interface CalendarSegment {
  bookingId: number;
  booking: Booking; // Full booking data for tooltip
  weekStart: string; // yyyy-MM-dd
  startCol: number; // 1..7 (Mon=1, Sun=7)
  span: number; // 1..7
  lane: number; // 0-based row index for stacking
  status: BookingStatus;
  label: string; // Guest name to display
  colorClass: string; // CSS class for status color
}

/**
 * Week definition for calendar grid
 */
export interface CalendarWeek {
  weekStart: Date;
  weekEnd: Date;
  weekStartStr: string; // yyyy-MM-dd
  days: Date[]; // 7 days Mon-Sun
}

/**
 * Convert day-of-week to column index (Mon=1, Sun=7)
 */
function dayToColumn(date: Date): number {
  const day = getDay(date);
  // getDay: Sun=0, Mon=1, ..., Sat=6
  // Convert to Mon=1, ..., Sun=7
  return day === 0 ? 7 : day;
}

/**
 * Check if booking intersects with week
 */
function bookingIntersectsWeek(booking: Booking, week: CalendarWeek): boolean {
  const checkIn = parseISO(booking.checkIn);
  const checkOut = parseISO(booking.checkOut);
  
  // Booking overlaps week if:
  // checkIn < weekEnd AND checkOut > weekStart
  return isBefore(checkIn, week.weekEnd) && isAfter(checkOut, week.weekStart);
}

/**
 * Build week grid for month view
 * Returns array of weeks covering the month + surrounding days to fill grid
 */
export function buildWeeksForMonth(viewDate: Date): CalendarWeek[] {
  const monthStart = startOfMonth(viewDate);
  const monthEnd = endOfMonth(viewDate);
  
  // Extend to Mon-Sun boundaries
  const from = startOfWeek(monthStart, { weekStartsOn: 1 }); // Monday
  const to = endOfWeek(monthEnd, { weekStartsOn: 1 }); // Sunday
  
  const weekStarts = eachWeekOfInterval(
    { start: from, end: to },
    { weekStartsOn: 1 }
  );
  
  return weekStarts.map(weekStart => {
    const weekEnd = endOfWeek(weekStart, { weekStartsOn: 1 });
    const days: Date[] = [];
    for (let i = 0; i < 7; i++) {
      days.push(addDays(weekStart, i));
    }
    
    return {
      weekStart,
      weekEnd,
      weekStartStr: format(weekStart, 'yyyy-MM-dd'),
      days
    };
  });
}

/**
 * Split booking into segment for specific week
 * Returns null if booking doesn't intersect week
 */
function splitBookingForWeek(booking: Booking, week: CalendarWeek): CalendarSegment | null {
  const checkIn = parseISO(booking.checkIn);
  const checkOut = parseISO(booking.checkOut);
  
  if (!bookingIntersectsWeek(booking, week)) {
    return null;
  }
  
  // Clamp to week boundaries
  const segmentStart = maxDate([checkIn, week.weekStart]);
  const segmentEnd = minDate([checkOut, addDays(week.weekEnd, 1)]); // end-exclusive
  
  const startCol = dayToColumn(segmentStart);
  const endCol = dayToColumn(segmentEnd);
  
  // Calculate span
  let span: number;
  if (isBefore(segmentEnd, addDays(week.weekEnd, 1)) || isEqual(segmentEnd, addDays(week.weekEnd, 1))) {
    // Ends within or exactly at week boundary
    const daysDiff = differenceInDays(segmentEnd, segmentStart);
    span = daysDiff > 0 ? daysDiff : 1;
  } else {
    // Extends beyond week - span to end of week
    span = 8 - startCol; // From startCol to Sunday
  }
  
  // Clamp span to not exceed week
  span = Math.min(span, 8 - startCol);
  span = Math.max(span, 1);
  
  return {
    bookingId: booking.id!,
    booking,
    weekStart: week.weekStartStr,
    startCol,
    span,
    lane: 0, // Will be assigned later
    status: booking.status!,
    label: booking.userName || `User #${booking.userId}`,
    colorClass: getStatusColorClass(booking.status!)
  };
}

/**
 * Get all segments for a week from bookings list
 */
export function getSegmentsForWeek(bookings: Booking[], week: CalendarWeek): CalendarSegment[] {
  const segments: CalendarSegment[] = [];
  
  for (const booking of bookings) {
    const segment = splitBookingForWeek(booking, week);
    if (segment) {
      segments.push(segment);
    }
  }
  
  return segments;
}

/**
 * Assign lanes to segments to avoid overlap (Interval Graph Coloring)
 * Uses greedy algorithm: sort by startCol, then assign to lowest available lane
 */
export function assignLanes(segments: CalendarSegment[]): CalendarSegment[] {
  if (segments.length === 0) return [];
  
  // Sort by startCol, then by span (longer first)
  const sorted = [...segments].sort((a, b) => {
    if (a.startCol !== b.startCol) return a.startCol - b.startCol;
    return b.span - a.span;
  });
  
  // Track occupied intervals per lane
  // lanes[i] = array of { start, end } intervals
  const lanes: Array<Array<{ start: number; end: number }>> = [];
  
  for (const segment of sorted) {
    const segStart = segment.startCol;
    const segEnd = segment.startCol + segment.span - 1;
    
    // Find first available lane
    let assignedLane = -1;
    for (let laneIdx = 0; laneIdx < lanes.length; laneIdx++) {
      const laneIntervals = lanes[laneIdx];
      
      // Check if segment overlaps any interval in this lane
      const overlaps = laneIntervals.some(interval => {
        // Overlap if: segStart <= interval.end AND segEnd >= interval.start
        return segStart <= interval.end && segEnd >= interval.start;
      });
      
      if (!overlaps) {
        assignedLane = laneIdx;
        break;
      }
    }
    
    // If no lane available, create new lane
    if (assignedLane === -1) {
      assignedLane = lanes.length;
      lanes.push([]);
    }
    
    // Assign lane and add interval
    segment.lane = assignedLane;
    lanes[assignedLane].push({ start: segStart, end: segEnd });
  }
  
  return sorted;
}

/**
 * Map booking status to CSS class
 */
function getStatusColorClass(status: BookingStatus): string {
  const map: Record<BookingStatus, string> = {
    [BookingStatus.PENDING]: 'status-pending',
    [BookingStatus.CONFIRMED]: 'status-confirmed',
    [BookingStatus.PAID]: 'status-paid',
    [BookingStatus.COMPLETED]: 'status-completed',
    [BookingStatus.CANCELLED]: 'status-cancelled',
    [BookingStatus.REJECTED]: 'status-rejected'
  };
  return map[status] || 'status-default';
}

/**
 * Get date range for calendar view (extended to week boundaries)
 */
export function getCalendarDateRange(viewDate: Date): { from: Date; to: Date } {
  const monthStart = startOfMonth(viewDate);
  const monthEnd = endOfMonth(viewDate);
  
  const from = startOfWeek(monthStart, { weekStartsOn: 1 });
  const to = endOfWeek(monthEnd, { weekStartsOn: 1 });
  
  return { from, to };
}

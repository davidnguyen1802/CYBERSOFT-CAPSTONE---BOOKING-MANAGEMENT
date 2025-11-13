import { Component, Input } from '@angular/core';
import { HostStatistics } from '../../../models/booking';

interface StatCard {
  icon: string;
  label: string;
  value: number | string;
  color: string;
  trend?: string;
  trendUp?: boolean;
}

@Component({
  selector: 'app-statistics-cards',
  templateUrl: './statistics-cards.component.html',
  styleUrls: ['./statistics-cards.component.scss']
})
export class StatisticsCardsComponent {
  @Input() statistics: HostStatistics | null = null;
  @Input() loading: boolean = false;

  get statCards(): StatCard[] {
    if (!this.statistics) {
      return [];
    }

    return [
      {
        icon: '🏠',
        label: 'Total Properties',
        value: this.statistics.totalProperties,
        color: '#2196F3',
        trend: '+2 this month',
        trendUp: true
      },
      {
        icon: '⏳',
        label: 'Pending Bookings',
        value: this.statistics.pendingBookings,
        color: '#FFC107',
        trend: 'Needs review',
        trendUp: false
      },
      {
        icon: '✅',
        label: 'Confirmed',
        value: this.statistics.confirmedBookings,
        color: '#4CAF50',
        trend: '+5 this week',
        trendUp: true
      },
      {
        icon: '🎉',
        label: 'Completed',
        value: this.statistics.completedBookings,
        color: '#9E9E9E',
        trend: this.getCompletionRate(),
        trendUp: true
      },
      {
        icon: '💰',
        label: 'Total Revenue',
        value: this.formatCurrency(this.statistics.totalRevenue),
        color: '#4CAF50',
        trend: '+12% from last month',
        trendUp: true
      },
      {
        icon: '⭐',
        label: 'Average Rating',
        value: this.statistics.averageRating.toFixed(1),
        color: '#FF9800',
        trend: this.getRatingText(),
        trendUp: this.statistics.averageRating >= 4.5
      }
    ];
  }

  formatCurrency(amount: number): string {
    if (amount >= 1000000) {
      return `${(amount / 1000000).toFixed(1)}M VND`;
    } else if (amount >= 1000) {
      return `${(amount / 1000).toFixed(0)}K VND`;
    }
    return `${amount} VND`;
  }

  getCompletionRate(): string {
    if (!this.statistics) return '';
    const total = this.statistics.pendingBookings + 
                  this.statistics.confirmedBookings + 
                  this.statistics.completedBookings;
    if (total === 0) return '0% completion';
    const rate = (this.statistics.completedBookings / total) * 100;
    return `${rate.toFixed(0)}% completion`;
  }

  getRatingText(): string {
    if (!this.statistics) return '';
    const rating = this.statistics.averageRating;
    if (rating >= 4.5) return 'Excellent!';
    if (rating >= 4.0) return 'Very Good';
    if (rating >= 3.5) return 'Good';
    return 'Needs Improvement';
  }

  getCurrentTime(): string {
    const now = new Date();
    const hours = now.getHours().toString().padStart(2, '0');
    const minutes = now.getMinutes().toString().padStart(2, '0');
    const seconds = now.getSeconds().toString().padStart(2, '0');
    const date = now.toLocaleDateString('en-US', { 
      month: 'short', 
      day: 'numeric', 
      year: 'numeric' 
    });
    return `${date} at ${hours}:${minutes}:${seconds}`;
  }
}





import { Injectable } from '@angular/core';
import { NgbDatepickerI18n, NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { TranslationWidth } from '@angular/common';

const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
];

@Injectable()
export class CustomDatepickerI18n extends NgbDatepickerI18n {
  
  override getMonthShortName(month: number): string {
    return MONTHS[month - 1] || '';
  }

  override getMonthFullName(month: number): string {
    return MONTHS[month - 1] || '';
  }

  override getDayAriaLabel(date: NgbDateStruct): string {
    return `${date.day}-${date.month}-${date.year}`;
  }

  override getDayNumerals(date: NgbDateStruct): string {
    return `${date.day}`;
  }

  getWeekdayShortName(weekday: number): string {
    const WEEKDAYS = ['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su'];
    return WEEKDAYS[weekday - 1] || '';
  }

  override getWeekdayLabel(weekday: number, width?: TranslationWidth): string {
    return this.getWeekdayShortName(weekday);
  }
}


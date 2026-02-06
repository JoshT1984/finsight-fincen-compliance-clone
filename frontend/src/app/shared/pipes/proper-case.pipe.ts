import { Pipe, PipeTransform } from '@angular/core';

/**
 * Converts type/enum strings to Proper Capitalization.
 * E.g. "FRAUD_RING" -> "Fraud Ring", "MONEY_LAUNDERING" -> "Money Laundering"
 */
@Pipe({
  name: 'properCase',
  standalone: true,
})
export class ProperCasePipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (value == null || value === '') return '';
    return value
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
  }
}

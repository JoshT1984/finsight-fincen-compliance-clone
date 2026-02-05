import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-support-ticket',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './support-ticket.component.html',
  styleUrl: './support-ticket.component.css',
})
export class SupportTicketComponent {
  subject = '';
  category = 'general';
  description = '';
  submitted = false;
  error = '';

  categories = [
    { value: 'general', label: 'General inquiry' },
    { value: 'access', label: 'Access or account request' },
    { value: 'technical', label: 'Technical issue' },
    { value: 'other', label: 'Other' },
  ];

  submit(): void {
    this.error = '';
    const sub = (this.subject || '').trim();
    const desc = (this.description || '').trim();
    if (!sub) {
      this.error = 'Please enter a subject.';
      return;
    }
    if (!desc) {
      this.error = 'Please enter a description.';
      return;
    }
    this.submitted = true;
  }
}

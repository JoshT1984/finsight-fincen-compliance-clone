import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Component } from '@angular/core';
import { LoginDialogService } from '../services/loginDialog.service';

@Component({
  selector: 'app-login-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login-dialog.component.html',
  styleUrls: ['./login-dialog.component.css'],
})
export class LoginDialogComponent {
  email = '';
  password = '';

  constructor(private loginDialog: LoginDialogService) {}

  close() {
    this.loginDialog.close();
  }

  login() {
    this.loginDialog.login(this.email, this.password).subscribe({
      next: (token: string) => {},
      error: (error: any) => {
        console.error('Login failed', error);
      },
    });
  }
}

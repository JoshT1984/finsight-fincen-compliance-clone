import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { FooterComponent } from '../footer/footer.component';
import { HeaderComponent } from '../header/header.component';
import { NavComponent } from '../nav/nav.component';
import { IdentityService } from '../../shared/services/identity.service';
import { LoginDialogService } from '../../shared/services/loginDialog.service';
import { LoginDialogComponent } from '../../shared/login-dialog/login-dialog.component';
import { FormsModule } from '@angular/forms';
import { CommonModule, } from '@angular/common';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    HeaderComponent,
    NavComponent,
    FooterComponent,
    LoginDialogComponent,
    FormsModule,
    CommonModule,
  ],
  templateUrl: './shell.component.html',
})
export class ShellComponent {
  private identity = inject(IdentityService);
  protected isLoggedIn = toSignal(this.identity.isLoggedIn$, { initialValue: false });
  public loginDialog = inject(LoginDialogService);
}

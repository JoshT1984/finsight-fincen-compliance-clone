import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { FooterComponent } from '../footer/footer.component';
import { HeaderComponent } from '../header/header.component';
import { NavComponent } from '../nav/nav.component';
import { IdentityService } from '../../shared/services/identity.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, NavComponent, FooterComponent],
  templateUrl: './shell.component.html',
})
export class ShellComponent {
  private identity = inject(IdentityService);
  protected isLoggedIn = toSignal(this.identity.isLoggedIn$, { initialValue: false });
}

import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { IdentityService } from './shared/services/identity.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('finsight');

  constructor(private identityService: IdentityService) {
    this.identityService.checkAuthOnStartup();
    this.identityService.startInactivityTracking();
  }
}

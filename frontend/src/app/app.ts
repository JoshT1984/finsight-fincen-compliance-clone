import { Component, signal, Inject, Renderer2, OnInit } from '@angular/core';
import { Router, NavigationEnd, RouterOutlet } from '@angular/router';
import { DOCUMENT } from '@angular/common';
import { IdentityService } from './shared/services/identity.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  protected readonly title = signal('finsight');

  constructor(
    private identityService: IdentityService,
    private router: Router,
    private renderer: Renderer2,
    @Inject(DOCUMENT) private document: Document,
  ) {
    this.identityService.checkAuthOnStartup();
    this.identityService.startInactivityTracking();
  }

  ngOnInit(): void {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        // Defer DOM update to avoid triggering change detection in the same tick
        setTimeout(() => {
          if (event.urlAfterRedirects === '/' || event.url === '/') {
            this.renderer.addClass(this.document.body, 'landing-page-bg');
          } else {
            this.renderer.removeClass(this.document.body, 'landing-page-bg');
          }
        }, 0);
      }
    });
  }
}

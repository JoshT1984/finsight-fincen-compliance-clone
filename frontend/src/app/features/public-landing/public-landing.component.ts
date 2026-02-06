import { Component, OnInit, OnDestroy, NgZone } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SideNavComponent, NavItem } from '../../shared/side-nav/side-nav.component';

const SECTION_IDS = ['about', 'accessing', 'forms', 'support'] as const;
type SectionId = (typeof SECTION_IDS)[number];

function isValidSection(fragment: string | null): fragment is SectionId {
  return fragment != null && SECTION_IDS.includes(fragment as SectionId);
}

@Component({
  selector: 'app-public-landing',
  standalone: true,
  imports: [RouterLink, SideNavComponent],
  templateUrl: './public-landing.component.html',
  styleUrl: './public-landing.component.css',
})
export class PublicLandingComponent implements OnInit, OnDestroy {
  navItems: NavItem[] = [
    { id: 'about', label: 'About Finsight' },
    { id: 'accessing', label: 'Accessing Finsight' },
    { id: 'forms', label: 'CTR & SAR Forms' },
    { id: 'support', label: 'Contact & Support' },
  ];

  selectedSection: SectionId = 'about';

  private hashChangeHandler = () => this.applyFragmentFromUrl();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private ngZone: NgZone,
  ) {}

  ngOnInit(): void {
    this.applyFragmentFromUrl();
    // Apply again after a tick so we pick up the fragment once the router has updated
    setTimeout(() => this.applyFragmentFromUrl(), 0);
    this.route.fragment.subscribe((f) => {
      if (isValidSection(f)) this.selectedSection = f;
    });
    this.ngZone.runOutsideAngular(() => {
      window.addEventListener('hashchange', this.hashChangeHandler);
    });
  }

  ngOnDestroy(): void {
    window.removeEventListener('hashchange', this.hashChangeHandler);
  }

  private applyFragmentFromUrl(): void {
    const fromRoute = this.route.snapshot.fragment;
    const fromHash = typeof window !== 'undefined' && window.location.hash
      ? window.location.hash.slice(1)
      : null;
    const fragment = fromRoute ?? fromHash;
    if (isValidSection(fragment)) {
      this.ngZone.run(() => {
        this.selectedSection = fragment;
      });
    }
  }

  onSectionSelected(id: string): void {
    if (isValidSection(id)) {
      this.selectedSection = id;
      this.router.navigate([], { fragment: id, replaceUrl: true });
    }
  }
}

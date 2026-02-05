import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SideNavComponent, NavItem } from '../../shared/side-nav/side-nav.component';

const SECTION_IDS = ['about', 'accessing', 'forms', 'support'] as const;

@Component({
  selector: 'app-public-landing',
  standalone: true,
  imports: [RouterLink, SideNavComponent],
  templateUrl: './public-landing.component.html',
  styleUrl: './public-landing.component.css',
})
export class PublicLandingComponent implements OnInit {
  navItems: NavItem[] = [
    { id: 'about', label: 'About Finsight' },
    { id: 'accessing', label: 'Accessing Finsight' },
    { id: 'forms', label: 'CTR & SAR Forms' },
    { id: 'support', label: 'Contact & Support' },
  ];

  selectedSection = 'about';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    const applyFragment = (fragment: string | null) => {
      if (fragment && SECTION_IDS.includes(fragment as (typeof SECTION_IDS)[number])) {
        this.selectedSection = fragment;
      }
    };
    applyFragment(this.route.snapshot.fragment);
    this.route.fragment.subscribe(applyFragment);
  }

  onSectionSelected(id: string): void {
    this.selectedSection = id;
    this.router.navigate([], { fragment: id, replaceUrl: true });
  }
}

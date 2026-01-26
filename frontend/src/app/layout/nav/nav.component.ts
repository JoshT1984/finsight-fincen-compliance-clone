import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgFor } from '@angular/common';

@Component({
  selector: 'app-nav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, NgFor],
  templateUrl: './nav.component.html',
  styleUrls: ['./nav.component.css'],
})
export class NavComponent {
  navItems = [
    { path: '', label: 'Home' },
    { path: '/cases', label: 'Cases' },
    { path: '/sars', label: 'SARs' },
    { path: '/ctrs', label: 'CTRs' },
    { path: '/documents', label: 'Documents' },
  ];
}

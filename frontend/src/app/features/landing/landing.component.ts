import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { RoleService } from '../../shared/services/role.service';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css'],
})
export class LandingComponent {
  constructor(
    public roleService: RoleService,
    private router: Router,
  ) {}

  goToUpload(): void {
    this.router.navigate(['/upload']);
  }
}

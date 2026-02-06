import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ProfileModel } from '../../models/profile.model';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IdentityService } from '../../shared/services/identity.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
  standalone: true,
  imports: [CommonModule, FormsModule],
})
export class ProfileComponent implements OnInit {
  profile: ProfileModel | null = null;

  constructor(
    private identityService: IdentityService,
    private cdr: ChangeDetectorRef,
    private router: Router,
  ) {}

  ngOnInit() {
    this.identityService.profile$.subscribe((profile) => {
      this.profile = profile;
      if (!profile) {
        this.router.navigate(['/home']);
      } else {
        this.updateProviderLinks();
      }
    });
  }

  editMode = false;
  editPasswordMode = false;
  savingProfile = false;
  savingPassword = false;

  startEdit() {
    this.editMode = true;
  }

  saveEdit() {
    this.savingProfile = true;
    this.identityService.saveProfileUpdates(this.profile!).subscribe({
      next: () => {
        this.editMode = false;
        this.savingProfile = false;
      },
      error: () => {
        this.savingProfile = false;
      },
    });
  }

  cancelEdit() {
    this.editMode = false;
  }

  startEditPassword() {
    this.editPasswordMode = true;
  }

  currentPassword = '';
  newPassword = '';
  confirmPassword = '';

  saveEditPassword() {
    if (this.newPassword !== this.confirmPassword) {
      // Optionally show error message
      return;
    }
    this.savingPassword = true;
    this.identityService.changePassword(this.currentPassword, this.newPassword).subscribe({
      next: () => {
        console.log('Password changed successfully');
        this.editPasswordMode = false;
        this.editMode = false;
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        this.savingPassword = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.savingPassword = false;
      },
    });
  }

  cancelEditPassword() {
    this.editPasswordMode = false;
  }

  linkGoogle() {
    this.identityService.linkProvider('google');
  }

  linkGithub() {
    this.identityService.linkProvider('github');
  }

  private updateProviderLinks() {
    this.identityService.hasLinkedProvider('google').subscribe((val) => {
      this.googleLinked = val;
      this.cdr.detectChanges();
    });
    this.identityService.hasLinkedProvider('github').subscribe((val) => {
      this.githubLinked = val;
      this.cdr.detectChanges();
    });
  }

  googleLinked = false;
  githubLinked = false;

  ngAfterViewInit() {
    // If redirected back from OAuth, refresh profile
    const params = new URLSearchParams(window.location.search);
    if (params.has('linked')) {
      // Optionally, show a success message
      this.identityService.setCurrentUserProfile().subscribe(() => {
        // this.updateProviderLinks();
      });
      // Remove the query param from the URL
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }
}

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
  profileError: string | null = null;
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
        this.router.navigate(['/dashboard']);
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
    console.log('Saving profile:', this.profile);
    this.profileError = null;
    this.identityService.saveProfileUpdates(this.profile!).subscribe({
      next: () => {
        this.editMode = false;
        this.savingProfile = false;
        this.profileError = null;
      },
      error: (err) => {
        console.log('Error saving profile:', err);
        this.savingProfile = false;
        if (err && err.error && err.error.message) {
          this.profileError = err.error.message;
        } else {
          this.profileError = 'Failed to update profile.';
        }
        this.cdr.detectChanges();
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

  passwordError: string | null = null;

  saveEditPassword() {
    this.passwordError = null;
    if (this.newPassword !== this.confirmPassword) {
      this.passwordError = 'Passwords do not match.';
      return;
    }
    this.savingPassword = true;
    this.identityService.changePassword(this.currentPassword, this.newPassword).subscribe({
      next: () => {
        this.editPasswordMode = false;
        this.editMode = false;
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        this.savingPassword = false;
        this.passwordError = null;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.savingPassword = false;
        if (err && err.error && err.error.message) {
          this.passwordError = err.error.message;
        } else {
          this.passwordError = 'Failed to change password.';
        }
        this.cdr.detectChanges();
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

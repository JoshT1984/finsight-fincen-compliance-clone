import { Component, OnInit } from '@angular/core';
import { ProfileModel } from '../../models/profile.model';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IdentityService } from '../../shared/services/identity.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
  standalone: true,
  imports: [CommonModule, FormsModule],
})
export class ProfileComponent implements OnInit {
  profile: ProfileModel | null = null;

  constructor(private identityService: IdentityService) {}

  ngOnInit() {
    this.identityService.profile$.subscribe((profile) => {
      this.profile = profile;
      this.updateProviderLinks();
    });
  }

  editMode = false;
  editPasswordMode = false;

  startEdit() {
    this.editMode = true;
  }

  saveEdit() {
    this.identityService.saveProfileUpdates(this.profile!).subscribe(() => {
      this.editMode = false;
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
    this.identityService.changePassword(this.currentPassword, this.newPassword).subscribe(() => {
      this.editPasswordMode = false;
      this.currentPassword = '';
      this.newPassword = '';
      this.confirmPassword = '';
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
    this.identityService.hasLinkedProvider('google').subscribe((val) => (this.googleLinked = val));
    this.identityService.hasLinkedProvider('github').subscribe((val) => (this.githubLinked = val));
  }

  googleLinked = false;
  githubLinked = false;

  ngAfterViewInit() {
    // If redirected back from OAuth, refresh profile
    const params = new URLSearchParams(window.location.search);
    if (params.has('linked')) {
      // Optionally, show a success message
      this.identityService.setCurrentUserProfile().subscribe(() => {
        this.updateProviderLinks();
      });
      // Remove the query param from the URL
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }
}

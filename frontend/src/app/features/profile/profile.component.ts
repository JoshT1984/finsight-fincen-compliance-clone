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
    /* TODO: Link Google */
  }
  linkGithub() {
    /* TODO: Link GitHub */
  }
}

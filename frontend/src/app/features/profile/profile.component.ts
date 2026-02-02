import { Component } from '@angular/core';
import { ProfileModel } from './profile.model';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
  standalone: true,
  imports: [CommonModule, FormsModule],
})
export class ProfileComponent {
  profile: ProfileModel = {
    first_name: 'John',
    last_name: 'Doe',
    email: 'john.doe@example.com',
    phone: '555-1234',
    role_name: 'Analyst',
  };

  editMode = false;
  editPasswordMode = false;

  startEdit() {
    this.editMode = true;
  }
  saveEdit() {
    this.editMode = false; /* TODO: Save changes */
  }
  cancelEdit() {
    this.editMode = false;
  }

  startEditPassword() {
    this.editPasswordMode = true;
  }
  saveEditPassword() {
    this.editPasswordMode = false; /* TODO: Save password */
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

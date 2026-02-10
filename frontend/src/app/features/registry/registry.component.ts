import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-registry',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './registry.component.html',
  styleUrls: ['./registry.component.css'],
})
export class RegistryComponent {}

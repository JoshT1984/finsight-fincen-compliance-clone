import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';

export interface NavItem {
  id: string;
  label: string;
}

@Component({
  selector: 'app-side-nav',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './side-nav.component.html',
  styleUrls: ['./side-nav.component.css'],
})
export class SideNavComponent {
  @Input() navItems: NavItem[] = [];
  @Input() initialSelectedItem?: string;
  /** When set, nav items are rendered as router links to linkBasePath + '/' + item.id (e.g. /cases/123/overview). */
  @Input() linkBasePath?: string;
  /** When set, this id is used for the active class (e.g. for URL fragment–driven section selection). */
  @Input() currentItemId?: string;

  @Output() itemSelected = new EventEmitter<string>();

  selectedItem: string = '';

  ngOnInit() {
    if (this.linkBasePath) return;
    if (this.initialSelectedItem) {
      this.selectedItem = this.initialSelectedItem;
      this.itemSelected.emit(this.initialSelectedItem);
    } else if (this.navItems.length > 0) {
      this.selectedItem = this.navItems[0].id;
      this.itemSelected.emit(this.navItems[0].id);
    }
  }

  selectItem(id: string) {
    this.selectedItem = id;
    this.itemSelected.emit(id);
  }
}

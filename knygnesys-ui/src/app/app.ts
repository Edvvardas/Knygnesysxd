import { Component } from '@angular/core';
import { RouterOutlet, Router } from '@angular/router';
import { NavbarComponent } from './components/navbar/navbar.component';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NavbarComponent, CommonModule],
  templateUrl: './app.html',
})
export class App {
  constructor(public router: Router) {}

  get showNavbar(): boolean {
    const url = this.router.url;
    return url !== '/login' && url !== '/register';
  }
}

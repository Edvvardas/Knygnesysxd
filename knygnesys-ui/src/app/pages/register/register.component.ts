import {Component, inject} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {AuthService} from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  email = '';
  username = '';
  password = '';
  error = '';

  register() {
    this.authService.register(this.email, this.username, this.password).subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.error = 'Registracija nepavyko. Patikrinkite įvestus duomenis.'
    });
  }
}

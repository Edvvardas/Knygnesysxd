import {Routes} from '@angular/router';
import {Home} from './pages/home/home';
import {LoginComponent} from './pages/login/login.component';
import {RegisterComponent} from './pages/register/register.component';
import {BibliotekaPuslapis} from './pages/biblioteka/biblioteka.component';
import {StatistikaPuslapis} from './pages/statistika/statistika.component';
import {authGuard} from './guards/auth.guard';

export const routes: Routes = [
  {path: 'login', component: LoginComponent},
  {path: 'register', component: RegisterComponent},
  {path: '', component: Home, canActivate: [authGuard]},
  {path: 'biblioteka', component: BibliotekaPuslapis, canActivate: [authGuard]},
  {path: 'statistika', component: StatistikaPuslapis, canActivate: [authGuard]},
  {path: '**', redirectTo: ''}
];

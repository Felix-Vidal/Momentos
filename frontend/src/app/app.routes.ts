import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/home/home.page').then((m) => m.HomePage),
    title: 'Momentos',
  },
  {
    path: 'entrar',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login.page').then((m) => m.LoginPage),
    title: 'Entrar · Momentos',
  },
  {
    path: 'criar-conta',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register.page').then((m) => m.RegisterPage),
    title: 'Criar conta · Momentos',
  },
  {
    path: 'perfil',
    canActivate: [authGuard],
    loadComponent: () => import('./features/members/profile.page').then((m) => m.ProfilePage),
    title: 'Meu perfil · Momentos',
  },
  {
    path: '**',
    loadComponent: () => import('./features/home/not-found.page').then((m) => m.NotFoundPage),
    title: 'Página não encontrada · Momentos',
  },
];

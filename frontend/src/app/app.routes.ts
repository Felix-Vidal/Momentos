import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/inicio/inicio.page').then((m) => m.InicioPage),
    title: 'Momentos',
  },
  {
    path: '**',
    loadComponent: () =>
      import('./features/inicio/nao-encontrado.page').then((m) => m.NaoEncontradoPage),
    title: 'Página não encontrada · Momentos',
  },
];

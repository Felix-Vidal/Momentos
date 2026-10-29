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
    path: 'grupos',
    canActivate: [authGuard],
    loadComponent: () => import('./features/groups/my-groups.page').then((m) => m.MyGroupsPage),
    title: 'Meus grupos · Momentos',
  },
  {
    path: 'criar-grupo',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/groups/create-group.page').then((m) => m.CreateGroupPage),
    title: 'Criar grupo · Momentos',
  },
  {
    path: 'descobrir',
    loadComponent: () =>
      import('./features/groups/discover-groups.page').then((m) => m.DiscoverGroupsPage),
    title: 'Descobrir grupos · Momentos',
  },
  {
    // Moldura do grupo: as abas são rotas filhas e compartilham o grupo carregado.
    path: 'g/:slug',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/groups/group-shell.page').then((m) => m.GroupShellPage),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/timeline/timeline.page').then((m) => m.TimelinePage),
      },
      {
        path: 'integrantes',
        loadComponent: () => import('./features/members/members-tab').then((m) => m.MembersTab),
      },
      {
        path: 'integrantes/:userId',
        loadComponent: () =>
          import('./features/members/member-profile.page').then((m) => m.MemberProfilePage),
      },
      {
        path: 'configuracoes',
        loadComponent: () =>
          import('./features/groups/group-settings.page').then((m) => m.GroupSettingsPage),
      },
    ],
  },
  {
    path: '**',
    loadComponent: () => import('./features/home/not-found.page').then((m) => m.NotFoundPage),
    title: 'Página não encontrada · Momentos',
  },
];

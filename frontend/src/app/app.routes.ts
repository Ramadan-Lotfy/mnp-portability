import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'requests' },
  {
    path: 'requests',
    title: 'Porting requests · MNP',
    loadComponent: () =>
      import('./features/porting-requests/request-list/request-list').then((m) => m.RequestList),
  },
  {
    path: 'requests/new',
    title: 'New porting request · MNP',
    loadComponent: () =>
      import('./features/porting-requests/request-form/request-form').then((m) => m.RequestForm),
  },
  {
    path: 'numbers',
    title: 'Number status · MNP',
    loadComponent: () =>
      import('./features/number-status/number-lookup').then((m) => m.NumberLookup),
  },
  { path: '**', redirectTo: 'requests' },
];

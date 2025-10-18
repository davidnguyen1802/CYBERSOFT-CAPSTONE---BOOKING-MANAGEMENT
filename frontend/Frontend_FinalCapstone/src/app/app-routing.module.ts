import { NgModule, importProvidersFrom } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HomeComponent } from './components/home/home.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { DetailPropertyComponent } from './components/detail-property/detail-property.component';
import { OrderComponent } from './components/order/order.component';
import { OrderDetailComponent } from './components/detail-order/order.detail.component';
import { UserProfileComponent } from './components/user-profile/user.profile.component';
import { AdminComponent } from './components/admin/admin.component';
import { PropertyListComponent } from './components/property-list/property-list.component';
import { PropertyFilterComponent } from './components/property-filter/property-filter.component';
import { AuthGuardFn } from './guards/auth.guard';
import { AdminGuardFn } from './guards/admin.guard';
import { AuthCallbackComponent } from './components/auth-callback/auth-callback.component';
//import { OrderAdminComponent } from './components/admin/order/order.admin.component';

const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginComponent },  
  { path: 'register', component: RegisterComponent },
  { path: 'properties/filter', component: PropertyFilterComponent },
  { path: 'properties/type/:type', component: PropertyListComponent },
  { path: 'properties', component: PropertyListComponent },
  { path: 'properties/:id', component: DetailPropertyComponent },
  { path: 'auth/callback', component: AuthCallbackComponent }, // OAuth callback handler
  { path: 'orders', component: OrderComponent, canActivate:[AuthGuardFn] },
  { path: 'user-profile', component: UserProfileComponent, canActivate:[AuthGuardFn] },
  { path: 'orders/:id', component: OrderDetailComponent },
  //Admin   
  { 
    path: 'admin', 
    component: AdminComponent, 
    canActivate:[AdminGuardFn] 
  },      
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes),
    CommonModule
  ],
  exports: [RouterModule]
})
export class AppRoutingModule { }

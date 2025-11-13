import { NgModule, importProvidersFrom } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HomeComponent } from './components/home/home.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { DetailPropertyComponent } from './components/detail-property/detail-property.component';
import { EditPropertyComponent } from './components/edit-property/edit-property.component';
import { OrderDetailComponent } from './components/detail-order/order.detail.component';
import { UserProfileComponent } from './components/user-profile/user.profile.component';
import { AdminComponent } from './components/admin/admin.component';
import { PropertyListComponent } from './components/property-list/property-list.component';
import { PropertyFilterComponent } from './components/property-filter/property-filter.component';
import { AddPropertyComponent } from './components/add-property/add-property.component';
import { AddPropertyImagesComponent } from './components/add-property-images/add-property-images.component';
import { AddPropertyAmenitiesComponent } from './components/add-property-amenities/add-property-amenities.component';
import { AddPropertyFacilitiesComponent } from './components/add-property-facilities/add-property-facilities.component';
import { AuthGuardFn } from './guards/auth.guard';
import { AdminGuardFn } from './guards/admin.guard';
import { AuthCallbackComponent } from './components/auth-callback/auth-callback.component';
import { MyPromotionsComponent } from './components/my-promotions/my-promotions.component';
import { BookingComponent } from './components/booking/booking.component';
import { PromotionSelectionComponent } from './components/promotion-selection/promotion-selection.component';
import { PaymentSuccessComponent } from './components/payment-success/payment-success.component';
import { PaymentCancelComponent } from './components/payment-cancel/payment-cancel.component';
import { MyBookingsComponent } from './components/my-bookings/my-bookings.component';
import { HostDashboardComponent } from './components/host-dashboard/host-dashboard.component';
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
  { path: 'booking', component: BookingComponent, canActivate:[AuthGuardFn] },
  { path: 'booking/select-promotion', component: PromotionSelectionComponent, canActivate:[AuthGuardFn] },
  { path: 'my-bookings', component: MyBookingsComponent, canActivate:[AuthGuardFn] },
  { path: 'payment/success', component: PaymentSuccessComponent },
  { path: 'payment/cancel', component: PaymentCancelComponent },
  { path: 'user-profile', component: UserProfileComponent, canActivate:[AuthGuardFn] },
  { path: 'my-promotions', component: MyPromotionsComponent, canActivate:[AuthGuardFn] },
  // Old route redirect removed - no longer needed after API endpoint fix
  { path: 'orders/:id', component: OrderDetailComponent },
  // Host routes
  { path: 'host/dashboard', component: HostDashboardComponent, canActivate:[AuthGuardFn] },
  { path: 'host/add-property', component: AddPropertyComponent, canActivate:[AuthGuardFn] },
  { path: 'host/add-property-images', component: AddPropertyImagesComponent, canActivate:[AuthGuardFn] },
  { path: 'host/add-property-amenities', component: AddPropertyAmenitiesComponent, canActivate:[AuthGuardFn] },
  { path: 'host/add-property-facilities', component: AddPropertyFacilitiesComponent, canActivate:[AuthGuardFn] },
  { path: 'host/edit-property/:id', component: EditPropertyComponent, canActivate:[AuthGuardFn] },
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

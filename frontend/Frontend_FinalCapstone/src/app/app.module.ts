import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { RouterModule } from '@angular/router';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

// Components
import { AppComponent } from './app/app.component';
import { HomeComponent } from './components/home/home.component';
import { RecommendedComponent } from './components/home/recommended.component';
import { PopularPropertiesComponent } from './components/home/popular-properties.component';
import { PropertyCardComponent } from './components/shared/property-card/property-card.component';
import { PaginationComponent } from './components/shared/pagination/pagination.component';
import { PropertyListComponent } from './components/property-list/property-list.component';
import { PropertyFilterComponent } from './components/property-filter/property-filter.component';
import { AddPropertyComponent } from './components/add-property/add-property.component';
import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { DetailPropertyComponent } from './components/detail-property/detail-property.component';
import { EditPropertyComponent } from './components/edit-property/edit-property.component';
import { OrderDetailComponent } from './components/detail-order/order.detail.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { UserProfileComponent } from './components/user-profile/user.profile.component';
import { AuthCallbackComponent } from './components/auth-callback/auth-callback.component';
import { SimpleModalComponent } from './components/shared/simple-modal/simple-modal.component';
import { MyPromotionsComponent } from './components/my-promotions/my-promotions.component';
import { BookingComponent } from './components/booking/booking.component';
import { PromotionSelectionComponent } from './components/promotion-selection/promotion-selection.component';
import { PaymentSuccessComponent } from './components/payment-success/payment-success.component';
import { PaymentCancelComponent } from './components/payment-cancel/payment-cancel.component';
import { MyBookingsComponent } from './components/my-bookings/my-bookings.component';
import { BookingCardComponent } from './components/booking-card/booking-card.component';
import { HostDashboardComponent } from './components/host-dashboard/host-dashboard.component';
import { DateRangePickerComponent } from './components/shared/date-range-picker/date-range-picker.component';
import { ConfirmModalComponent } from './components/shared/confirm-modal/confirm-modal.component';
import { InputModalComponent } from './components/shared/input-modal/input-modal.component';
import { CalendarComponent } from './components/shared/calendar/calendar.component';
import { StatisticsCardsComponent } from './components/shared/statistics-cards/statistics-cards.component';

// Modules
import { AppRoutingModule } from './app-routing.module';
import { AdminModule } from './components/admin/admin.module';

// Interceptors
import { TokenInterceptor } from './interceptors/token.interceptor';
import { DeviceIdInterceptor } from './interceptors/device-id.interceptor';
import { AddPropertyImagesComponent } from './components/add-property-images/add-property-images.component';
import { AddPropertyAmenitiesComponent } from './components/add-property-amenities/add-property-amenities.component';
import { AddPropertyFacilitiesComponent } from './components/add-property-facilities/add-property-facilities.component';

// Services
import { CustomDatepickerI18n } from './services/datepicker-i18n.service';
import { NgbDatepickerI18n } from '@ng-bootstrap/ng-bootstrap';

@NgModule({
  declarations: [    
    AppComponent,
    HomeComponent, 
    RecommendedComponent,
    PopularPropertiesComponent,
    PropertyCardComponent,
    PaginationComponent,
    PropertyListComponent,
    PropertyFilterComponent,
    AddPropertyComponent,
    HeaderComponent,
    FooterComponent, 
    DetailPropertyComponent,
    EditPropertyComponent,
    OrderDetailComponent, 
    LoginComponent, 
    RegisterComponent, 
    UserProfileComponent,
    AuthCallbackComponent,
    SimpleModalComponent,
    MyPromotionsComponent,
    AddPropertyImagesComponent,
    AddPropertyAmenitiesComponent,
    AddPropertyFacilitiesComponent,
    BookingComponent,
    PromotionSelectionComponent,
    PaymentSuccessComponent,
    PaymentCancelComponent,
    MyBookingsComponent,
    BookingCardComponent,
    HostDashboardComponent,
    DateRangePickerComponent,
    ConfirmModalComponent,
    InputModalComponent,
    CalendarComponent,
    StatisticsCardsComponent,
    //admin    
    //AdminComponent,
    //OrderAdminComponent,
    //ProductAdminComponent,
    //CategoryAdminComponent,
    //DetailOrderAdminComponent,
  ],
  imports: [
    BrowserModule,
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    RouterModule,
    AppRoutingModule,
    NgbModule,
    AdminModule,
  ],
  providers: [
    // Device ID Interceptor - MUST be first (runs before TokenInterceptor)
    {
      provide: HTTP_INTERCEPTORS,
      useClass: DeviceIdInterceptor,
      multi: true,
    },
    // Token Interceptor
    {
      provide: HTTP_INTERCEPTORS,
      useClass: TokenInterceptor,
      multi: true,
    },
    // Custom Datepicker I18n for full month names
    {
      provide: NgbDatepickerI18n,
      useClass: CustomDatepickerI18n
    },
  ],
  bootstrap: [
    AppComponent
    // HomeComponent,
    //DetailProductComponent,
    // OrderComponent,
    //OrderDetailComponent,
    //LoginComponent,
    // RegisterComponent
  ]
})
export class AppModule { }

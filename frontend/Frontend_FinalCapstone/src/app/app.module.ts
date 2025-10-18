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
import { PropertyListComponent } from './components/property-list/property-list.component';
import { PropertyFilterComponent } from './components/property-filter/property-filter.component';
import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { DetailPropertyComponent } from './components/detail-property/detail-property.component';
import { OrderComponent } from './components/order/order.component';
import { OrderDetailComponent } from './components/detail-order/order.detail.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { UserProfileComponent } from './components/user-profile/user.profile.component';
import { AuthCallbackComponent } from './components/auth-callback/auth-callback.component';

// Modules
import { AppRoutingModule } from './app-routing.module';
import { AdminModule } from './components/admin/admin.module';

// Interceptors
import { TokenInterceptor } from './interceptors/token.interceptor';

@NgModule({
  declarations: [    
    AppComponent,
    HomeComponent, 
    RecommendedComponent,
    PopularPropertiesComponent,
    PropertyCardComponent,
    PropertyListComponent,
    PropertyFilterComponent,
    HeaderComponent,
    FooterComponent, 
    DetailPropertyComponent,
    OrderComponent, 
    OrderDetailComponent, 
    LoginComponent, 
    RegisterComponent, 
    UserProfileComponent,
    AuthCallbackComponent,
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
    {
      provide: HTTP_INTERCEPTORS,
      useClass: TokenInterceptor,
      multi: true,
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

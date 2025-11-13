import { Component, OnInit } from '@angular/core';
import { UserService } from '../../services/user.service';
import { Router } from '@angular/router';
import { UserResponse } from '../../responses/user/user.response';
import { TokenService } from '../../services/token.service';
import { AuthStateService } from '../../services/auth-state.service';
@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrls: [
    './admin.component.scss',        
  ]
})
export class AdminComponent implements OnInit {
  //adminComponent: string = 'orders';
  userResponse?:UserResponse | null;
  constructor(
    private userService: UserService,       
    private tokenService: TokenService,    
    private router: Router,
    private authStateService: AuthStateService,
  ) {
    
   }
  ngOnInit() {
    this.userResponse = this.userService.getUserResponseFromLocalStorage();    
    // Default router
    // debugger
    if (this.router.url === '/admin') {
      this.router.navigate(['/admin/orders']);
    }
   }  
  logout() {
    console.log('🚪 Logging out from admin panel');
    
    // IMPORTANT: Notify logout FIRST to trigger UI updates in header
    this.authStateService.notifyLogout(); // ← Triggers header to show "Đăng nhập" button
    
    // Then clear all tokens and user data
    this.tokenService.removeToken();
    this.userService.removeUserFromLocalStorage();
    this.userResponse = null;
    
    console.log('✅ Admin logout complete, login button should now be visible');
    console.log('➡️ Redirecting to home page');
    this.router.navigate(['/']);
  }
  showAdminComponent(componentName: string): void {
    if (componentName === 'orders') {
      this.router.navigate(['/admin/orders']);
    } else if (componentName === 'categories') {
      this.router.navigate(['/admin/categories']);
    } else if (componentName === 'products') {
      this.router.navigate(['/admin/products']);
    }
  }
}


/**
 npm install --save font-awesome
 angular.json:
 "styles": [   
    "node_modules/font-awesome/css/font-awesome.min.css"
],
 */
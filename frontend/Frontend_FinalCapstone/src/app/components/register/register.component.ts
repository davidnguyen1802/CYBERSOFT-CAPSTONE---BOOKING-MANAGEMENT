import { Component, ViewChild } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../../services/user.service';
import { TokenService } from '../../services/token.service';
import { AuthStateService } from '../../services/auth-state.service';
import { CartService } from '../../services/cart.service';
import { RegisterDTO } from '../../dtos/user/register.dto';
@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  @ViewChild('registerForm') registerForm!: NgForm;
  
  // Step control
  currentStep: number = 1;
  
  // Step 1 fields
  email: string;
  password: string;
  retypePassword: string;
  
  // Step 2 fields
  fullName: string;
  username: string;
  phoneNumber: string;
  address: string;
  gender: 'MALE' | 'FEMALE' | 'NONE';
  dateOfBirth: Date | string;
  avatarFile: File | null = null;
  avatarPreview: string | null = null;
  
  // UI control
  isAccepted: boolean;
  showPassword: boolean = false;

  constructor(
    private router: Router, 
    private userService: UserService,
    private tokenService: TokenService,
    private authStateService: AuthStateService,
    private cartService: CartService
  ) {
    // Step 1 initialization
    this.email = '';
    this.password = '';
    this.retypePassword = '';
    
    // Step 2 initialization
    this.fullName = '';
    this.username = '';
    this.phoneNumber = '';
    this.address = '';
    this.gender = 'NONE';
    this.isAccepted = true;
    this.dateOfBirth = new Date();
    this.dateOfBirth.setFullYear(this.dateOfBirth.getFullYear() - 18);
  }
  
  // Navigation methods
  nextStep() {
    if (this.currentStep === 1 && this.validateStep1()) {
      this.currentStep = 2;
    }
  }
  
  previousStep() {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }
  
  // Validation methods
  validateStep1(): boolean {
    // Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.email)) {
      alert('Vui lòng nhập email hợp lệ');
      return false;
    }
    
    // Validate password length (min 6 to match backend)
    if (this.password.length < 6) {
      alert('Mật khẩu phải có ít nhất 6 ký tự');
      return false;
    }
    
    // Validate passwords match
    if (this.password !== this.retypePassword) {
      alert('Mật khẩu không khớp');
      return false;
    }
    
    return true;
  }
  
  validateStep2(): boolean {
    // Validate fullName (2-100 chars)
    if (!this.fullName || this.fullName.trim().length < 2) {
      alert('Họ và tên phải có ít nhất 2 ký tự');
      return false;
    }
    if (this.fullName.trim().length > 100) {
      alert('Họ và tên không được vượt quá 100 ký tự');
      return false;
    }
    
    // Validate username (3-50 chars, pattern: letters, numbers, dots, underscores, hyphens)
    if (!this.username || this.username.trim().length < 3) {
      alert('Username phải có ít nhất 3 ký tự');
      return false;
    }
    if (this.username.trim().length > 50) {
      alert('Username không được vượt quá 50 ký tự');
      return false;
    }
    const usernamePattern = /^[a-zA-Z0-9._-]+$/;
    if (!usernamePattern.test(this.username)) {
      alert('Username chỉ được chứa chữ cái, số, dấu chấm (.), gạch dưới (_) và gạch ngang (-)');
      return false;
    }
    
    // Validate phone - exactly 10 digits
    const phoneRegex = /^\d{10}$/;
    if (!this.phoneNumber || !phoneRegex.test(this.phoneNumber)) {
      alert('Số điện thoại phải có đúng 10 chữ số');
      return false;
    }
    
    // Validate age
    const today = new Date();
    const birthDate = new Date(this.dateOfBirth);
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }
    if (age < 18) {
      alert('Bạn chưa đủ 18 tuổi');
      return false;
    }
    
    // Validate acceptance
    if (!this.isAccepted) {
      alert('Vui lòng đồng ý với điều khoản và điều kiện');
      return false;
    }
    
    return true;
  }
  
  // File upload handler
  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.avatarFile = file;
      
      // Create preview
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.avatarPreview = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }
  
  onPhoneNumberChange(){
    console.log(`Phone typed: ${this.phoneNumber}`);
    // Only allow digits and limit to 10 characters
    this.phoneNumber = this.phoneNumber.replace(/\D/g, '').slice(0, 10);
  }
  
  // Validate username pattern in real-time
  isUsernameInvalid(): boolean {
    if (this.username.length < 3) return false;
    const pattern = /^[a-zA-Z0-9._-]+$/;
    return !pattern.test(this.username);
  }
  register() {
    console.log('📝 Register attempt started');
    
    // Validate step 2
    if (!this.validateStep2()) {
      return;
    }
    
    // Format date as YYYY-MM-DD
    // Handle both Date object and string input
    let dobString: string;
    if (this.dateOfBirth instanceof Date) {
      dobString = this.dateOfBirth.toISOString().split('T')[0];
    } else {
      // If it's already a string (from date input), use it directly
      dobString = this.dateOfBirth.toString();
    }
    
    // Create form data for multipart/form-data
    const formData = new FormData();
    formData.append('fullName', this.fullName);
    formData.append('username', this.username);
    formData.append('email', this.email);
    formData.append('password', this.password);
    formData.append('phone', this.phoneNumber);
    formData.append('address', this.address);
    formData.append('gender', this.gender);
    formData.append('dob', dobString);
    formData.append('facebookAccountId', '0');
    formData.append('googleAccountId', '0');
    
    // Add avatar if selected
    if (this.avatarFile) {
      formData.append('avatar', this.avatarFile, this.avatarFile.name);
    }
    
    console.log('📝 Sending registration data with FormData');
    
    this.userService.register(formData).subscribe({
      next: (response: any) => {
        console.log('✅ Registration successful:', response);
        
        // Check if response and response.data exist (same as login)
        if (!response || !response.data) {
          console.error('❌ Invalid response structure:', response);
          alert('Invalid response from server');
          return;
        }
        
        const authData = response.data;
        const token = authData.token;
        console.log('🔑 Token received:', token);
        console.log('👤 User data:', authData);
        
        // Save access token to localStorage
        // Refresh token is automatically stored in HttpOnly cookie by backend
        console.log('=== SAVING TOKEN ===');
        console.log('Calling tokenService.setToken()...');
        this.tokenService.setToken(token);
        
        // Verify token was saved
        const savedToken = localStorage.getItem('access_token');
        console.log('=== TOKEN VERIFICATION ===');
        console.log('Token saved to localStorage?', savedToken ? 'YES' : 'NO');
        console.log('Saved token:', savedToken);
        
        // Notify that user has logged in
        console.log('� Notifying login state change');
        this.authStateService.notifyLogin();
        
        // Refresh cart (cart service will use token to identify user)
        console.log('🛒 Refreshing cart for registered user');
        this.cartService.refreshCart();
        
        // Navigate based on role
        const roles = authData.roles || [];
        const isAdmin = roles.includes('ROLE_ADMIN');
        
        console.log('👤 User roles:', roles);
        console.log('👤 Is Admin:', isAdmin);
        
        // Show welcome message
        alert(`Đăng ký thành công! Chào mừng ${authData.username} đến với hệ thống.`);
        
        if (isAdmin) {
          console.log('➡️ Navigating to /admin');
          this.router.navigate(['/admin']);
        } else {
          console.log('➡️ Navigating to home page');
          this.router.navigate(['/']);
        }
      },
      complete: () => {
        console.log('✅ Registration process complete');
      },
      error: (error: any) => {
        console.error('❌ Registration error:', error);
        console.error('❌ Error details:', {
          status: error.status,
          statusText: error.statusText,
          message: error?.error?.message,
          fullError: error?.error
        });
        const errorMessage = error?.error?.message || 'Registration failed. Please try again.';
        alert(errorMessage);
      }
    })   
  }
  togglePassword() {
    this.showPassword = !this.showPassword;
  }
  //how to check password match ?
  checkPasswordsMatch() {    
    if (this.password !== this.retypePassword) {
      this.registerForm.form.controls['retypePassword']
            .setErrors({ 'passwordMismatch': true });
    } else {
      this.registerForm.form.controls['retypePassword'].setErrors(null);
    }
  }
  checkAge() {
    if (this.dateOfBirth) {
      const today = new Date();
      const birthDate = new Date(this.dateOfBirth);
      let age = today.getFullYear() - birthDate.getFullYear();
      const monthDiff = today.getMonth() - birthDate.getMonth();
      if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
        age--;
      }

      if (age < 18) {
        this.registerForm.form.controls['dateOfBirth'].setErrors({ 'invalidAge': true });
      } else {
        this.registerForm.form.controls['dateOfBirth'].setErrors(null);
      }
    }
  }
}



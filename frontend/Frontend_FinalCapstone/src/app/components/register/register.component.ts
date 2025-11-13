import { Component, ViewChild } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../../services/user.service';
import { TokenService } from '../../services/token.service';
import { AuthStateService } from '../../services/auth-state.service';
import { CartService } from '../../services/cart.service';
import { SimpleModalService } from '../../services/simple-modal.service';
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
    private cartService: CartService,
    private modalService: SimpleModalService
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
      this.modalService.showError('Vui lòng nhập email hợp lệ');
      return false;
    }
    
    // Validate password length (min 6 to match backend)
    if (this.password.length < 6) {
      this.modalService.showError('Mật khẩu phải có ít nhất 6 ký tự');
      return false;
    }
    
    // Validate passwords match
    if (this.password !== this.retypePassword) {
      this.modalService.showError('Mật khẩu không khớp');
      return false;
    }
    
    return true;
  }
  
  validateStep2(): boolean {
    // Validate fullName (2-100 chars)
    if (!this.fullName || this.fullName.trim().length < 2) {
      this.modalService.showError('Họ và tên phải có ít nhất 2 ký tự');
      return false;
    }
    if (this.fullName.trim().length > 100) {
      this.modalService.showError('Họ và tên không được vượt quá 100 ký tự');
      return false;
    }
    
    // Validate username (3-50 chars, pattern: letters, numbers, dots, underscores, hyphens)
    if (!this.username || this.username.trim().length < 3) {
      this.modalService.showError('Username phải có ít nhất 3 ký tự');
      return false;
    }
    if (this.username.trim().length > 50) {
      this.modalService.showError('Username không được vượt quá 50 ký tự');
      return false;
    }
    const usernamePattern = /^[a-zA-Z0-9._-]+$/;
    if (!usernamePattern.test(this.username)) {
      this.modalService.showError('Username chỉ được chứa chữ cái, số, dấu chấm (.), gạch dưới (_) và gạch ngang (-)');
      return false;
    }
    
    // Validate phone - exactly 10 digits
    const phoneRegex = /^\d{10}$/;
    if (!this.phoneNumber || !phoneRegex.test(this.phoneNumber)) {
      this.modalService.showError('Số điện thoại phải có đúng 10 chữ số');
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
      this.modalService.showError('Bạn chưa đủ 18 tuổi');
      return false;
    }
    
    // Validate acceptance
    if (!this.isAccepted) {
      this.modalService.showError('Vui lòng đồng ý với điều khoản và điều kiện');
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
        console.log('========================================');
        console.log('FULL REGISTRATION RESPONSE:');
        console.log(JSON.stringify(response, null, 2));
        console.log('========================================');
        
        // Check if response and response.data exist
        if (!response || !response.data) {
          console.error('❌ Invalid response structure:', response);
          console.error('response is null?', response === null);
          console.error('response is undefined?', response === undefined);
          console.error('response.data is null?', response?.data === null);
          console.error('response.data is undefined?', response?.data === undefined);
          this.modalService.showError('Phản hồi không hợp lệ từ server');
          return;
        }
        
        // ⚠️ CRITICAL: Backend returns JWT string directly in response.data
        // Format: { status: "OK", message: "...", data: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." }
        // NOT: { status: "OK", message: "...", data: { token: "..." } }
        const token = response.data;
        
        console.log('========================================');
        console.log('TOKEN EXTRACTION:');
        console.log('Token value:', token);
        console.log('Token type:', typeof token);
        console.log('Token is null?', token === null);
        console.log('Token is undefined?', token === undefined);
        console.log('Token is empty string?', token === '');
        console.log('Token length:', token ? token.length : 0);
        console.log('========================================');
        
        if (!token || token === null || token === undefined || token === '') {
          console.error('❌ Token is invalid!');
          this.modalService.showError('Đăng ký thành công nhưng không nhận được token. Vui lòng liên hệ admin.');
          return;
        }
        
        // Save access token to localStorage FIRST
        console.log('========================================');
        console.log('SAVING TOKEN TO LOCALSTORAGE:');
        console.log('Calling tokenService.setToken()...');
        this.tokenService.setToken(token);
        
        // Verify token was saved
        const savedToken = localStorage.getItem('access_token');
        console.log('Token saved to localStorage?', savedToken ? 'YES ✅' : 'NO ❌');
        console.log('Saved token value:', savedToken);
        console.log('Saved token matches original?', savedToken === token);
        console.log('========================================');
        
        // NOTE: Device ID is auto-generated by DeviceService.getDeviceId() when needed
        // DeviceIdInterceptor will trigger auto-generation on first auth request
        // No need to manually generate here
        
        // CRITICAL: Notify auth ready BEFORE triggering any operations
        // This allows header/other components to decode JWT and populate cache
        console.log('🔐 Notifying auth ready state...');
        this.authStateService.notifyLogin(); // This sets authReady = true
        
        // Small delay to ensure all subscribers receive auth ready signal
        setTimeout(() => {
          console.log('🛒 Refreshing cart for registered user');
          this.cartService.refreshCart();
          
          // Parse JWT token to get user info
          const authData = this.tokenService.getUserInfo();
          const role = authData?.role || '';
          const username = authData?.username || 'User';
          const isAdmin = role === 'ROLE_ADMIN';
          
          console.log('👤 User role:', role);
          console.log('👤 Username:', username);
          console.log('👤 Is Admin:', isAdmin);
          
          // Show welcome message
          this.modalService.showSuccess(`Đăng ký thành công! Chào mừng ${username} đến với hệ thống.`);
          
          if (isAdmin) {
            console.log('➡️ Navigating to /admin');
            this.router.navigate(['/admin']);
          } else {
            console.log('➡️ Navigating to home page');
            this.router.navigate(['/']);
          }
        }, 100); // 100ms delay to ensure auth ready propagates
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
        const errorMessage = error?.error?.message || 'Đăng ký thất bại. Vui lòng thử lại.';
        this.modalService.showError(errorMessage);
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



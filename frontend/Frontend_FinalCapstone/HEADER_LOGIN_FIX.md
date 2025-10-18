# Header Login/Profile Button Fix

## Problem
After successfully logging in, the header still showed "Đăng nhập" (Login) instead of showing the user's name with a profile dropdown menu.

## Root Cause
The header component was checking for user data in localStorage (`getUserResponseFromLocalStorage()`), but we removed user data from localStorage as part of the security fix. This caused `userResponse` to always be `null`, so the header always showed the login button.

## Solution Implemented

### 1. Created Auth State Service
**File**: `src/app/services/auth-state.service.ts` (**NEW**)

A new service to notify components when login/logout events occur:

```typescript
export class AuthStateService {
  private loginStateSubject = new BehaviorSubject<boolean>(false);
  public loginState$: Observable<boolean> = this.loginStateSubject.asObservable();

  notifyLogin(): void { ... }  // Call when user logs in
  notifyLogout(): void { ... } // Call when user logs out
}
```

### 2. Updated Header Component
**File**: `src/app/components/header/header.component.ts`

**Changes:**
- ✅ Removed `getUserResponseFromLocalStorage()` call
- ✅ Added `checkLoginStatus()` method that checks JWT token
- ✅ Added `loadUserProfile()` method to fetch user data from API
- ✅ Subscribed to `AuthStateService` to detect login/logout events
- ✅ Updated logout to notify `AuthStateService`

**Before** (❌):
```typescript
ngOnInit() {
  // Always returns null since we removed user data from localStorage
  this.userResponse = this.userService.getUserResponseFromLocalStorage();
}
```

**After** (✅):
```typescript
ngOnInit() {
  // Check if user is logged in by checking token
  this.checkLoginStatus();
  
  // Subscribe to login state changes
  this.authStateService.loginState$.subscribe(() => {
    this.checkLoginStatus();
  });
}

checkLoginStatus(): void {
  const token = this.tokenService.getToken();
  this.isLoggedIn = !!(token && !this.tokenService.isTokenExpired());
  
  // If logged in, fetch user data from API
  if (this.isLoggedIn) {
    this.loadUserProfile();
  }
}

loadUserProfile(): void {
  // Fetch user profile from API
  this.userService.getMyProfile(token).subscribe({
    next: (response: any) => {
      this.userResponse = {...}; // Set user data
    }
  });
}
```

### 3. Updated Login Component
**File**: `src/app/components/login/login.component.ts`

Added notification when user logs in successfully:

```typescript
// After saving tokens
this.authStateService.notifyLogin(); // ← NEW
```

### 4. Updated OAuth Callback Component
**File**: `src/app/components/auth-callback/auth-callback.component.ts`

Added notification when OAuth login succeeds:

```typescript
// After saving tokens
this.authStateService.notifyLogin(); // ← NEW
```

## How It Works Now

### Login Flow
```
1. User enters credentials and clicks login
   ↓
2. Backend validates and returns tokens
   ↓
3. Frontend saves tokens to localStorage
   ↓
4. authStateService.notifyLogin() is called
   ↓
5. Header component receives login notification
   ↓
6. Header component:
   - Checks token (isLoggedIn = true)
   - Calls API: GET /users/me
   - Displays user's name with dropdown menu
```

### Header Display Logic
```typescript
// In header.component.html
<ng-container *ngIf="userResponse">
  <!-- Show user name with dropdown -->
  <a class="nav-link">{{ userResponse.fullname }}</a>
</ng-container>

<ng-container *ngIf="!userResponse">
  <!-- Show login button -->
  <a class="nav-link" routerLink="/login">Đăng nhập</a>
</ng-container>
```

### Logout Flow
```
1. User clicks "Đăng xuất" in dropdown
   ↓
2. Frontend:
   - Clears tokens from localStorage
   - Calls authStateService.notifyLogout()
   - Sets userResponse = null
   - Redirects to /login
   ↓
3. Header updates to show "Đăng nhập" button
```

## Benefits

### ✅ Real-time Updates
- Header updates immediately after login (no page refresh needed)
- Header updates immediately after logout
- Works with both traditional and OAuth login

### ✅ Always Fresh Data
- User data fetched from API on each login
- No stale data from localStorage

### ✅ Reactive Design
- Uses RxJS observables for state management
- Components automatically react to auth state changes

## Testing

### 1. Clear LocalStorage
```javascript
// In browser console
localStorage.clear();
```

### 2. Test Traditional Login
```powershell
npm start
```

**Steps:**
1. Navigate to `http://localhost:4200`
2. Header should show "Đăng nhập" button
3. Click "Đăng nhập" → Login with credentials
4. After successful login:
   - Should redirect to home page
   - Header should now show your full name (not "Đăng nhập")
   - Clicking your name shows dropdown with:
     - "Tài khoản của tôi" (My Account)
     - "Đơn mua" (My Orders)
     - "Đăng xuất" (Logout)

### 3. Test OAuth Login
1. Click "Login with Google"
2. After OAuth redirect:
   - Header should show your name
   - Dropdown should appear

### 4. Test Profile Navigation
1. Click your name in header
2. Click "Tài khoản của tôi"
3. Should navigate to `/user-profile`

### 5. Test Logout
1. Click your name in header
2. Click "Đăng xuất"
3. Header should change back to "Đăng nhập"
4. Should redirect to `/login`

## Files Modified

1. ✅ `src/app/services/auth-state.service.ts` - **NEW** - Auth state notification service
2. ✅ `src/app/components/header/header.component.ts` - Updated to check token and fetch user data from API
3. ✅ `src/app/components/login/login.component.ts` - Added login notification
4. ✅ `src/app/components/auth-callback/auth-callback.component.ts` - Added OAuth login notification

## Summary

**Before**: Header always showed "Đăng nhập" because user data was removed from localStorage

**After**: Header correctly shows:
- User's name + dropdown menu when logged in
- "Đăng nhập" button when logged out

The header now uses:
- JWT token to check login status
- API to fetch user data
- AuthStateService for real-time updates

This provides a better user experience with immediate visual feedback when logging in or out! 🎉

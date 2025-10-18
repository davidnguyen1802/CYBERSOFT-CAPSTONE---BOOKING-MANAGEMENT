# User Profile "Member Since" Date Fix

## Problem
The user profile page was displaying the user's date of birth (DOB) as the "Member Since" date, which is incorrect. The "Member Since" should show when the user account was created (`create_date` from the backend).

## Backend API Response
The backend `/users/me/details` endpoint returns:
```json
{
  "data": {
    "user_info": {
      "id": 7,
      "create_date": "2025-10-04",  // ← Account creation date
      "dob": "2000-01-10",           // ← User's date of birth
      "fullname": "Linh Guest",
      ...
    }
  }
}
```

## Solution Implemented

### 1. Updated UserResponse Interface
**File**: `src/app/responses/user/user.response.ts`

Added `create_date` field to store the account creation date:
```typescript
export interface UserResponse {
    id: number;
    fullname: string;
    ...
    date_of_birth: Date;
    create_date?: Date; // ← NEW: Member since date
    ...
}
```

### 2. Updated User Profile Component
**File**: `src/app/components/user-profile/user.profile.component.ts`

#### Changed API Call
Switched from `getMyProfile()` to `getMyDetailedProfile()` to access the full `user_info` object:

**Before**:
```typescript
this.userService.getMyProfile(this.token).subscribe({
  next: (response: any) => {
    if (response && response.data) {
      // response.data directly contains user fields
```

**After**:
```typescript
this.userService.getMyDetailedProfile(this.token, true).subscribe({
  next: (response: any) => {
    if (response && response.data && response.data.user_info) {
      const userData = response.data.user_info; // ← Access nested user_info
```

#### Extract create_date
Now extracts `create_date` from the API response:
```typescript
this.userResponse = {
  id: userData.id,
  fullname: userData.fullname,
  ...
  date_of_birth: userData.dob ? new Date(userData.dob) : new Date(),
  create_date: userData.create_date ? new Date(userData.create_date) : undefined, // ← NEW
  ...
};
```

#### Fixed getMemberSinceDate()
Changed from using `date_of_birth` to `create_date`:

**Before**:
```typescript
getMemberSinceDate(): string {
  return this.formatDate(this.userResponse?.date_of_birth); // ❌ Wrong
}
```

**After**:
```typescript
getMemberSinceDate(): string {
  return this.formatDate(this.userResponse?.create_date); // ✅ Correct
}
```

### 3. Updated OAuth Callback Component
**File**: `src/app/components/auth-callback/auth-callback.component.ts`

For users logging in via Google/Facebook OAuth, we now set `create_date` to the current date (since they're creating an account):
```typescript
const userResponse = {
  id: parseInt(id),
  username: username,
  ...
  create_date: new Date(), // ← NEW: Set to current date for new OAuth users
};
```

## Result

### User Profile Display
The "Member Since" field now correctly shows:
- **Before**: User's date of birth (e.g., "Jan 10, 2000")
- **After**: Account creation date (e.g., "Oct 4, 2025")

### HTML Template
No changes needed in the HTML template. It already uses:
```html
<div class="col-md-6 mb-3">
  <strong><i class="fas fa-calendar-plus"></i> Member Since:</strong>
  <p class="mb-0">{{ getMemberSinceDate() }}</p>
</div>
```

The method `getMemberSinceDate()` now returns the correct date.

## Testing

1. **Start the dev server**:
   ```powershell
   npm start
   ```

2. **Test with existing user**:
   - Login with an existing account
   - Navigate to `/user-profile`
   - Verify "Member Since" shows the account creation date, not DOB

3. **Test with OAuth login**:
   - Login with Google
   - Navigate to `/user-profile`
   - Verify "Member Since" shows today's date (when OAuth account was created)

4. **Verify DOB field**:
   - The "Date of Birth" field should still show the user's actual birthday
   - "Member Since" and "Date of Birth" should now show different dates

## Files Modified

1. ✅ `src/app/responses/user/user.response.ts` - Added `create_date` field
2. ✅ `src/app/components/user-profile/user.profile.component.ts` - Updated to use `/users/me/details` API and extract `create_date`
3. ✅ `src/app/components/auth-callback/auth-callback.component.ts` - Set `create_date` for OAuth users

## Summary

The issue was that the `getMemberSinceDate()` method was returning `date_of_birth` instead of `create_date`. This has been fixed by:
- Adding `create_date` to the UserResponse interface
- Updating the profile component to call the correct API endpoint (`/users/me/details`)
- Extracting `create_date` from the nested `user_info` object
- Updating `getMemberSinceDate()` to use `create_date`

Now the "Member Since" field correctly displays when the user account was created, not when they were born! 🎉

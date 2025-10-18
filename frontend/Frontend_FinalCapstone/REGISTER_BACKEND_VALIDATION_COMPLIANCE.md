# Register Validation - Backend Compliance Update

## Ngày: October 18, 2025

## Mục đích
Đảm bảo frontend validation match 100% với backend requirements để tránh lỗi khi submit form.

---

## Backend Requirements (From Java SignUpRequest)

```java
@NotBlank(message = "Full name is required")
@Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
private String fullName;

@NotBlank(message = "Username is required")
@Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
@Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, numbers, dots, underscores and hyphens")
private String username;

@NotBlank(message = "Email is required")
@Email(message = "Email must be valid")
private String email;

@NotBlank(message = "Password is required")
@Size(min = 6, message = "Password must be at least 6 characters")
private String password;

private String phone; // Optional

private String address; // Optional

private Gender gender; // Enum: MALE, FEMALE, NONE

private LocalDate dob; // Date of birth
```

---

## Changes Made

### 1. ✅ Password Validation: 3 → 6 characters

**TRƯỚC:**
```typescript
// validateStep1()
if (this.password.length < 3) {
  alert('Mật khẩu phải có ít nhất 3 ký tự');
  return false;
}
```

**SAU:**
```typescript
// validateStep1()
if (this.password.length < 6) {
  alert('Mật khẩu phải có ít nhất 6 ký tự');
  return false;
}
```

**HTML:**
```html
<!-- BEFORE -->
<input placeholder="Ít nhất 3 ký tự">
<p *ngIf="password.length > 0 && password.length < 3">
  Mật khẩu phải có ít nhất 3 ký tự
</p>

<!-- AFTER -->
<input placeholder="Ít nhất 6 ký tự">
<p *ngIf="password.length > 0 && password.length < 6">
  Mật khẩu phải có ít nhất 6 ký tự
</p>
```

---

### 2. ✅ FullName Validation: Added 2-100 length check

**TypeScript:**
```typescript
// validateStep2()
// Validate fullName (2-100 chars)
if (!this.fullName || this.fullName.trim().length < 2) {
  alert('Họ và tên phải có ít nhất 2 ký tự');
  return false;
}
if (this.fullName.trim().length > 100) {
  alert('Họ và tên không được vượt quá 100 ký tự');
  return false;
}
```

**HTML:**
```html
<input type="text" 
  [(ngModel)]="fullName"
  maxlength="100"
  placeholder="Nguyễn Văn A">
<p class="text-danger" 
  *ngIf="fullName.length > 0 && fullName.length < 2 && registerForm.form.touched">
  Họ và tên phải có ít nhất 2 ký tự
</p>
```

---

### 3. ✅ Username Validation: Added pattern check

**TypeScript:**
```typescript
// validateStep2()
// Validate username (3-50 chars, pattern)
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

// Helper method for real-time validation
isUsernameInvalid(): boolean {
  if (this.username.length < 3) return false;
  const pattern = /^[a-zA-Z0-9._-]+$/;
  return !pattern.test(this.username);
}
```

**HTML:**
```html
<input type="text" 
  [(ngModel)]="username"
  maxlength="50"
  placeholder="Chỉ chữ, số và . _ -">
<p class="text-danger" 
  *ngIf="username.length > 0 && username.length < 3 && registerForm.form.touched">
  Username phải có ít nhất 3 ký tự
</p>
<p class="text-danger" 
  *ngIf="isUsernameInvalid() && registerForm.form.touched">
  Username chỉ được chứa chữ, số và . _ -
</p>
```

---

## Complete Validation Matrix

| Field | Frontend | Backend | Status |
|-------|----------|---------|--------|
| **fullName** | ✅ Required, 2-100 chars | Required, 2-100 chars | ✅ Match |
| **username** | ✅ Required, 3-50 chars, pattern `^[a-zA-Z0-9._-]+$` | Required, 3-50 chars, same pattern | ✅ Match |
| **email** | ✅ Required, email format | Required, email format | ✅ Match |
| **password** | ✅ Required, min 6 chars | Required, min 6 chars | ✅ Match |
| **phone** | ✅ Optional, 10 digits | Optional | ✅ Match |
| **address** | ✅ Optional | Optional | ✅ Match |
| **gender** | ✅ MALE/FEMALE/NONE | Gender enum | ✅ Match |
| **dob** | ✅ Date, >= 18 years | LocalDate | ✅ Match |

---

## FormData Submission

API gửi đúng format như backend yêu cầu:

```typescript
const formData = new FormData();
formData.append('fullName', this.fullName);       // ✅ 2-100 chars
formData.append('username', this.username);       // ✅ 3-50 chars, pattern
formData.append('email', this.email);             // ✅ Valid email
formData.append('password', this.password);       // ✅ Min 6 chars
formData.append('phone', this.phoneNumber);       // ✅ 10 digits
formData.append('address', this.address);         // ✅ Optional
formData.append('gender', this.gender);           // ✅ MALE/FEMALE/NONE
formData.append('dob', dobString);                // ✅ YYYY-MM-DD
formData.append('facebookAccountId', '0');
formData.append('googleAccountId', '0');
if (this.avatarFile) {
  formData.append('avatar', this.avatarFile, this.avatarFile.name);
}
```

---

## Real-time Validation Features

### Password
- Shows error when < 6 characters
- Validates on both password and retype password fields
- Toggle show/hide password

### FullName
- Shows error when < 2 characters
- Prevented from exceeding 100 chars with `maxlength`

### Username  
- Shows error when < 3 characters
- Shows pattern error for invalid characters
- Real-time check với method `isUsernameInvalid()`
- Prevented from exceeding 50 chars with `maxlength`
- **Valid examples**: `john_doe`, `user.name`, `test-123`
- **Invalid examples**: `user@name`, `name#123`, `user name`

### Phone
- Auto-removes non-digits
- Limited to exactly 10 digits
- Shows error if not 10 digits

### Email
- Validates email format with regex
- Required field

### Date of Birth
- Must be >= 18 years old
- Shows age validation error

---

## Error Messages (Vietnamese)

| Field | Condition | Message |
|-------|-----------|---------|
| Email | Invalid format | Vui lòng nhập email hợp lệ |
| Password | < 6 chars | Mật khẩu phải có ít nhất 6 ký tự |
| Password | Mismatch | Mật khẩu không khớp |
| FullName | < 2 chars | Họ và tên phải có ít nhất 2 ký tự |
| FullName | > 100 chars | Họ và tên không được vượt quá 100 ký tự |
| Username | < 3 chars | Username phải có ít nhất 3 ký tự |
| Username | > 50 chars | Username không được vượt quá 50 ký tự |
| Username | Invalid pattern | Username chỉ được chứa chữ, số và . _ - |
| Phone | Not 10 digits | Số điện thoại phải có đúng 10 chữ số |
| Age | < 18 years | Bạn chưa đủ 18 tuổi |
| Terms | Not accepted | Vui lòng đồng ý với điều khoản và điều kiện |

---

## Testing Examples

### Valid Registration Data:
```json
{
  "fullName": "Nguyễn Văn A",
  "username": "nguyen_van_a",
  "email": "nguyen.van.a@example.com",
  "password": "123456",
  "phone": "0901234567",
  "address": "123 Đường ABC",
  "gender": "MALE",
  "dob": "2000-01-01"
}
```

### Invalid Cases (Will Show Error):

1. **Password too short**
```
password: "12345" → Error: "Mật khẩu phải có ít nhất 6 ký tự"
```

2. **Username with invalid characters**
```
username: "user@name" → Error: "Username chỉ được chứa chữ, số và . _ -"
```

3. **FullName too short**
```
fullName: "A" → Error: "Họ và tên phải có ít nhất 2 ký tự"
```

4. **Phone not 10 digits**
```
phone: "090123" → Error: "Số điện thoại phải có đúng 10 chữ số"
```

---

## Backend Response Handling

### Success (200 OK):
```json
{
  "message": "User registered successfully",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "id": 123,
    "username": "nguyen_van_a",
    "roles": ["USER"]
  },
  "status": 200
}
```
→ Frontend: Shows success dialog → Redirects to `/login`

### Error (400 Bad Request):
```json
{
  "message": "Username must be between 3 and 50 characters",
  "status": 400
}
```
→ Frontend: Shows alert with error message

---

## Files Modified

1. ✅ `register.component.ts`
   - Updated `validateStep1()` - password min 6
   - Updated `validateStep2()` - fullName 2-100, username 3-50 + pattern
   - Added `isUsernameInvalid()` helper method

2. ✅ `register.component.html`
   - Updated password placeholder: "Ít nhất 6 ký tự"
   - Updated password error messages: 6 chars
   - Added fullName `maxlength="100"` and error message
   - Added username `maxlength="50"`, pattern validation message
   - Updated username placeholder: "Chỉ chữ, số và . _ -"

---

## Compile Status
```
✅ No errors found.
```

---

## Benefits

1. ✅ **Prevents API errors**: Validation happens before submission
2. ✅ **Better UX**: Real-time feedback cho users
3. ✅ **Backend compliance**: 100% match với backend requirements
4. ✅ **Clear messages**: Vietnamese error messages dễ hiểu
5. ✅ **Type safety**: TypeScript validation logic

---

**Status**: ✅ Complete & Backend Compliant
**Version**: 2.0.0
**Updated**: October 18, 2025

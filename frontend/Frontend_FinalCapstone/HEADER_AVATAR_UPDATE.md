# 🎨 HEADER USER PROFILE UPDATE

## Tổng quan
Cập nhật header để hiển thị avatar và username của người dùng khi đã đăng nhập, thay thế nút "Đăng nhập". Khi click vào avatar/username sẽ chuyển đến trang user-profile.

---

## 📋 Các thay đổi đã thực hiện

### 1. **Header Component TypeScript** (`header.component.ts`)

#### ✅ Thêm properties mới:
```typescript
avatarUrl: string = '';  // URL của avatar user
private baseUrl = environment.apiBaseUrl || 'http://localhost:8080';
```

#### ✅ Cập nhật `loadUserProfile()`:
```typescript
// Set avatar URL based on user ID
if (this.userResponse && this.userResponse.id) {
    this.avatarUrl = `${this.baseUrl}/files/avatar_user${this.userResponse.id}.jpg`;
    console.log('👤 Avatar URL:', this.avatarUrl);
}
```

#### ✅ Thêm methods mới:
```typescript
// Navigate to user profile
navigateToProfile(): void {
    console.log('➡️ Navigating to user profile');
    this.router.navigate(['/user-profile']);
}

// Handle avatar image error (fallback to default)
onAvatarError(): void {
    console.warn('⚠️ Avatar image failed to load, using default');
    this.avatarUrl = 'assets/img/default-avatar.svg';
}
```

---

### 2. **Header Component Template** (`header.component.html`)

#### ✅ Thay đổi UI khi logged in:

**Trước (Old):**
```html
<ng-container *ngIf="userResponse">
  <ng-container ngbPopover...>
    <a class="nav-link">
      {{ userResponse.fullname }}
    </a>
    <!-- Popover with menu items -->
  </ng-container>
</ng-container>
<ng-container *ngIf="!userResponse">
  <a class="nav-link" routerLink="/login">Đăng nhập</a>
</ng-container>
```

**Sau (New):**
```html
<!-- When user is logged in: Show avatar and username -->
<ng-container *ngIf="isLoggedIn && userResponse">
  <a class="nav-link user-profile-link" 
     (click)="navigateToProfile()"
     style="cursor: pointer;">
    <img 
      [src]="avatarUrl" 
      (error)="onAvatarError()"
      alt="User Avatar"
      class="user-avatar"
      onerror="this.src='https://via.placeholder.com/40?text=User'"
    />
    <span class="username-text">{{ userResponse.fullname }}</span>
  </a>
</ng-container>

<!-- When user is NOT logged in: Show Login button -->
<ng-container *ngIf="!isLoggedIn">
  <a class="nav-link" routerLink="/login">
    <i class="fas fa-sign-in-alt"></i> Đăng nhập
  </a>
</ng-container>
```

**Đặc điểm:**
- ✅ Hiển thị avatar hình tròn 40x40px
- ✅ Hiển thị username bên cạnh avatar
- ✅ Click vào anywhere (avatar hoặc username) sẽ redirect đến `/user-profile`
- ✅ Có fallback khi avatar load fail (2 layers):
  1. Method `onAvatarError()` → đổi sang `default-avatar.svg`
  2. HTML `onerror` → đổi sang placeholder từ placeholder.com

---

### 3. **Header Component Styles** (`header.component.scss`)

#### ✅ Thêm CSS cho user profile link:

```scss
/* User Profile Link with Avatar */
.user-profile-link {
    display: flex !important;
    align-items: center !important;
    gap: 10px;
    padding: 5px 16px !important;
    transition: all 0.3s ease;
    
    &:hover {
        background-color: rgba(255, 255, 255, 0.1) !important;
        transform: translateY(-2px);  // Lift effect on hover
    }
}

.user-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
    border: 2px solid rgba(255, 255, 255, 0.3);
    transition: all 0.3s ease;
    
    &:hover {
        border-color: $shopapp-color;  // Pink/purple border on hover
        box-shadow: 0 0 10px rgba(240, 101, 197, 0.5);  // Glow effect
    }
}

.username-text {
    color: rgb(173, 178, 203);
    font-weight: 500;
    max-width: 150px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    
    .user-profile-link:hover & {
        color: white;  // Change to white on hover
    }
}

// Responsive adjustments
@media (max-width: 768px) {
    .username-text {
        max-width: 100px;  // Shorter on mobile
    }
    
    .user-avatar {
        width: 35px;
        height: 35px;  // Smaller on mobile
    }
}
```

**Hiệu ứng:**
- ✅ Hover → background fade in + lift up animation
- ✅ Avatar hover → border color change + glow effect
- ✅ Username hover → color change to white
- ✅ Responsive design cho mobile

---

### 4. **Default Avatar** (`assets/img/default-avatar.svg`)

#### ✅ Tạo SVG default avatar:
```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <circle cx="50" cy="50" r="50" fill="#e0e0e0"/>
  <circle cx="50" cy="35" r="15" fill="#9e9e9e"/>
  <path d="M 25 75 Q 25 55 50 55 Q 75 55 75 75 Z" fill="#9e9e9e"/>
</svg>
```

**Icon đơn giản:**
- Background circle (gray)
- Head circle
- Body shape

---

## 🔄 FLOW HOẠT ĐỘNG

### **1. User đăng nhập**
```
Login Success 
  ↓
AuthStateService.notifyLogin() 
  ↓
HeaderComponent detects login state change
  ↓
checkLoginStatus() → isLoggedIn = true
  ↓
loadUserProfile() → Fetch user data from API
  ↓
Set avatarUrl = baseUrl + "/files/avatar_user<ID>.jpg"
  ↓
Template renders:
  - Avatar image
  - Username text
  - Clickable link to profile
```

### **2. Avatar Load Process**
```
Try load: baseUrl + "/files/avatar_user<ID>.jpg"
  ↓
  ├─ Success → Display user's avatar
  ↓
  └─ Fail (404/error)
       ↓
       (error)="onAvatarError()" triggers
       ↓
       avatarUrl = "assets/img/default-avatar.svg"
       ↓
       ├─ Success → Display default SVG avatar
       ↓
       └─ Fail → HTML onerror triggers
              ↓
              Display placeholder.com image
```

### **3. Click vào Avatar/Username**
```
User clicks avatar or username
  ↓
navigateToProfile() method called
  ↓
Console: "➡️ Navigating to user profile"
  ↓
router.navigate(['/user-profile'])
  ↓
User redirected to profile page
```

---

## 🎯 AVATAR URL FORMAT

### **Backend API Expected:**
```
GET {baseUrl}/files/avatar_user{userId}.jpg

Example:
- User ID: 123
- Avatar URL: http://localhost:8080/files/avatar_user123.jpg
```

### **Fallback Chain:**
1. **Primary:** `{baseUrl}/files/avatar_user{userId}.jpg`
2. **Secondary:** `assets/img/default-avatar.svg` (local SVG)
3. **Tertiary:** `https://via.placeholder.com/40?text=User` (external placeholder)

---

## 🎨 VISUAL DESIGN

### **Normal State:**
```
┌──────────────────────────────┐
│  [○]  John Doe              │  ← Avatar + Username
└──────────────────────────────┘
```

### **Hover State:**
```
┌──────────────────────────────┐
│  [●]  John Doe              │  ← Lifted, glowing avatar, white text
└──────────────────────────────┘
     ↑ hover effect
```

### **Mobile (< 768px):**
```
┌────────────────┐
│  [○]  John D.. │  ← Smaller avatar, truncated text
└────────────────┘
```

---

## 🧪 TESTING CHECKLIST

### **Khi user chưa đăng nhập:**
- [ ] Header hiển thị nút "Đăng nhập"
- [ ] Click "Đăng nhập" → redirect to `/login`

### **Khi user đã đăng nhập:**
- [ ] Header hiển thị avatar và username
- [ ] Avatar load từ `{baseUrl}/files/avatar_user{id}.jpg`
- [ ] Click vào avatar → redirect to `/user-profile`
- [ ] Click vào username → redirect to `/user-profile`
- [ ] Hover vào avatar/username → animation effect

### **Avatar error handling:**
- [ ] Nếu avatar không tồn tại → hiển thị default SVG
- [ ] Nếu default SVG fail → hiển thị placeholder.com
- [ ] Console log: "⚠️ Avatar image failed to load, using default"

### **Responsive:**
- [ ] Desktop: avatar 40x40, full username
- [ ] Mobile: avatar 35x35, truncated username

### **Console Logs:**
- [ ] "📥 Loading user profile for header..."
- [ ] "👤 Avatar URL: http://..."
- [ ] "➡️ Navigating to user profile" (on click)
- [ ] "⚠️ Avatar image failed to load, using default" (on error)

---

## 📁 FILES MODIFIED

1. ✅ `header.component.ts` - Added avatar URL logic and navigation method
2. ✅ `header.component.html` - Updated UI to show avatar + username
3. ✅ `header.component.scss` - Added styling for avatar and hover effects
4. ✅ `assets/img/default-avatar.svg` - Created default avatar SVG

---

## 🚀 DEPLOYMENT NOTES

### **Backend Requirements:**
Ensure backend serves avatar images at:
```
GET /files/avatar_user{userId}.jpg
```

Example response headers:
```
Content-Type: image/jpeg
Access-Control-Allow-Origin: *
```

### **File Structure:**
```
backend/
  └─ public/
      └─ files/
          ├─ avatar_user1.jpg
          ├─ avatar_user2.jpg
          ├─ avatar_user123.jpg
          └─ ...
```

---

## 💡 FUTURE ENHANCEMENTS

1. **Dropdown Menu on Avatar Click**
   - Add menu with: Profile, Settings, Logout
   - Keep direct click to profile as default

2. **Avatar Upload Feature**
   - Allow users to upload/change avatar from profile page
   - Real-time update in header after upload

3. **Online Status Indicator**
   - Small green dot on avatar when user is active
   - WebSocket integration for real-time status

4. **Notification Badge**
   - Show notification count on avatar
   - Red badge with number

5. **Avatar Optimization**
   - Use WebP format for better performance
   - Lazy loading for avatars
   - CDN integration

---

**Last Updated**: October 18, 2025  
**Author**: AI Assistant  
**Version**: 1.0

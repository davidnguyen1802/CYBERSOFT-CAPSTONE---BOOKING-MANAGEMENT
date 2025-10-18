# Debug: Token Not Saving Issue

## Quick Check

Mở DevTools Console và chạy register, xem log nào xuất hiện:

### Scenario 1: Response không có data
```
✅ Registration successful: {...}
❌ Invalid response structure: {...}
Alert: "Invalid response from server"
```
**→ Backend response structure sai**

### Scenario 2: Token là undefined
```
✅ Registration successful: {...}
🔑 Token received: undefined
```
**→ response.data.token không tồn tại**

### Scenario 3: TokenService không được gọi
```
✅ Registration successful: {...}
🔑 Token received: eyJ...
(Không có log "🔑 Token saved to localStorage")
```
**→ Code return sớm hoặc error**

### Scenario 4: LocalStorage bị block
```
✅ Registration successful: {...}
🔑 Token received: eyJ...
🔑 Token saved to localStorage
(Nhưng localStorage vẫn trống)
```
**→ Browser privacy mode hoặc localStorage disabled**

---

## Manual Debug Steps

### Step 1: Check Response in Network Tab
1. F12 → Network tab
2. Submit register form
3. Find `POST /auth/signup`
4. Tab "Response"
5. Copy response body và paste vào đây

Expected:
```json
{
  "message": "Sign up successfully",
  "status": "OK",
  "data": {
    "token": "eyJ...",
    "username": "...",
    "roles": [...]
  }
}
```

### Step 2: Check Console Logs
Submit form và copy ALL console logs từ:
```
📝 Register attempt started
```
đến
```
✅ Registration process complete
```

### Step 3: Check LocalStorage Manually
1. F12 → Application tab
2. Storage → Local Storage → http://localhost:4200
3. Xem có key `access_token` không?

---

## Possible Issues & Fixes

### Issue 1: Response structure khác
**Backend có thể trả:**
```json
{
  "token": "eyJ...",
  "username": "..."
}
```
Không có wrapper `data`.

**Fix:**
```typescript
const authData = response.data || response;
const token = authData.token;
```

### Issue 2: Field name khác
Backend có thể dùng `accessToken` thay vì `token`.

**Check:**
```typescript
console.log('Response keys:', Object.keys(response.data));
```

### Issue 3: UserService không return đúng
UserService có thể đang map response sai.

**Check:**
```typescript
// In user.service.ts
register(formData): Observable<any> {
  return this.http.post(url, formData).pipe(
    tap(res => console.log('UserService received:', res))
  );
}
```

---

## Quick Fix to Try

Add this temporary debug code:

```typescript
next: (response: any) => {
  // TEMP DEBUG - Log everything
  console.log('=== REGISTER RESPONSE DEBUG ===');
  console.log('Response:', response);
  console.log('Response type:', typeof response);
  console.log('Has data?', 'data' in response);
  console.log('Response.data:', response?.data);
  console.log('Response.token:', response?.token);
  console.log('Response.data.token:', response?.data?.token);
  console.log('All keys:', Object.keys(response));
  if (response.data) {
    console.log('Data keys:', Object.keys(response.data));
  }
  console.log('=== END DEBUG ===');
  
  // Rest of your code...
}
```

This will tell us EXACTLY what backend returns.

---

## Test Cases

Submit form và paste kết quả của những log này:

1. Console log full response
2. Network tab response body
3. LocalStorage screenshot
4. Any error messages

Gửi cho tôi và tôi sẽ fix ngay!

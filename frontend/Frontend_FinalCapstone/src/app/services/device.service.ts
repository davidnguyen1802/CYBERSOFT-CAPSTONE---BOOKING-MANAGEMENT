import { Injectable } from '@angular/core';

/**
 * Device Service
 * 
 * UPDATED PRINCIPLES (Option B - Auto-validate + Auto-generate):
 * - Device ID được TẠO TỰ ĐỘNG khi getDeviceId() được gọi lần đầu
 * - AUTO-VALIDATE format UUID v4, tự động regenerate nếu invalid
 * - Frontend validation + Backend validation = Defense in depth
 * 
 * Device ID:
 * - Format: UUID v4 (e.g., "550e8400-e29b-41d4-a716-446655440000")
 * - Persist trong localStorage với key 'device_id'
 * - Sent với AUTH requests (login, signup, refresh, logout) qua DeviceIdInterceptor
 * 
 * Backend usage:
 * - Track device cụ thể của user
 * - Limit max 3 active devices per user
 * - Auto-revoke oldest device khi vượt limit
 * - Validate X-Device-Id header (trả 428 nếu missing/invalid)
 * 
 * Flow:
 * 1. User chưa có deviceId → getDeviceId() auto-generate → localStorage lưu
 * 2. User đã có deviceId hợp lệ → getDeviceId() return existing
 * 3. User có deviceId INVALID (corrupt) → getDeviceId() auto-regenerate
 * 4. Backend trả 428 → TokenInterceptor clear + retry → getDeviceId() generate mới
 */
@Injectable({
  providedIn: 'root'
})
export class DeviceService {
  private readonly DEVICE_ID_KEY = 'device_id';

  /**
   * Get current device ID (with auto-validation + auto-generation)
   * 
   * @returns Device ID (luôn trả về valid UUID v4, KHÔNG BAO GIỜ null)
   * 
   * NEW BEHAVIOR (Option B):
   * - Nếu CHƯA có deviceId → Auto-generate mới
   * - Nếu CÓ deviceId NHƯNG invalid format → Auto-regenerate
   * - Nếu CÓ deviceId hợp lệ → Return existing
   * 
   * This ensures:
   * - DeviceIdInterceptor LUÔN gắn valid X-Device-Id header
   * - User KHÔNG BAO GIỜ gặp 428 error (trừ edge cases)
   * - Code đơn giản hơn (không cần null check)
   */
  getDeviceId(): string {
    let deviceId = localStorage.getItem(this.DEVICE_ID_KEY);
    
    // Validate format + auto-fix nếu invalid
    if (!deviceId || !this.isValidUUIDv4(deviceId)) {
      if (deviceId) {
        console.warn('⚠️ Invalid deviceId format detected, regenerating...');
        console.warn('   Old (invalid):', deviceId);
      }
      deviceId = this.generateAndCommit();
    }
    
    return deviceId; // Always return valid UUID v4
  }

  /**
   * Generate new device ID và persist vào localStorage
   * 
   * INTERNAL USE:
   * - Called by getDeviceId() when auto-generating
   * - Called by TokenInterceptor.handle428Error() when backend rejects deviceId
   * 
   * PUBLIC USE (optional):
   * - Testing multi-device scenarios
   * - "Forget this device" feature
   * 
   * @returns Device ID vừa được tạo
   */
  generateAndCommit(): string {
    // Use native crypto.randomUUID() (Chrome 92+, Edge 92+, Firefox 95+)
    // More secure than Math.random() based solutions
    const deviceId = crypto.randomUUID();
    localStorage.setItem(this.DEVICE_ID_KEY, deviceId);
    console.log('🆕 Device ID generated:', deviceId);
    return deviceId;
  }

  /**
   * Validate UUID v4 format
   * 
   * UUID v4 format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
   * - 4th group starts with 4 (version 4)
   * - 5th group starts with 8, 9, a, or b (variant bits)
   * 
   * @param uuid String to validate
   * @returns true nếu đúng UUID v4 format, false nếu không
   */
  private isValidUUIDv4(uuid: string): boolean {
    const uuidV4Regex = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
    return uuidV4Regex.test(uuid);
  }

  /**
   * Clear device ID from localStorage
   * 
   * Use cases:
   * - Testing multi-device scenarios
   * - "Forget this device" feature
   * - QA/debugging
   * 
   * NOTE: Không cần gọi khi logout thường.
   * Device ID nên persist để user không bị count nhiều devices.
   */
  clearDeviceId(): void {
    const deviceId = localStorage.getItem(this.DEVICE_ID_KEY);
    if (deviceId) {
      localStorage.removeItem(this.DEVICE_ID_KEY);
      console.log('🗑️ Device ID cleared:', deviceId);
    } else {
      console.log('⚠️ No device ID to clear');
    }
  }
}

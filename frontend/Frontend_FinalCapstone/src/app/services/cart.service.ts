import { Injectable } from '@angular/core';
import { Product } from '../models/product';
import { TokenService } from './token.service';

@Injectable({
  providedIn: 'root'
})

export class CartService {
  private cart: Map<number, number> = new Map<number, number>(); // Dùng Map để lưu trữ giỏ hàng, key là id sản phẩm, value là số lượng

  constructor(private tokenService: TokenService) {
    // Lấy dữ liệu giỏ hàng từ localStorage khi khởi tạo service
    console.log('🛒 Cart Service initialized');
    this.refreshCart()
  }
  public  refreshCart(){
    const storedCart = localStorage.getItem(this.getCartKey());
    if (storedCart) {
      this.cart = new Map(JSON.parse(storedCart));
      console.log(`🛒 Cart refreshed from localStorage: ${this.cart.size} items`);
    } else {
      this.cart = new Map<number, number>();
      console.log('🛒 Cart initialized as empty');
    }
  }
  private getCartKey():string {    
    // Get user ID from token instead of localStorage
    const userId = this.tokenService.getUserId();
    const key = `cart:${userId || ''}`;
    console.log(`🛒 Cart key: ${key}`);
    return key;
  }

  addToCart(productId: number, quantity: number = 1): void {
    if (this.cart.has(productId)) {
      // Nếu sản phẩm đã có trong giỏ hàng, tăng số lượng lên `quantity`
      const newQuantity = this.cart.get(productId)! + quantity;
      this.cart.set(productId, newQuantity);
      console.log(`🛒 Updated product ${productId} quantity to ${newQuantity}`);
    } else {
      // Nếu sản phẩm chưa có trong giỏ hàng, thêm sản phẩm vào với số lượng là `quantity`
      this.cart.set(productId, quantity);
      console.log(`🛒 Added product ${productId} with quantity ${quantity}`);
    }
     // Sau khi thay đổi giỏ hàng, lưu trữ nó vào localStorage
    this.saveCartToLocalStorage();
  }
  
  getCart(): Map<number, number> {
    console.log(`🛒 Getting cart: ${this.cart.size} items`);
    return this.cart;
  }
  // Lưu trữ giỏ hàng vào localStorage
  private saveCartToLocalStorage(): void {
    localStorage.setItem(this.getCartKey(), JSON.stringify(Array.from(this.cart.entries())));
    console.log(`🛒 Cart saved to localStorage: ${this.cart.size} items`);
  }  
  setCart(cart : Map<number, number>) {
    this.cart = cart ?? new Map<number, number>();
    console.log(`🛒 Cart set with ${this.cart.size} items`);
    this.saveCartToLocalStorage();
  }
  // Hàm xóa dữ liệu giỏ hàng và cập nhật Local Storage
  clearCart(): void {
    this.cart.clear(); // Xóa toàn bộ dữ liệu trong giỏ hàng
    console.log('🛒 Cart cleared');
    this.saveCartToLocalStorage(); // Lưu giỏ hàng mới vào Local Storage (trống)
  }
}

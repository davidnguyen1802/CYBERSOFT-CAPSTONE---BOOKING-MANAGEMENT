import { ProductService } from './product.service';
import { Injectable } from '@angular/core';
import { 
  HttpClient, 
  HttpParams, 
  HttpHeaders 
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { getBaseUrl } from '../utils/url.util';
import { OrderDTO } from '../dtos/order/order.dto';
import { OrderResponse } from '../responses/order/order.response';

@Injectable({
  providedIn: 'root',
})
export class OrderService {
  private baseUrl = getBaseUrl();
  private apiUrl = `${this.baseUrl}/orders`;
  private apiGetAllOrders = `${this.baseUrl}/orders/get-orders-by-keyword`;

  constructor(private http: HttpClient) {}

  placeOrder(orderData: OrderDTO): Observable<any> {
    console.log('🔵 API Call: POST /orders', orderData);
    // Gửi yêu cầu đặt hàng
    return this.http.post(this.apiUrl, orderData);
  }
  getOrderById(orderId: number): Observable<any> {
    console.log(`🔵 API Call: GET /orders/${orderId}`);
    const url = `${this.baseUrl}/orders/${orderId}`;
    return this.http.get(url);
  }
  getAllOrders(keyword:string,
    page: number, limit: number
  ): Observable<OrderResponse[]> {
      console.log(`🔵 API Call: GET /orders/get-orders-by-keyword?keyword=${keyword}&page=${page}&limit=${limit}`);
      const params = new HttpParams()
      .set('keyword', keyword)      
      .set('page', page.toString())
      .set('limit', limit.toString());            
      return this.http.get<any>(this.apiGetAllOrders, { params });
  }
  updateOrder(orderId: number, orderData: OrderDTO): Observable<any> {
    console.log(`🔵 API Call: PUT /orders/${orderId}`, orderData);
    const url = `${this.baseUrl}/orders/${orderId}`;
    return this.http.put(url, orderData);
  }
  deleteOrder(orderId: number): Observable<any> {
    console.log(`🔵 API Call: DELETE /orders/${orderId}`);
    const url = `${this.baseUrl}/orders/${orderId}`;
    return this.http.delete(url, { responseType: 'text' });
  }
}

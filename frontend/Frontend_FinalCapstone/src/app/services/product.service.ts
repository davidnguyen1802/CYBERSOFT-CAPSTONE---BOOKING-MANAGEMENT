import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { getBaseUrl } from '../utils/url.util';
import { Product } from '../models/product';
import { UpdateProductDTO } from '../dtos/product/update.product.dto';
import { InsertProductDTO } from '../dtos/product/insert.product.dto';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private apiBaseUrl = getBaseUrl();

  constructor(private http: HttpClient) { }

  getProducts(
    keyword: string,
    categoryId: number,
    page: number,
    limit: number
  ): Observable<Product[]> {
    console.log(`🔵 API Call: GET /products?keyword=${keyword}&category_id=${categoryId}&page=${page}&limit=${limit}`);
    const params = {
      keyword: keyword,
      category_id: categoryId.toString(),
      page: page.toString(),
      limit: limit.toString()
    };
    return this.http.get<Product[]>(`${this.apiBaseUrl}/products`, { params });
  }

  getDetailProduct(productId: number): Observable<Product> {
    console.log(`🔵 API Call: GET /products/${productId}`);
    return this.http.get<Product>(`${this.apiBaseUrl}/products/${productId}`);
  }

  getProductsByIds(productIds: number[]): Observable<Product[]> {
    console.log(`🔵 API Call: GET /products/by-ids?ids=${productIds.join(',')}`);
    const params = new HttpParams().set('ids', productIds.join(','));
    return this.http.get<Product[]>(`${this.apiBaseUrl}/products/by-ids`, { params });
  }
  deleteProduct(productId: number): Observable<string> {
    console.log(`🔵 API Call: DELETE /products/${productId}`);
    return this.http.delete<string>(`${this.apiBaseUrl}/products/${productId}`);
  }
  updateProduct(productId: number, updatedProduct: UpdateProductDTO): Observable<UpdateProductDTO> {
    console.log(`🔵 API Call: PUT /products/${productId}`, updatedProduct);
    return this.http.put<Product>(`${this.apiBaseUrl}/products/${productId}`, updatedProduct);
  }  
  insertProduct(insertProductDTO: InsertProductDTO): Observable<any> {
    console.log('🔵 API Call: POST /products', insertProductDTO);
    // Add a new product
    return this.http.post(`${this.apiBaseUrl}/products`, insertProductDTO);
  }
  uploadImages(productId: number, files: File[]): Observable<any> {
    console.log(`🔵 API Call: POST /products/uploads/${productId} (${files.length} files)`);
    const formData = new FormData();
    for (let i = 0; i < files.length; i++) {
      formData.append('files', files[i]);
    }
    // Upload images for the specified product id
    return this.http.post(`${this.apiBaseUrl}/products/uploads/${productId}`, formData);
  }
  deleteProductImage(id: number): Observable<any> {
    console.log(`🔵 API Call: DELETE /product_images/${id}`);
    return this.http.delete<string>(`${this.apiBaseUrl}/product_images/${id}`);
  }
}
//update.category.admin.component.html
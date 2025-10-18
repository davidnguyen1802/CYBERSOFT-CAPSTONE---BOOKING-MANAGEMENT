import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { getBaseUrl } from '../utils/url.util';
import { Category } from '../models/category';
import { UpdateCategoryDTO } from '../dtos/category/update.category.dto';
import { InsertCategoryDTO } from '../dtos/category/insert.category.dto';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {

  private apiBaseUrl = getBaseUrl();

  constructor(private http: HttpClient) { }
  getCategories(page: number, limit: number):Observable<Category[]> {
    console.log(`🔵 API Call: GET /categories?page=${page}&limit=${limit}`);
    const params = new HttpParams()
      .set('page', page.toString())
      .set('limit', limit.toString());     
  return this.http.get<Category[]>(`${this.apiBaseUrl}/categories`, { params });           
  }
  getDetailCategory(id: number): Observable<Category> {
    console.log(`🔵 API Call: GET /categories/${id}`);
    return this.http.get<Category>(`${this.apiBaseUrl}/categories/${id}`);
  }
  deleteCategory(id: number): Observable<string> {
    console.log(`🔵 API Call: DELETE /categories/${id}`);
    return this.http.delete<string>(`${this.apiBaseUrl}/categories/${id}`);
  }
  updateCategory(id: number, updatedCategory: UpdateCategoryDTO): Observable<UpdateCategoryDTO> {
    console.log(`🔵 API Call: PUT /categories/${id}`, updatedCategory);
    return this.http.put<Category>(`${this.apiBaseUrl}/categories/${id}`, updatedCategory);
  }  
  insertCategory(insertCategoryDTO: InsertCategoryDTO): Observable<any> {
    console.log('🔵 API Call: POST /categories', insertCategoryDTO);
    // Add a new category
    return this.http.post(`${this.apiBaseUrl}/categories`, insertCategoryDTO);
  }
}

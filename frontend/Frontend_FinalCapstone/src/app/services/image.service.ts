import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ImageService {
  private apiUrl = `${environment.apiBaseUrl}/images`;

  constructor(private http: HttpClient) {}

  /**
   * Add images to property
   * @param propertyId Property ID
   * @param files Array of image files
   * @param descriptions Array of image descriptions
   */
  addImagesToProperty(propertyId: number, files: File[], descriptions: string[]): Observable<any> {
    const formData = new FormData();
    
    // Add propertyId
    formData.append('propertyId', propertyId.toString());
    
    // Add all files
    files.forEach((file, index) => {
      formData.append('file', file, file.name);
    });
    
    // Add all descriptions
    descriptions.forEach((desc, index) => {
      formData.append('imageDescription', desc);
    });

    return this.http.post(`${this.apiUrl}/property`, formData);
  }
}


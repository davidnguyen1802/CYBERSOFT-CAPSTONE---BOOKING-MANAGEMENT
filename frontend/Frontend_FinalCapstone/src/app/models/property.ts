export interface PropertyImage {
  imageId: number;
  imageUrl: string;
  description: string;
  createDate: string;
  updateDate: string;
}

export interface PropertyReview {
  reviewId: number;
  username: string;
  propertyName: string;
  propertyId: number;
  comment: string;
  rating: number;
  reviewDate: string;
}

export interface Amenity {
  id: number;
  iconUrl: string;
  amenityName: string;
  description: string;
}

export interface Facility {
  id: number;
  iconUrl: string;
  facilityName: string;
  quantity: number;
  price?: number; // Optional since API doesn't return it
  isBroken?: boolean; // Optional since API doesn't return it
}

export interface Property {
  id: number;
  name: string;
  rating: number;
  hostName: string;
  address: string;
  locationName: string;
  cityName: string;
  pricePerNight: number;
  numberOfBedrooms: number;
  numberOfBathrooms: number;
  maxAdults: number;
  maxChildren: number;
  maxInfants: number;
  maxPets: number;
  propertyType: number; // 0: Apartment, 1: House, 2: Hotel
  description: string;
  images: PropertyImage[];
  reviews: PropertyReview[];
  amenities: Amenity[];
  facilities: Facility[];
  nameUserFavorites: string[];
  available: boolean;
  guestFavorite: boolean;
  createdAt?: string;
  updatedAt?: string;
  createDate?: string; // New field from BE
}

export interface PropertyTypeLabel {
  0: 'Apartment';
  1: 'House';
  2: 'Hotel';
}

// ================ NEW SEARCH & FILTER INTERFACES ================

// Enums
export enum PropertyType {
  VILLA = 'VILLA',
  APARTMENT = 'APARTMENT', 
  HOUSE = 'HOUSE',
  CONDO = 'CONDO'
}

export enum SortDirection {
  ASC = 'ASC',
  DESC = 'DESC'
}

export type SortField = 
  | 'priority' 
  | 'pricePerNight' 
  | 'createdAt' 
  | 'updatedAt' 
  | 'overallRating' 
  | 'propertyName'
  | 'numberOfBedrooms'
  | 'numberOfBathrooms';

// Lightweight DTO for list/card view  
export interface PropertyListItem {
  id: number;
  name: string;
  rating: number;
  hostName: string;
  locationName: string;
  cityName: string;
  pricePerNight: number;
  numberOfBedrooms: number;
  numberOfBathrooms: number;
  maxAdults: number;
  maxPets: number;
  propertyType: PropertyType;
  guestFavorite: boolean;
  thumbnailImageUrl: string;
}

// Pagination metadata
export interface PageResponse<T> {
  content: T[];
  currentPage: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// Base API response wrapper
export interface BaseResponse<T> {
  code: number;
  message: string;
  data: T;
}

// Search request payload
export interface PropertySearchRequest {
  type?: number;
  city?: string;
  location?: string;
  minPrice?: number;
  maxPrice?: number;
  bedrooms?: number;
  bathrooms?: number;
  maxAdults?: number;
  maxChildren?: number;
  maxInfants?: number;
  maxPets?: number;
  amenities?: number[];
  facilities?: number[];
  name?: string;
  page?: number;
  size?: number;
  sortBy?: SortField;
  sortDirection?: SortDirection;
}

// UI filter state (matches search request but with UI-specific defaults)
export interface PropertyFilterState extends PropertySearchRequest {
  // UI-only fields
  showAllCities?: boolean;
  showAllLocations?: boolean;
}

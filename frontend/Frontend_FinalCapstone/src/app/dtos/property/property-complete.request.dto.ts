/**
 * DTO for creating complete property in one request
 * POST /property/complete
 */
export interface PropertyCompleteRequestDTO {
  property: PropertyBasicInfo;
  images?: File[];
  imageDescriptions?: string[];
  amenityIds?: number[];
  facilityIds?: number[];
}

export interface PropertyBasicInfo {
  hostId: number;
  fullAddress: string;
  propertyName: string;
  pricePerNight: number;
  numberOfBedrooms: number;
  numberOfBathrooms: number;
  description: string;
  propertyType: number; // 0: APARTMENT, 1: HOUSE, 2: HOTEL
  maxAdults: number;
  maxChildren: number;
  maxInfants: number;
  maxPets: number;
  locationId: number;
}


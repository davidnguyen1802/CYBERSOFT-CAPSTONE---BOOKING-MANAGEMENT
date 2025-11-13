export interface Amenity {
  id: number;
  iconUrl: string;
  amenityName: string;
  description: string;
}

export interface AmenityRequest {
  ids: number[];
  idProperty: number;
}


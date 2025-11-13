export interface Facility {
  id: number;
  iconUrl: string;
  facilityName: string;
  description: string;
}

export interface FacilityRequest {
  ids: number[];
  propertyId: number;
}


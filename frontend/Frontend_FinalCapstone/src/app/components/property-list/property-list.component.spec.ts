import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PropertyListComponent } from './property-list.component';
import { ActivatedRoute } from '@angular/router';
import { PropertyService } from '../../services/property.service';
import { LocationService } from '../../services/location.service';
import { CityService } from '../../services/city.service';
import { of } from 'rxjs';

describe('PropertyListComponent', () => {
  let component: PropertyListComponent;
  let fixture: ComponentFixture<PropertyListComponent>;
  let mockPropertyService: jasmine.SpyObj<PropertyService>;
  let mockLocationService: jasmine.SpyObj<LocationService>;
  let mockCityService: jasmine.SpyObj<CityService>;

  beforeEach(async () => {
    mockPropertyService = jasmine.createSpyObj('PropertyService', [
      'getPropertiesByType',
      'getByTypeAndCity',
      'getByTypeAndLocation'
    ]);
    mockLocationService = jasmine.createSpyObj('LocationService', ['getAll', 'getByCity']);
    mockCityService = jasmine.createSpyObj('CityService', ['getAll']);

    await TestBed.configureTestingModule({
      declarations: [PropertyListComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            params: of({ type: '0' })
          }
        },
        { provide: PropertyService, useValue: mockPropertyService },
        { provide: LocationService, useValue: mockLocationService },
        { provide: CityService, useValue: mockCityService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PropertyListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('onCityToggle', () => {
    it('should select city when checked is true', () => {
      const cityName = 'Ho Chi Minh';
      mockPropertyService.getByTypeAndCity.and.returnValue(of([]));
      mockLocationService.getByCity.and.returnValue(of([]));

      component.propertyType = '0';
      component.onCityToggle(cityName, true);

      expect(component.selectedCity).toBe(cityName);
      expect(component.selectedLocation).toBeNull();
      expect(mockPropertyService.getByTypeAndCity).toHaveBeenCalledWith('0', cityName);
      expect(mockLocationService.getByCity).toHaveBeenCalledWith(cityName);
    });

    it('should reset filters when unchecked', () => {
      component.selectedCity = 'Ho Chi Minh';
      component.allLocations = [
        { id: 1, locationName: 'District 1', cityName: 'Ho Chi Minh', country: 'Vietnam' }
      ];
      mockPropertyService.getPropertiesByType.and.returnValue(of([]));

      component.onCityToggle('Ho Chi Minh', false);

      expect(component.selectedCity).toBeNull();
      expect(component.selectedLocation).toBeNull();
      expect(component.showAllCities).toBeFalse();
      expect(component.showAllLocations).toBeFalse();
    });
  });

  describe('onLocationToggle', () => {
    beforeEach(() => {
      component.allLocations = [
        { id: 1, locationName: 'District 1', cityName: 'Ho Chi Minh', country: 'Vietnam' },
        { id: 2, locationName: 'District 2', cityName: 'Ho Chi Minh', country: 'Vietnam' }
      ];
      component.cities = [
        { id: 1, cityName: 'Ho Chi Minh' },
        { id: 2, cityName: 'Ha Noi' }
      ];
    });

    it('should select location and infer city when checked', () => {
      const locationName = 'District 1';
      mockPropertyService.getByTypeAndLocation.and.returnValue(of([]));

      component.propertyType = '0';
      component.onLocationToggle(locationName, true);

      expect(component.selectedLocation).toBe(locationName);
      expect(component.selectedCity).toBe('Ho Chi Minh');
      expect(mockPropertyService.getByTypeAndLocation).toHaveBeenCalledWith('0', locationName);
      expect(component.cities.length).toBe(1);
      expect(component.cities[0].cityName).toBe('Ho Chi Minh');
    });

    it('should reset filters when unchecked', () => {
      component.selectedLocation = 'District 1';
      component.selectedCity = 'Ho Chi Minh';
      mockCityService.getAll.and.returnValue(of(component.cities));
      mockPropertyService.getPropertiesByType.and.returnValue(of([]));

      component.onLocationToggle('District 1', false);

      expect(component.selectedLocation).toBeNull();
      expect(component.selectedCity).toBeNull();
      expect(component.showAllCities).toBeFalse();
      expect(component.showAllLocations).toBeFalse();
    });
  });

  describe('trackBy functions', () => {
    it('should track by city id', () => {
      const city = { id: 1, cityName: 'Test City' };
      expect(component.trackByCity(0, city)).toBe(1);
    });

    it('should track by location id', () => {
      const location = { id: 1, locationName: 'Test Location', cityName: 'Test City', country: 'Test' };
      expect(component.trackByLocation(0, location)).toBe(1);
    });

    it('should track by property id', () => {
      const property: any = { id: 123 };
      expect(component.trackByPropertyId(0, property)).toBe(123);
    });
  });
});

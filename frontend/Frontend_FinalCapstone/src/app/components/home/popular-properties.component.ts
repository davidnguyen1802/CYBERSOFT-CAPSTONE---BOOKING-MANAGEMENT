import { Component, OnInit } from '@angular/core';
import { PropertyService } from '../../services/property.service';

@Component({
  selector: 'app-popular-properties',
  templateUrl: './popular-properties.component.html',
  styleUrls: ['./popular-properties.component.scss']
})
export class PopularPropertiesComponent implements OnInit {
  hotels: any[] = [];
  apartments: any[] = [];
  houses: any[] = [];
  loading = false;

  constructor(private propertyService: PropertyService) { }

  ngOnInit(): void {
    this.loading = true;
    this.propertyService.getTop4ByType(2).subscribe({ next: (res:any) => this.hotels = res.data || [], error: (e:any) => console.error(e) });
    this.propertyService.getTop4ByType(0).subscribe({ next: (res:any) => this.apartments = res.data || [], error: (e:any) => console.error(e) });
    this.propertyService.getTop4ByType(1).subscribe({ next: (res:any) => this.houses = res.data || [], error: (e:any) => console.error(e), complete: () => this.loading = false });
  }
}

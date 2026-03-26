import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AmfiScheme {
  id: number;
  schemeCode: string;
  schemeName: string;
  fundHouse: string;
  instrumentType: string;
  active: boolean;
}

export interface AmfiNav {
  id: number;
  schemeCode: string;
  navDate: string;
  navValue: number;
}

@Injectable({
  providedIn: 'root'
})
export class AmfiService {
  private apiUrl = '/api/amfi';
  private syncApiUrl = '/api/v1/amfi/sync';

  constructor(private http: HttpClient) {}

  searchFunds(query: string, searchBy: string = 'schemeName'): Observable<AmfiScheme[]> {
    return this.http.get<AmfiScheme[]>(`${this.apiUrl}/search`, { params: { q: query, by: searchBy } });
  }

  getFundsByHouse(fundHouse: string, activeOnly: boolean = true): Observable<AmfiScheme[]> {
    return this.http.get<AmfiScheme[]>(`${this.apiUrl}/funds`, { params: { fundHouse, activeOnly: activeOnly.toString() } });
  }

  getNav(schemeCode: string, date: string): Observable<AmfiNav> {
    return this.http.get<AmfiNav>(`${this.apiUrl}/nav/${schemeCode}`, { params: { date } });
  }

  syncDailyNav(): Observable<string> {
    return this.http.post(`${this.syncApiUrl}/daily`, null, { responseType: 'text' });
  }
}


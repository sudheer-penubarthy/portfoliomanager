import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PortfolioSummary {
  userId: number;
  email: string;
  name: string;
  totalInvestedAmount: number;
  currentPortfolioValue: number;
  totalGain: number;
  gainPercentage: number;
  xirr: number;
  totalFunds: number;
  totalHoldings: number;
}

export interface TransactionDetails {
  id: number;
  schemeCode: string;
  schemeName: string;
  txnType: string;
  txnDate: string;
  units: number;
  amount: number;
  pricePerUnit: number;
  remarks?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PortfolioService {
  private apiUrl = '/api/portfolio';

  constructor(private http: HttpClient) {}

  getPortfolioSummary(userId: number): Observable<PortfolioSummary> {
    return this.http.get<PortfolioSummary>(`${this.apiUrl}/summary`, { params: { userId: userId.toString() } });
  }

  getPortfolioSummaryByEmail(email: string): Observable<PortfolioSummary> {
    return this.http.get<PortfolioSummary>(`${this.apiUrl}/summary/by-email`, { params: { email } });
  }

  getFundTransactions(userId: number, schemeCode: string): Observable<TransactionDetails[]> {
    return this.http.get<TransactionDetails[]>(
      `${this.apiUrl}/fund/${schemeCode}/transactions`,
      { params: { userId: userId.toString() } }
    );
  }

  getHoldings(userId: number): Observable<{ [key: string]: number }> {
    return this.http.get<{ [key: string]: number }>(`${this.apiUrl}/holdings`, { params: { userId: userId.toString() } });
  }
}


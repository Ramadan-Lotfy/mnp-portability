import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Page, PortingRequest, PortingStatus } from '../../models/porting-request.model';

@Injectable({ providedIn: 'root' })
export class PortingRequestService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/porting-requests';

  submit(phoneNumber: string): Observable<PortingRequest> {
    return this.http.post<PortingRequest>(this.baseUrl, { phoneNumber });
  }

  list(page: number, size: number, status?: PortingStatus): Observable<Page<PortingRequest>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Page<PortingRequest>>(this.baseUrl, { params });
  }

  accept(id: number): Observable<PortingRequest> {
    return this.http.post<PortingRequest>(`${this.baseUrl}/${id}/accept`, null);
  }

  reject(id: number): Observable<PortingRequest> {
    return this.http.post<PortingRequest>(`${this.baseUrl}/${id}/reject`, null);
  }
}

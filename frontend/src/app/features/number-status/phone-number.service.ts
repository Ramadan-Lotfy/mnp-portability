import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { PhoneNumberStatus } from '../../models/phone-number-status.model';

@Injectable({ providedIn: 'root' })
export class PhoneNumberService {
  private readonly http = inject(HttpClient);

  getStatus(phoneNumber: string): Observable<PhoneNumberStatus> {
    return this.http.get<PhoneNumberStatus>(`/api/v1/phone-numbers/${encodeURIComponent(phoneNumber)}`);
  }
}

import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { errorMessage } from '../../../core/error-message';
import { OperatorService } from '../../../core/operator.service';
import { PortingRequest } from '../../../models/porting-request.model';
import { PortingRequestService } from '../porting-request.service';

@Component({
  selector: 'app-request-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './request-form.html',
})
export class RequestForm {
  private readonly service = inject(PortingRequestService);
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly operator = inject(OperatorService);

  // Same format rule as the backend's @ValidEgyptianMobile: 11 digits starting with 01.
  protected readonly form = this.fb.group({
    phoneNumber: ['', [Validators.required, Validators.pattern(/^01\d{9}$/)]],
  });
  protected readonly phoneNumber = this.form.controls.phoneNumber;

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly created = signal<PortingRequest | null>(null);

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.error.set(null);
    this.created.set(null);

    this.service.submit(this.phoneNumber.value).subscribe({
      next: (request) => {
        this.created.set(request);
        this.form.reset();
        this.submitting.set(false);
      },
      error: (err: unknown) => {
        this.error.set(errorMessage(err));
        this.submitting.set(false);
      },
    });
  }
}

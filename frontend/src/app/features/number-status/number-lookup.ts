import { Component, effect, inject, signal, untracked } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { errorMessage } from '../../core/error-message';
import { OperatorService } from '../../core/operator.service';
import {
  NUMBER_STATUS_LABEL,
  NUMBER_STATUS_TONE,
  PhoneNumberStatus,
} from '../../models/phone-number-status.model';
import { PhoneNumberService } from './phone-number.service';

@Component({
  selector: 'app-number-lookup',
  imports: [ReactiveFormsModule],
  templateUrl: './number-lookup.html',
})
export class NumberLookup {
  private readonly service = inject(PhoneNumberService);
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly operator = inject(OperatorService);

  protected readonly label = NUMBER_STATUS_LABEL;
  protected readonly tone = NUMBER_STATUS_TONE;

  protected readonly form = this.fb.group({
    phoneNumber: ['', [Validators.required, Validators.pattern(/^01\d{9}$/)]],
  });
  protected readonly phoneNumber = this.form.controls.phoneNumber;

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly result = signal<PhoneNumberStatus | null>(null);

  constructor() {
    // A pending request is only visible to its donor and recipient, so an old result
    // can be wrong for a different operator. Clear it when the acting operator changes.
    effect(() => {
      this.operator.organization();
      untracked(() => {
        this.result.set(null);
        this.error.set(null);
      });
    });
  }

  protected lookup(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.result.set(null);

    this.service.getStatus(this.phoneNumber.value).subscribe({
      next: (status) => {
        this.result.set(status);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }
}

import { DatePipe } from '@angular/common';
import { Component, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { errorMessage } from '../../../core/error-message';
import { OperatorService } from '../../../core/operator.service';
import {
  PORTING_STATUSES,
  PageInfo,
  PortingRequest,
  PortingStatus,
  REQUEST_STATUS_TONE,
} from '../../../models/porting-request.model';
import { PortingRequestService } from '../porting-request.service';

@Component({
  selector: 'app-request-list',
  imports: [DatePipe, RouterLink],
  templateUrl: './request-list.html',
})
export class RequestList {
  private readonly service = inject(PortingRequestService);
  protected readonly operator = inject(OperatorService);

  protected readonly statuses = PORTING_STATUSES;
  protected readonly tone = REQUEST_STATUS_TONE;
  private readonly pageSize = 10;

  protected readonly requests = signal<PortingRequest[]>([]);
  protected readonly pageInfo = signal<PageInfo | null>(null);
  protected readonly pageIndex = signal(0);
  protected readonly statusFilter = signal<PortingStatus | ''>('');
  protected readonly loading = signal(false);
  protected readonly busyId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);

  private loadSubscription?: Subscription;

  constructor() {
    // Runs once on start, and again whenever the acting operator changes:
    // what a caller may see depends on who is asking.
    effect(() => {
      this.operator.organization();
      untracked(() => {
        this.pageIndex.set(0);
        this.load();
      });
    });
    inject(DestroyRef).onDestroy(() => this.loadSubscription?.unsubscribe());
  }

  protected load(): void {
    // Cancel an in-flight request so a slow, outdated response can never overwrite a newer one.
    this.loadSubscription?.unsubscribe();
    this.loading.set(true);
    this.error.set(null);

    this.loadSubscription = this.service
      .list(this.pageIndex(), this.pageSize, this.statusFilter() || undefined)
      .subscribe({
        next: (page) => {
          this.requests.set(page.content);
          this.pageInfo.set(page.page);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          this.requests.set([]);
          this.pageInfo.set(null);
          this.error.set(errorMessage(err));
          this.loading.set(false);
        },
      });
  }

  protected onStatusChange(event: Event): void {
    this.statusFilter.set((event.target as HTMLSelectElement).value as PortingStatus | '');
    this.pageIndex.set(0);
    this.load();
  }

  protected goTo(pageIndex: number): void {
    this.pageIndex.set(pageIndex);
    this.load();
  }

  /** Only the donor can decide, and only while the request is still pending. */
  protected canDecide(request: PortingRequest): boolean {
    return request.status === 'PENDING' && request.donor === this.operator.organization();
  }

  protected decide(request: PortingRequest, action: 'accept' | 'reject'): void {
    this.busyId.set(request.id);
    const call =
      action === 'accept' ? this.service.accept(request.id) : this.service.reject(request.id);

    call.subscribe({
      next: () => {
        this.busyId.set(null);
        this.load();
      },
      error: (err: unknown) => {
        this.busyId.set(null);
        // Reload first: a 409 means the request changed (for example it timed out), so show the new state.
        this.load();
        this.error.set(errorMessage(err));
      },
    });
  }
}

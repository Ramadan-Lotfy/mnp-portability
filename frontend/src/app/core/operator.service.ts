import { Injectable, signal } from '@angular/core';

export interface Operator {
  /** Value sent in the `organization` header. */
  readonly code: string;
  readonly name: string;
}

/** The three operators seeded by the backend's Flyway migration. */
export const OPERATORS: readonly Operator[] = [
  { code: 'vodafone', name: 'Vodafone' },
  { code: 'orange', name: 'Orange' },
  { code: 'etisalat', name: 'Etisalat' },
];

const STORAGE_KEY = 'mnp.organization';

/** Holds which operator the user is acting as. Mocked authentication: no login, just a selector. */
@Injectable({ providedIn: 'root' })
export class OperatorService {
  readonly operators = OPERATORS;

  private readonly current = signal(this.loadInitial());
  readonly organization = this.current.asReadonly();

  select(code: string): void {
    if (!this.isKnown(code)) {
      return;
    }
    this.current.set(code);
    try {
      localStorage.setItem(STORAGE_KEY, code);
    } catch {
      // Storage can be unavailable (private mode); the selection still works for this session.
    }
  }

  private loadInitial(): string {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved && this.isKnown(saved)) {
        return saved;
      }
    } catch {
      // fall through to the default
    }
    return OPERATORS[0].code;
  }

  private isKnown(code: string): boolean {
    return OPERATORS.some((operator) => operator.code === code);
  }
}

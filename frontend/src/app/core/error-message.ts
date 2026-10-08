import { HttpErrorResponse } from '@angular/common/http';

/** Turns a failed HTTP call into text for the user, preferring the message the backend sent. */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Cannot reach the server. Check that the backend is running.';
    }
    const message: unknown = error.error?.message;
    if (typeof message === 'string' && message.length > 0) {
      return message;
    }
    return `Request failed (HTTP ${error.status}).`;
  }
  return 'Unexpected error.';
}

import { Tone } from './porting-request.model';

export type NumberStatus = 'NOT_PORTED' | 'PORTED' | 'PORTING_PENDING';

export const NUMBER_STATUS_LABEL: Record<NumberStatus, string> = {
  NOT_PORTED: 'Not ported',
  PORTED: 'Ported',
  PORTING_PENDING: 'Porting pending',
};

export const NUMBER_STATUS_TONE: Record<NumberStatus, Tone> = {
  NOT_PORTED: 'neutral',
  PORTED: 'info',
  PORTING_PENDING: 'warn',
};

/** Mirrors the backend's PhoneNumberStatusResponse. */
export interface PhoneNumberStatus {
  phoneNumber: string;
  status: NumberStatus;
  currentOperator: string;
  originalOperator: string;
}

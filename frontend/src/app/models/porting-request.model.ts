export const PORTING_STATUSES = ['PENDING', 'ACCEPTED', 'REJECTED', 'CANCELED'] as const;
export type PortingStatus = (typeof PORTING_STATUSES)[number];

/** Colour group used by the status badge. */
export type Tone = 'neutral' | 'info' | 'warn' | 'success' | 'danger';

export const REQUEST_STATUS_TONE: Record<PortingStatus, Tone> = {
  PENDING: 'warn',
  ACCEPTED: 'success',
  REJECTED: 'danger',
  CANCELED: 'neutral',
};

/** Mirrors the backend's PortingRequestResponse. donor and recipient are organization codes. */
export interface PortingRequest {
  id: number;
  phoneNumber: string;
  donor: string;
  recipient: string;
  status: PortingStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PageInfo {
  size: number;
  number: number;
  totalElements: number;
  totalPages: number;
}

/** Shape of Spring Data's PagedModel. */
export interface Page<T> {
  content: T[];
  page: PageInfo;
}

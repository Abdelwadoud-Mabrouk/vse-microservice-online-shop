export type AuditStatus = 'SUCCESS' | 'FAILURE' | 'COMPENSATED' | 'BUSINESS_FAILED' | string;

export interface AuditEvent {
  id: string;
  correlationId: string;
  eventType: string;
  service: string;
  timestamp: string;
  payload: unknown;
  actor: string;
  statusCode: AuditStatus;
}

export interface AuditOrderSummary {
  correlationId: string;
  eventCount: number;
  lastEventType: string;
  lastStatusCode: AuditStatus;
  lastTimestamp: string;
}

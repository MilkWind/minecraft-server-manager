import { readStoredToken } from './storage';

export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  error?: {
    code: string;
    message: string;
  };
}

export class ApiError extends Error {
  public readonly status: number;
  public readonly code: string;

  constructor(
    message: string,
    status: number,
    code: string,
  ) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

const unauthorizedHandlers = new Set<() => void>();

export function registerUnauthorizedHandler(handler: () => void) {
  unauthorizedHandlers.add(handler);
  return () => unauthorizedHandlers.delete(handler);
}

export async function apiRequest<T>(url: string, init: RequestInit = {}): Promise<T> {
  const token = readStoredToken();
  const headers = new Headers(init.headers ?? {});

  if (!headers.has('Content-Type') && init.body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(url, {
    ...init,
    headers,
  });

  const payload = (await response.json()) as ApiEnvelope<T>;
  if (!response.ok || !payload.success) {
    if (response.status === 401) {
      for (const handler of unauthorizedHandlers) {
        handler();
      }
    }
    throw new ApiError(
      payload.error?.message ?? '请求失败',
      response.status,
      payload.error?.code ?? 'request_failed',
    );
  }

  return payload.data;
}

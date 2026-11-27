const API_BASE = import.meta.env.VITE_API_BASE_URL || '';

let traceListener = null;

export function setTraceListener(fn) {
  traceListener = fn;
}

export function getStoredAuth() {
  try {
    const raw = localStorage.getItem('kutuphane_auth');
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function saveAuth(auth) {
  if (auth) {
    localStorage.setItem('kutuphane_auth', JSON.stringify(auth));
  } else {
    localStorage.removeItem('kutuphane_auth');
  }
}

async function request(path, options = {}) {
  const auth = getStoredAuth();
  const headers = {
    Accept: 'application/json',
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...(auth?.token ? { Authorization: `Bearer ${auth.token}` } : {}),
    ...(options.headers || {}),
  };

  const method = (options.method || 'GET').toUpperCase();
  const startedAt = performance.now();

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...options,
      method,
      headers,
    });
  } catch {
    throw new Error('API Gateway (localhost:8080) bağlantısı kurulamadı. Servislerin çalıştığından emin olun.');
  }

  const durationMs = Math.round(performance.now() - startedAt);
  const correlationId = response.headers.get('X-Correlation-Id') || '-';

  if (traceListener) {
    traceListener({
      id: `${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
      method,
      path,
      status: response.status,
      correlationId,
      durationMs,
      time: new Date().toLocaleTimeString('tr-TR'),
    });
  }

  let data = null;
  const contentType = response.headers.get('content-type') || '';
  if (response.status !== 204 && contentType.includes('application/json')) {
    data = await response.json();
  }

  if (!response.ok) {
    let message = data?.message || `İstek başarısız oldu (HTTP ${response.status})`;
    if (data?.errors && typeof data.errors === 'object') {
      const details = Object.values(data.errors).join(' • ');
      if (details) message = details;
    }
    const err = new Error(message);
    err.status = response.status;
    err.correlationId = correlationId;
    throw err;
  }

  return {
    data,
    status: response.status,
    correlationId,
    headers: response.headers,
  };
}

export const api = {
  // Auth
  login: (username, password) =>
    request('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),

  me: () => request('/api/auth/me'),

  // Books
  getBooks: async ({ title = '', author = '', category = '', page = 0, size = 8 } = {}) => {
    const params = new URLSearchParams();
    if (title.trim()) params.set('title', title.trim());
    if (author.trim()) params.set('author', author.trim());
    if (category.trim()) params.set('category', category.trim());
    params.set('page', String(page));
    params.set('size', String(size));
    const res = await request(`/api/books?${params.toString()}`);
    return {
      items: res.data || [],
      totalCount: Number(res.headers.get('X-Total-Count') ?? (res.data?.length || 0)),
      totalPages: Math.max(1, Number(res.headers.get('X-Total-Pages') ?? 1)),
      correlationId: res.correlationId,
    };
  },

  createBook: (payload) =>
    request('/api/books', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  updateBook: (id, payload) =>
    request(`/api/books/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),

  deleteBook: (id) =>
    request(`/api/books/${id}`, {
      method: 'DELETE',
    }),

  // Members
  getMembers: () => request('/api/members'),

  getMemberById: (id) => request(`/api/members/${id}`),

  createMember: (payload) =>
    request('/api/members', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  updateMember: (id, payload) =>
    request(`/api/members/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),

  updateMemberStatus: (id, status) =>
    request(`/api/members/${id}/status?status=${encodeURIComponent(status)}`, {
      method: 'PUT',
    }),

  deleteMember: (id) =>
    request(`/api/members/${id}`, {
      method: 'DELETE',
    }),

  // Borrowings
  getBorrowings: ({ status = '', memberId = '' } = {}) => {
    const params = new URLSearchParams();
    if (status) params.set('status', status);
    if (memberId !== '' && memberId !== null && memberId !== undefined) {
      params.set('memberId', String(memberId));
    }
    const qs = params.toString();
    return request(`/api/borrowings${qs ? `?${qs}` : ''}`);
  },

  borrowBook: ({ bookId, memberId, durationDays }) => {
    const params = new URLSearchParams();
    params.set('bookId', String(bookId));
    if (memberId) params.set('memberId', String(memberId));
    if (durationDays) params.set('durationDays', String(durationDays));
    return request(`/api/borrowings?${params.toString()}`, {
      method: 'POST',
    });
  },

  returnBook: (id) =>
    request(`/api/borrowings/${id}/return`, {
      method: 'PUT',
    }),

  renewBorrowing: (id) =>
    request(`/api/borrowings/${id}/renew`, {
      method: 'PUT',
    }),
};

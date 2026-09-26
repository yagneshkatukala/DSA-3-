const BASE_URL = 'http://localhost:8080/api';

async function getJson(path, params = {}) {
  const url = new URL(BASE_URL + path);
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, value);
    }
  });
  const res = await fetch(url.toString());
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    const err = new Error(body.error || `Request failed: ${res.status}`);
    err.status = res.status;
    throw err;
  }
  return res.json();
}

export function search({ query, language = 'all', algorithm = 'auto', mode = '', searchMode = 'exact' }) {
  return getJson('/search', { query, language, algorithm, mode, searchMode });
}

export function getFlow({ query, language = 'all', algorithm = 'dinic', demand = 1 }) {
  return getJson('/flow', { query, language, algorithm, demand });
}

export function getSuggestions(term, limit = 5) {
  return getJson('/suggest', { term, limit });
}

export function getStatistics() {
  return getJson('/statistics');
}

export function getLanguages() {
  return getJson('/languages');
}

export function getDocument(id) {
  return getJson(`/documents/${encodeURIComponent(id)}`);
}

export function getPerformance({ query, language = 'all' }) {
  return getJson('/performance', { query, language });
}

export async function rebuildIndex() {
  const res = await fetch(`${BASE_URL}/index/rebuild`);
  return res.json();
}

import { environment } from '../../environments/environment';

/**
 * Returns the api base URL without a trailing slash.
 */
export function getBaseUrl(): string {
  return (environment.apiBaseUrl || 'http://localhost:8080').replace(/\/$/, '');
}

export default getBaseUrl;

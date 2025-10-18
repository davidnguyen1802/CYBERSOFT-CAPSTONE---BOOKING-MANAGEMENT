export interface AuthData {
  message: string;
  token: string;
  refresh_token: string;
  tokenType: string;
  id: number;
  username: string;
  roles: string[];
}

export interface LoginResponse {
  message: string;
  status: string;
  data: AuthData;
}

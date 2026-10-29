export interface UserProfile {
  id: string;
  name: string;
  email?: string | null;
  bio?: string | null;
  avatarMediaId?: string | null;
  createdAt?: string;
}

export interface Session {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: UserProfile;
}

export interface Credentials {
  email: string;
  password: string;
}

export interface Registration extends Credentials {
  name: string;
}

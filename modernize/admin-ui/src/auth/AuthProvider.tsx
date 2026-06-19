import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useReducer,
  useRef,
  type ReactNode,
} from "react";
import { ApiError, apiClient } from "@/api/client";
import { authApi } from "@/api/endpoints";

/**
 * Auth state. The access token lives in a ref (not React state) so
 * the closure in {@link apiClient} always reads the latest value
 * without forcing every component to re-render on token change.
 * Components only re-render when `status` or `user` change.
 */
export type AuthStatus = "loading" | "authenticated" | "unauthenticated";

export interface AuthUser {
  username: string;
  roles: string[];
}

export interface AuthState {
  status: AuthStatus;
  user: AuthUser | null;
  error: string | null;
}

type Action =
  | { type: "LOGIN_SUCCESS"; user: AuthUser }
  | { type: "LOGIN_FAILURE"; error: string }
  | { type: "LOGOUT" }
  | { type: "CLEAR_ERROR" };

const initialState: AuthState = {
  status: "loading",
  user: null,
  error: null,
};

function reducer(state: AuthState, action: Action): AuthState {
  switch (action.type) {
    case "LOGIN_SUCCESS":
      return { status: "authenticated", user: action.user, error: null };
    case "LOGIN_FAILURE":
      return { status: "unauthenticated", user: null, error: action.error };
    case "LOGOUT":
      return { status: "unauthenticated", user: null, error: null };
    case "CLEAR_ERROR":
      return { ...state, error: null };
  }
}

export interface AuthContextValue extends AuthState {
  login: (username: string, password: string) => Promise<boolean>;
  logout: () => Promise<void>;
  clearError: () => void;
  /**
   * The current access token, for {@link apiClient}. Exposed
   * separately so consumers can wire the client once at mount.
   */
  getAccessToken: () => string | null;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, initialState);
  const accessTokenRef = useRef<string | null>(null);

  // Wire the client to read the access token from our ref, and to
  // notify us on auth failure (refresh failed -> we need to log out).
  useEffect(() => {
    apiClient.setAccessTokenGetter(() => accessTokenRef.current);
    apiClient.setAuthFailureHandler(() => {
      accessTokenRef.current = null;
      dispatch({ type: "LOGOUT" });
    });
  }, []);

  // Listen for refresh-completed events from the client. The
  // client dispatches a CustomEvent when /auth/refresh succeeds so
  // we don't have to thread a callback through its constructor
  // (which would create a circular import).
  useEffect(() => {
    function onTokenRefreshed(evt: Event) {
      const detail = (evt as CustomEvent<string>).detail;
      accessTokenRef.current = detail;
    }
    window.addEventListener("auth:token-refreshed", onTokenRefreshed);
    return () => window.removeEventListener("auth:token-refreshed", onTokenRefreshed);
  }, []);

  /**
   * On mount, attempt a silent refresh. If the cookie is still
   * valid, the user lands on the page already logged in; if not,
   * they go to /login.
   */
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const resp = await fetch("/api/auth/refresh", {
          method: "POST",
          credentials: "same-origin",
        });
        if (cancelled) return;
        if (resp.ok) {
          const body = (await resp.json()) as { token: string };
          accessTokenRef.current = body.token;
          // We don't have a full user object on refresh yet. Use
          // a placeholder; the next /sso-admin/whoami call (or a
          // page that requires a role) will hydrate it. For now
          // we trust the token's presence as "logged in".
          dispatch({
            type: "LOGIN_SUCCESS",
            user: { username: "(you)", roles: [] },
          });
        } else {
          dispatch({ type: "LOGOUT" });
        }
      } catch {
        if (!cancelled) dispatch({ type: "LOGOUT" });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (username: string, password: string): Promise<boolean> => {
    try {
      const resp = await authApi.login({ username, password });
      accessTokenRef.current = resp.token;
      // For now, hydrate user with what we have. A real /whoami
      // endpoint would be cleaner; the backend's user info is
      // embedded in the JWT but decoding it client-side would
      // require knowing the (server-side) structure.
      dispatch({
        type: "LOGIN_SUCCESS",
        user: { username, roles: [] },
      });
      return true;
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : "Login failed";
      dispatch({ type: "LOGIN_FAILURE", error: msg });
      return false;
    }
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // ignore — we're logging out anyway
    }
    accessTokenRef.current = null;
    dispatch({ type: "LOGOUT" });
  }, []);

  const clearError = useCallback(() => dispatch({ type: "CLEAR_ERROR" }), []);

  const value = useMemo<AuthContextValue>(
    () => ({
      ...state,
      login,
      logout,
      clearError,
      getAccessToken: () => accessTokenRef.current,
    }),
    [state, login, logout, clearError],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

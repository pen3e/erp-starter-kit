import { AxiosError } from "axios";
import type { ApiError } from "@/types/api";

/** Narrow an unknown thrown value to the backend's ApiError contract, if present. */
export function asApiError(error: unknown): ApiError | null {
  if (
    error instanceof AxiosError &&
    error.response?.data &&
    typeof error.response.data === "object"
  ) {
    return error.response.data as ApiError;
  }
  return null;
}

/** A safe, human-readable message for toasts. Never leaks stack traces. */
export function errorMessage(error: unknown, fallback = "Something went wrong"): string {
  const apiError = asApiError(error);
  if (apiError?.message) return apiError.message;
  if (error instanceof AxiosError) {
    if (error.code === "ERR_NETWORK") return "Cannot reach the server.";
    if (error.message) return error.message;
  }
  return fallback;
}

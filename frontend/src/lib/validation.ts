import { z } from "zod";

/**
 * Client mirror of the backend StrongPassword policy (12–200 chars, with upper, lower, digit
 * and symbol). Client-side validation is for UX only; the server enforces the real rule.
 */
export const strongPassword = z
  .string()
  .min(12, "At least 12 characters")
  .max(200, "At most 200 characters")
  .refine((v) => /[A-Z]/.test(v), "Add an upper-case letter")
  .refine((v) => /[a-z]/.test(v), "Add a lower-case letter")
  .refine((v) => /[0-9]/.test(v), "Add a digit")
  .refine((v) => /[^A-Za-z0-9]/.test(v), "Add a symbol");

export const emailSchema = z.string().min(1, "Email is required").email("Enter a valid email");

export const phoneSchema = z
  .string()
  .max(30, "At most 30 characters")
  .regex(/^[+0-9 ().-]*$/, "Phone contains invalid characters")
  .optional()
  .or(z.literal(""));

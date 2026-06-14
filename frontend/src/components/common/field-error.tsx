interface FieldErrorProps {
  message?: string;
}

/** Inline, accessible validation message rendered beneath a form control. */
export function FieldError({ message }: FieldErrorProps) {
  if (!message) return null;
  return (
    <p role="alert" className="text-sm font-medium text-destructive">
      {message}
    </p>
  );
}

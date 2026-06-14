import { cn } from "@/lib/utils";

export interface CheckboxOption {
  value: string;
  label: string;
  description?: string | null;
}

interface CheckboxListProps {
  options: CheckboxOption[];
  selected: string[];
  onChange: (next: string[]) => void;
  disabled?: boolean;
  columns?: 1 | 2;
  emptyLabel?: string;
}

/**
 * Accessible multi-select built on native checkboxes (no extra dependency). Used to assign
 * roles to a user and permissions to a role.
 */
export function CheckboxList({
  options,
  selected,
  onChange,
  disabled = false,
  columns = 1,
  emptyLabel = "Nothing to select.",
}: CheckboxListProps) {
  const selectedSet = new Set(selected);

  function toggle(value: string) {
    const next = new Set(selectedSet);
    if (next.has(value)) next.delete(value);
    else next.add(value);
    onChange([...next]);
  }

  if (options.length === 0) {
    return <p className="text-sm text-muted-foreground">{emptyLabel}</p>;
  }

  return (
    <div
      className={cn(
        "max-h-64 overflow-y-auto rounded-md border p-3",
        columns === 2 ? "grid grid-cols-1 gap-2 sm:grid-cols-2" : "space-y-2",
      )}
    >
      {options.map((option) => {
        const checked = selectedSet.has(option.value);
        return (
          <label
            key={option.value}
            className={cn(
              "flex cursor-pointer items-start gap-2 rounded-md p-2 text-sm hover:bg-accent",
              disabled && "cursor-not-allowed opacity-60",
            )}
          >
            <input
              type="checkbox"
              className="mt-0.5 h-4 w-4 rounded border-input accent-primary"
              checked={checked}
              disabled={disabled}
              onChange={() => toggle(option.value)}
            />
            <span className="leading-tight">
              <span className="font-medium">{option.label}</span>
              {option.description ? (
                <span className="block text-xs text-muted-foreground">{option.description}</span>
              ) : null}
            </span>
          </label>
        );
      })}
    </div>
  );
}

import { AbstractControl, FormGroup } from '@angular/forms';

/**
 * Maps backend `fieldErrors` onto the matching Reactive Form controls ({ server: message }).
 * Returns the messages that matched no control, to show in the form alert.
 */
export function applyFieldErrors(form: FormGroup, fieldErrors: Record<string, string> | undefined | null): string[] {
  const leftover: string[] = [];
  for (const [field, message] of Object.entries(fieldErrors ?? {})) {
    const ctrl: AbstractControl | null = form.get(field);
    if (ctrl) {
      ctrl.setErrors({ ...(ctrl.errors ?? {}), server: message });
      ctrl.markAsTouched();
    } else {
      leftover.push(`${field}: ${message}`);
    }
  }
  return leftover;
}

/** First readable message of a control, or ''. */
export function controlError(ctrl: AbstractControl | null | undefined): string {
  if (!ctrl || !ctrl.errors || !(ctrl.touched || ctrl.dirty)) return '';
  const e = ctrl.errors;
  for (const key of Object.keys(e)) {
    const v = e[key];
    if (key === 'server') return String(v);
    if (v && typeof v === 'object' && 'message' in v) return String((v as { message: string }).message);
    if (key === 'required') return 'This field is required.';
  }
  return 'Invalid value.';
}

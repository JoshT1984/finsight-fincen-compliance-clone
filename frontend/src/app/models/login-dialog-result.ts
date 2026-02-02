export type LoginDialogResult =
  | { action: 'password'; email: string; password: string }
  | { action: 'google' }
  | { action: 'github' }
  | undefined;

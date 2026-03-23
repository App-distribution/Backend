export const THEME_STORAGE_KEY = "appdistribution-admin-theme";

export type ThemePreference = "light" | "dark" | "system";
export type ResolvedTheme = Exclude<ThemePreference, "system">;

export function isThemePreference(value: string | null): value is ThemePreference {
  return value === "light" || value === "dark" || value === "system";
}

export const themeInitScript = `
  (() => {
    const storageKey = "${THEME_STORAGE_KEY}";
    const mediaQuery = "(prefers-color-scheme: dark)";

    const resolveTheme = (theme) => {
      if (theme === "dark" || theme === "light") {
        return theme;
      }

      return window.matchMedia(mediaQuery).matches ? "dark" : "light";
    };

    try {
      const storedTheme = localStorage.getItem(storageKey);
      const theme = storedTheme === "light" || storedTheme === "dark" || storedTheme === "system"
        ? storedTheme
        : "system";
      const resolvedTheme = resolveTheme(theme);
      const root = document.documentElement;

      root.dataset.themePreference = theme;
      root.dataset.theme = resolvedTheme;
      root.classList.toggle("dark", resolvedTheme === "dark");
      root.style.colorScheme = resolvedTheme;
    } catch {
      const resolvedTheme = resolveTheme("system");
      const root = document.documentElement;

      root.dataset.themePreference = "system";
      root.dataset.theme = resolvedTheme;
      root.classList.toggle("dark", resolvedTheme === "dark");
      root.style.colorScheme = resolvedTheme;
    }
  })();
`;

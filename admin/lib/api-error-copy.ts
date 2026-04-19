export const API_ERROR_MESSAGES = {
  authFailed: "Не удалось выполнить авторизацию.",
  badRequest: "Проверьте корректность введённых данных.",
  conflict: "Операцию нельзя выполнить из-за конфликта данных.",
  invalidOtp: "Неверный или просроченный OTP-код.",
  invalidToken: "Сессия истекла или токен недействителен. Войдите снова.",
  network: "Не удалось связаться с backend.",
  notFound: "Запрошенный ресурс не найден.",
  requestFailed: "Ошибка запроса.",
  refreshFailed: "Не удалось обновить сессию.",
  server: "Backend временно недоступен. Повторите попытку.",
  sessionExpired: "Сессия истекла. Войдите снова.",
  tokenExpiredPublic: "Токен недействителен или истёк.",
  workspaceNotFound: "Рабочее пространство не найдено.",
  forbidden: "У вас нет прав для этого действия.",
} as const;

export const API_TOAST_TITLES = {
  authError: "Ошибка авторизации",
  authSession: "Требуется повторный вход",
  badRequest: "Некорректный запрос",
  conflict: "Конфликт данных",
  forbidden: "Недостаточно прав",
  network: "Ошибка сети",
  notFound: "Не найдено",
  server: "Ошибка сервера",
} as const;

export const API_ERROR_MESSAGE_BY_CODE: Record<string, string> = {
  BAD_REQUEST: API_ERROR_MESSAGES.badRequest,
  CONFLICT: API_ERROR_MESSAGES.conflict,
  FORBIDDEN: API_ERROR_MESSAGES.forbidden,
  INVALID_OTP: API_ERROR_MESSAGES.invalidOtp,
  INVALID_TOKEN: API_ERROR_MESSAGES.invalidToken,
  NOT_FOUND: API_ERROR_MESSAGES.notFound,
  UNAUTHORIZED: API_ERROR_MESSAGES.authFailed,
  WORKSPACE_NOT_FOUND: API_ERROR_MESSAGES.workspaceNotFound,
};

export const API_ERROR_MESSAGE_BY_BACKEND_MESSAGE: Record<string, string> = {
  "failed to refresh session": API_ERROR_MESSAGES.refreshFailed,
  "invalid or expired otp": API_ERROR_MESSAGES.invalidOtp,
  "request failed": API_ERROR_MESSAGES.requestFailed,
  "token is not valid or expired": API_ERROR_MESSAGES.invalidToken,
};

export const API_ERROR_MESSAGE_BY_STATUS = {
  0: API_ERROR_MESSAGES.network,
  400: API_ERROR_MESSAGES.badRequest,
  403: API_ERROR_MESSAGES.forbidden,
  404: API_ERROR_MESSAGES.notFound,
  409: API_ERROR_MESSAGES.conflict,
} as const;

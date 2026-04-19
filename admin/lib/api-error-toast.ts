import { toast } from "react-toastify";
import { API_ERROR_MESSAGES, API_TOAST_TITLES } from "@/lib/api-error-copy";

export function showUnauthorizedToast(message: string) {
  toast.warning(`${API_TOAST_TITLES.authSession}\n${message}`, {
    toastId: "auth-session",
  });
}

function showAuthErrorToast(message: string) {
  toast.warning(`${API_TOAST_TITLES.authError}\n${message}`, {
    toastId: "auth-error",
  });
}

function showBadRequestToast(message: string) {
  toast.warning(`${API_TOAST_TITLES.badRequest}\n${message}`, {
    toastId: "bad-request",
  });
}

function showForbiddenToast(message: string) {
  toast.warning(`${API_TOAST_TITLES.forbidden}\n${message}`, {
    toastId: "forbidden-request",
  });
}

function showNotFoundToast(message: string) {
  toast.warning(`${API_TOAST_TITLES.notFound}\n${message}`, {
    toastId: "not-found",
  });
}

function showConflictToast(message: string) {
  toast.warning(`${API_TOAST_TITLES.conflict}\n${message}`, {
    toastId: "conflict",
  });
}

function showServerErrorToast(message: string) {
  toast.error(`${API_TOAST_TITLES.server}\n${message}`, {
    autoClose: 7000,
    toastId: "server-error",
  });
}

function showNetworkErrorToast(message: string) {
  toast.error(`${API_TOAST_TITLES.network}\n${message}`, {
    autoClose: 7000,
    toastId: "network-error",
  });
}

export function notifyApiError(status: number, message: string, authenticated: boolean) {
  if (status === 400) {
    showBadRequestToast(message || API_ERROR_MESSAGES.badRequest);
    return;
  }
  if (status === 401) {
    if (authenticated) {
      showUnauthorizedToast(message || API_ERROR_MESSAGES.invalidToken);
    } else {
      showAuthErrorToast(message || API_ERROR_MESSAGES.authFailed);
    }
    return;
  }
  if (status === 403) {
    showForbiddenToast(message || API_ERROR_MESSAGES.forbidden);
    return;
  }
  if (status === 404) {
    showNotFoundToast(message || API_ERROR_MESSAGES.notFound);
    return;
  }
  if (status === 409) {
    showConflictToast(message || API_ERROR_MESSAGES.conflict);
    return;
  }
  if (status === 0) {
    showNetworkErrorToast(message || API_ERROR_MESSAGES.network);
    return;
  }
  if (status >= 500) {
    showServerErrorToast(message || API_ERROR_MESSAGES.server);
  }
}

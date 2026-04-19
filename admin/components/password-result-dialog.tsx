"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";

interface PasswordResultDialogProps {
  password: string;
  onClose: () => void;
}

export function PasswordResultDialog({ password, onClose }: PasswordResultDialogProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    await navigator.clipboard.writeText(password);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <Card className="w-full max-w-md mx-4">
        <CardContent className="space-y-4 p-6">
          <h2 className="text-lg font-semibold">User created</h2>
          <p className="text-sm text-[var(--text-muted)]">
            Save this password — it will not be shown again.
          </p>
          <div className="rounded-xl bg-[var(--surface-muted)] px-4 py-3 font-mono text-lg tracking-widest">
            {password}
          </div>
          <div className="flex gap-3">
            <Button variant="secondary" className="flex-1" onClick={handleCopy}>
              {copied ? "Copied!" : "Copy"}
            </Button>
            <Button className="flex-1" onClick={onClose}>
              Close
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

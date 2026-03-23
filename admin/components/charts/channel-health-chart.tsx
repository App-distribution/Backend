"use client";

import { Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { Build } from "@/lib/types";

export function ChannelHealthChart({ builds }: { builds: Build[] }) {
  const data = Object.entries(
    builds.reduce<Record<string, { healthy: number; flagged: number }>>((acc, build) => {
      acc[build.channel] ??= { healthy: 0, flagged: 0 };
      if (build.status === "ACTIVE") acc[build.channel].healthy += 1;
      else acc[build.channel].flagged += 1;
      return acc;
    }, {}),
  ).map(([channel, counters]) => ({
    channel: channel.toLowerCase(),
    ...counters,
  }));

  return (
    <div className="h-72 w-full">
      <ResponsiveContainer>
        <BarChart data={data}>
          <XAxis dataKey="channel" tickLine={false} axisLine={false} />
          <YAxis tickLine={false} axisLine={false} />
          <Tooltip />
          <Bar dataKey="healthy" stackId="a" fill="#0f766e" radius={[8, 8, 0, 0]} />
          <Bar dataKey="flagged" stackId="a" fill="#d97706" radius={[8, 8, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

"use client";

import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";
import type { Build } from "@/lib/types";

const COLORS = ["#0f766e", "#0891b2", "#64748b", "#d97706"];

export function EnvironmentDistributionChart({ builds }: { builds: Build[] }) {
  const data = Object.entries(
    builds.reduce<Record<string, number>>((acc, build) => {
      acc[build.environment] = (acc[build.environment] ?? 0) + 1;
      return acc;
    }, {}),
  ).map(([environment, value]) => ({
    name: environment.toLowerCase(),
    value,
  }));

  return (
    <div className="h-72 w-full">
      <ResponsiveContainer>
        <PieChart>
          <Pie data={data} dataKey="value" nameKey="name" innerRadius={70} outerRadius={100} paddingAngle={4}>
            {data.map((entry, index) => (
              <Cell key={entry.name} fill={COLORS[index % COLORS.length]} />
            ))}
          </Pie>
          <Tooltip />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}

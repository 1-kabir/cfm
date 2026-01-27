import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
	title: "Cursor for Minecraft",
	description: "Cursor for Minecraft Application",
};

export default function RootLayout({
	children,
}: Readonly<{
	children: React.ReactNode;
}>) {
	return (
		<html lang="en">
			<head>
				<link rel="manifest" href="/manifest.json" />
			</head>
			<body className="antialiased">{children}</body>
		</html>
	);
}

import type { NextConfig } from 'next';
const config: NextConfig = { output: "standalone", async rewrites(){return [{source:'/api/:path*',destination:`${process.env.BACKEND_ORIGIN ?? 'http://localhost:8081'}/api/:path*`}]} };
export default config;

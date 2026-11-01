import {defineConfig} from '@playwright/test';
export default defineConfig({testDir:'./tests-operations',workers:1,use:{actionTimeout:10000,baseURL:'http://localhost:3100',trace:'off'},timeout:240000});

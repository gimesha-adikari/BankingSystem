// Compatibility import for older callers. The application has one configured
// Axios instance so base URLs and error/token handling cannot drift by page.
export { default } from "@/api/axios";
export type { NormalizedError } from "@/api/axios";

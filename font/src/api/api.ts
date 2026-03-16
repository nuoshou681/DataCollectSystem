import request from '@/api/axiosConfig'
import type { UserInfo } from '@/types/entity'
import type { ApiResponse  } from '@/types/apiResponse'

export async function login(email: string, password: string) {
  return request.post<ApiResponse<UserInfo>>('/login', { email, password })
}

export async function register(email: string, password: string) {
  return request.post<ApiResponse<UserInfo>>('/register', { email, password })
}

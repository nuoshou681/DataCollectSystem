import axios from 'axios'
import { errorCodeMap } from '@/types/errorCode'
import type { AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const requset = axios.create({
  baseURL: 'http://localhost:8080', // 根据实际后端地址调整
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})
// 请求拦截器
requset.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
// 响应拦截器
requset.interceptors.response.use(
  (response: AxiosResponse) => {
    console.log(response);
    const res = response.data
    if (res.code === 0) return res
    // 优先使用后端 msg，否则使用前端配置信息
    const msg = res.msg || errorCodeMap[res.code]
    ElMessage.error(msg)
    // 401特殊处理
    if (res.code === 401) {
      router.push('/login')
    }
    return null
  },
  (error) => {
    ElMessage.error('网络错误')
    return Promise.reject(error)
  },
)

export default requset

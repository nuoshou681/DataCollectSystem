export const errorCodeMap: Record<number,string> = {
  0:   '成功',
  400: '参数错误',
  401: '请先登陆',
  403: '无权限访问',
  404: '资源不存在',
  500: '服务器异常'
}
// 导出类型
export type ErrorCode = keyof typeof errorCodeMap;

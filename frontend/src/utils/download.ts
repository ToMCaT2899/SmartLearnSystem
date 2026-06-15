import axios from 'axios'

export function downloadBlob(url: string, filename: string) {
  const token = localStorage.getItem('token')
  return axios.get(url, {
    baseURL: '/api/v1',
    responseType: 'blob',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  }).then(response => {
    const blobUrl = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(blobUrl)
  })
}

import { api } from '../../shared/api/client'

export type AmbientCategory = {
  id: string
  label: string
}

export type AmbientSound = {
  categoryId: string
  id: number
  name: string
  author: string
  license: string
  previewUrl: string
}

export async function getAmbientCategories(signal?: AbortSignal) {
  return (await api.get<AmbientCategory[]>('/ambient/categories', { signal })).data
}

/** `exclude` is the sound already playing, so a new pick differs from it when possible. */
export async function getAmbientSound(categoryId: string, exclude?: number) {
  return (await api.get<AmbientSound>(`/ambient/categories/${encodeURIComponent(categoryId)}/sound`, { params: { exclude } })).data
}

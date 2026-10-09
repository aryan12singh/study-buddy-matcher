/** What a section needs from a loaded resource: the data plus its loading and error state. */
export type Loaded<T> = { data?: T; loading: boolean; error?: string; reload: (purge?: boolean) => void }

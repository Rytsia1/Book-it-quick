import { ref } from 'vue'
import request from '@/utils/request'

/**
 * Shared module-level state so the categories list is cached
 * across the whole single-page app and we don't re-fetch it
 * on every dialog open.
 */
const _categories      = ref([])            // Category[] (from /api/categories)
const loading          = ref(false)
const lastUserId       = ref(null)

/**
 * Public, named export of the categories ref. Re-exported as a
 * getter-backed object so that `import { categories }` works in any
 * view while still keeping the same underlying ref shared everywhere.
 */
export const categories = _categories

/**
 * The default "system" categories that every new user can use out of the box.
 * They are returned by the backend too, but having them here as a fallback
 * makes the UI snappy before the network call completes.
 */
export const DEFAULT_CATEGORIES = {
  1: ['Salary', 'Bonus', 'Freelance', 'Investment', 'Loan', 'Debt', 'Other Income'],
  0: ['Food', 'Transport', 'Utilities', 'Shopping', 'Entertainment', 'Health', 'Education', 'Rent', 'Other Expense'],
}

/**
 * Load all categories for a user from the backend, then merge with
 * the default system categories. The result is cached in `categories`.
 */
export const fetchCategories = async (userId, force = false) => {
  if (!userId) return categories.value
  if (!force && lastUserId.value === userId && categories.value.length > 0) {
    return categories.value
  }
  try {
    loading.value = true
    const data = await request.get('/categories', { params: { userId } })
    categories.value = Array.isArray(data) ? data : []
    lastUserId.value = userId
  } catch (e) {
    // Non-fatal: keep whatever we already have.
    console.warn('Failed to load categories, falling back to defaults', e)
  } finally {
    loading.value = false
  }
  return categories.value
}

/**
 * Return a list of category names for the given type (0 = expense, 1 = income).
 * Combines the default system categories with the user's custom ones.
 * Importantly, this also EXCLUDES any name that is reserved for the
 * *opposite* type, so that the dropdown never offers a category that
 * would later be rejected by the backend.
 */
export const getCategoryNamesForType = (type) => {
  if (type == null) return []
  const other        = type === 1 ? 0 : 1
  const fixed        = DEFAULT_CATEGORIES[type] ?? []
  const otherFixed   = new Set(
    (DEFAULT_CATEGORIES[other] ?? []).map(n => n.toLowerCase())
  )
  const custom       = categories.value
    .filter(c => c.type === type && c.name)
    .map(c => c.name)
  // Custom categories of the *opposite* type should also be excluded,
  // because the backend will reject them.
  const otherCustom  = new Set(
    categories.value
      .filter(c => c.type === other && c.name)
      .map(c => c.name.toLowerCase())
  )
  // Use a Set to deduplicate (case-insensitive comparison).
  const seen         = new Set()
  const merged       = []
  for (const name of [...fixed, ...custom]) {
    const lower = name.toLowerCase()
    if (otherFixed.has(lower))  continue
    if (otherCustom.has(lower)) continue
    if (seen.has(lower))        continue
    seen.add(lower)
    merged.push(name)
  }
  return merged
}

/**
 * Create a new custom category through the API and update the local cache.
 * Returns the created Category on success, or null on failure.
 */
export const createCategory = async (userId, type, name) => {
  const trimmed = (name || '').trim()
  if (!trimmed) return null
  try {
    const created = await request.post('/categories', {
      userId,
      type,
      name: trimmed,
    })
    // Refresh the cache so the new category shows up everywhere.
    await fetchCategories(userId, true)
    return created
  } catch (e) {
    console.error('Failed to create category', e)
    return null
  }
}

/**
 * Update an existing custom category through the API and refresh the cache.
 */
export const updateCategory = async (userId, category) => {
  if (!category || !category.id) return null
  try {
    const updated = await request.put(`/categories/${category.id}`, {
      ...category,
      userId,
    })
    await fetchCategories(userId, true)
    return updated
  } catch (e) {
    console.error('Failed to update category', e)
    return null
  }
}

/**
 * Delete a custom category through the API and refresh the cache.
 * Note: historical bills that used the category name are kept untouched
 * (the bill's `category` is a string column, not a foreign key).
 */
export const deleteCategory = async (userId, categoryId) => {
  try {
    await request.delete(`/categories/${categoryId}`, { params: { userId } })
    await fetchCategories(userId, true)
    return true
  } catch (e) {
    console.error('Failed to delete category', e)
    return false
  }
}

export const useCategories = () => ({
  categories,
  loading,
  fetchCategories,
  getCategoryNamesForType,
  createCategory,
  updateCategory,
  deleteCategory,
})

/**
 * Default export is the same `categories` ref so views can do
 *   `import categories from '@/utils/categories'`
 * without losing reactivity.
 */
export default categories

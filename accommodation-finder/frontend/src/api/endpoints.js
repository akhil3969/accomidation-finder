import client from './client';

/** Every server call the app makes, in one place. */

// ------------------------------------------------------------------ auth
export const authApi = {
  register: (payload) => client.post('/api/auth/register', payload).then((r) => r.data),
  login: (payload) => client.post('/api/auth/login', payload).then((r) => r.data),
  me: () => client.get('/api/auth/me').then((r) => r.data),
};

// ------------------------------------------------------------------ rooms
export const roomApi = {
  search: (params) => client.get('/api/rooms', { params }).then((r) => r.data),
  byId: (id) => client.get(`/api/rooms/${id}`).then((r) => r.data),
  cities: () => client.get('/api/rooms/cities').then((r) => r.data),
  mine: (params) => client.get('/api/rooms/mine', { params }).then((r) => r.data),
  myStats: () => client.get('/api/rooms/mine/stats').then((r) => r.data),
  create: (payload) => client.post('/api/rooms', payload).then((r) => r.data),
  update: (id, payload) => client.put(`/api/rooms/${id}`, payload).then((r) => r.data),
  setAvailability: (id, available) =>
    client.patch(`/api/rooms/${id}/availability`, { available }).then((r) => r.data),
  remove: (id) => client.delete(`/api/rooms/${id}`).then((r) => r.data),
};

// --------------------------------------------------------------- bookings
export const bookingApi = {
  request: (payload) => client.post('/api/bookings', payload).then((r) => r.data),
  mine: (params) => client.get('/api/bookings/mine', { params }).then((r) => r.data),
  requests: (params) => client.get('/api/bookings/requests', { params }).then((r) => r.data),
  byId: (id) => client.get(`/api/bookings/${id}`).then((r) => r.data),
  decide: (id, status, response) =>
    client.patch(`/api/bookings/${id}/decision`, { status, response }).then((r) => r.data),
  cancel: (id) => client.patch(`/api/bookings/${id}/cancel`).then((r) => r.data),
  ranges: (roomId) => client.get(`/api/bookings/room/${roomId}/ranges`).then((r) => r.data),
};

// --------------------------------------------------------------- messages
export const messageApi = {
  send: (payload) => client.post('/api/messages', payload).then((r) => r.data),
  inbox: () => client.get('/api/messages/inbox').then((r) => r.data),
  conversation: (roomId, userId) =>
    client.get('/api/messages/conversation', { params: { roomId, userId } }).then((r) => r.data),
  unreadCount: () => client.get('/api/messages/unread-count').then((r) => r.data),
};

// -------------------------------------------------------------- favorites
export const favoriteApi = {
  list: () => client.get('/api/favorites').then((r) => r.data),
  ids: () => client.get('/api/favorites/ids').then((r) => r.data),
  toggle: (roomId) => client.post(`/api/favorites/${roomId}/toggle`).then((r) => r.data),
};

// ---------------------------------------------------------------- reviews
export const reviewApi = {
  forRoom: (roomId) => client.get(`/api/reviews/room/${roomId}`).then((r) => r.data),
  create: (roomId, payload) => client.post(`/api/reviews/room/${roomId}`, payload).then((r) => r.data),
  remove: (id) => client.delete(`/api/reviews/${id}`).then((r) => r.data),
};

// ------------------------------------------------------------------ users
export const userApi = {
  me: () => client.get('/api/users/me').then((r) => r.data),
  updateMe: (payload) => client.put('/api/users/me', payload).then((r) => r.data),
  changePassword: (payload) => client.post('/api/users/me/password', payload).then((r) => r.data),
  becomeLandlord: () => client.post('/api/users/me/become-landlord').then((r) => r.data),
};

// ------------------------------------------------------------------ admin
export const adminApi = {
  users: () => client.get('/api/admin/users').then((r) => r.data),
  stats: () => client.get('/api/admin/stats').then((r) => r.data),
  setEnabled: (id, enabled) =>
    client.patch(`/api/admin/users/${id}/enabled`, { enabled }).then((r) => r.data),
  setRole: (id, role) => client.patch(`/api/admin/users/${id}/role`, { role }).then((r) => r.data),
  deleteUser: (id) => client.delete(`/api/admin/users/${id}`).then((r) => r.data),
  analytics: (days) => client.get('/api/admin/analytics', { params: { days } }).then((r) => r.data),
  rooms: (params) => client.get('/api/admin/rooms', { params }).then((r) => r.data),
  deleteRoom: (id) => client.delete(`/api/admin/rooms/${id}`).then((r) => r.data),
  rescoreAll: () => client.post('/api/admin/rooms/rescore').then((r) => r.data),
};

// ------------------------------------------------------------- housing aid
export const aidApi = {
  forRoom: (roomId) => client.get(`/api/aid/room/${roomId}`).then((r) => r.data),
  summary: (roomId) => client.get(`/api/aid/room/${roomId}/summary`).then((r) => r.data),
  estimate: (payload) => client.post('/api/aid/estimate', payload).then((r) => r.data),
  visale: (payload) => client.post('/api/aid/visale', payload).then((r) => r.data),
  visaleForRoom: (roomId) => client.get(`/api/aid/visale/room/${roomId}`).then((r) => r.data),
  zone: (postcode) => client.get('/api/aid/zone', { params: { postcode } }).then((r) => r.data),
};

// ---------------------------------------------------------------- newcomer
export const newcomerApi = {
  profile: () => client.get('/api/newcomer/profile').then((r) => r.data),
  saveProfile: (payload) => client.put('/api/newcomer/profile', payload).then((r) => r.data),
  checklist: () => client.get('/api/newcomer/checklist').then((r) => r.data),
  toggleChecklist: (itemId, done, note) =>
    client.patch(`/api/newcomer/checklist/${itemId}`, { done, note }).then((r) => r.data),
  neighbourhood: (roomId) => client.get(`/api/newcomer/neighbourhood/${roomId}`).then((r) => r.data),
  safety: (roomId) => client.get(`/api/newcomer/safety/${roomId}`).then((r) => r.data),
  guides: () => client.get('/api/newcomer/guides').then((r) => r.data),
  guide: (slug) => client.get(`/api/newcomer/guides/${slug}`).then((r) => r.data),
};

// ----------------------------------------------------------------- reports
export const reportApi = {
  reasons: () => client.get('/api/reports/reasons').then((r) => r.data),
  submit: (payload) => client.post('/api/reports', payload).then((r) => r.data),
};

// ------------------------------------------------------- admin: moderation
export const moderationApi = {
  reports: (params) => client.get('/api/admin/moderation/reports', { params }).then((r) => r.data),
  resolve: (id, payload) =>
    client.patch(`/api/admin/moderation/reports/${id}`, payload).then((r) => r.data),
  verifications: () => client.get('/api/admin/moderation/verifications').then((r) => r.data),
  decideVerification: (userId, approve, note) =>
    client.patch(`/api/admin/moderation/verifications/${userId}`, { approve, note }).then((r) => r.data),
  setHidden: (roomId, hidden, reason) =>
    client.patch(`/api/admin/moderation/rooms/${roomId}/hidden`, { hidden, reason }).then((r) => r.data),
  setVerified: (roomId, verified) =>
    client.patch(`/api/admin/moderation/rooms/${roomId}/verified`, { verified }).then((r) => r.data),
  highRisk: () => client.get('/api/admin/moderation/high-risk').then((r) => r.data),
  audit: (params) => client.get('/api/admin/moderation/audit', { params }).then((r) => r.data),
};

// ---------------------------------------------------------- admin: content
export const contentApi = {
  guides: () => client.get('/api/admin/content/guides').then((r) => r.data),
  createGuide: (payload) => client.post('/api/admin/content/guides', payload).then((r) => r.data),
  updateGuide: (id, payload) => client.put(`/api/admin/content/guides/${id}`, payload).then((r) => r.data),
  publishGuide: (id, published) =>
    client.patch(`/api/admin/content/guides/${id}/published`, { published }).then((r) => r.data),
  deleteGuide: (id) => client.delete(`/api/admin/content/guides/${id}`).then((r) => r.data),
  checklist: () => client.get('/api/admin/content/checklist').then((r) => r.data),
  createItem: (payload) => client.post('/api/admin/content/checklist', payload).then((r) => r.data),
  updateItem: (id, payload) => client.put(`/api/admin/content/checklist/${id}`, payload).then((r) => r.data),
  deleteItem: (id) => client.delete(`/api/admin/content/checklist/${id}`).then((r) => r.data),
  aidParameters: () => client.get('/api/admin/content/aid-parameters').then((r) => r.data),
  updateAidParameter: (id, payload) =>
    client.put(`/api/admin/content/aid-parameters/${id}`, payload).then((r) => r.data),
};

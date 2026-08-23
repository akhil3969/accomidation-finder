import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { messageApi } from '../api/endpoints';
import { useAuth } from './AuthContext';
import { useSubscription } from './RealtimeContext';

const InboxContext = createContext(null);

/**
 * The unread-message counter, owned in one place.
 *
 * The navbar badge used to only ever go up: it counted incoming sockets frames
 * but nothing told it that opening a thread had marked those messages read on
 * the server, so the badge stayed lit until a full page reload. Keeping the
 * count here lets the messages page hand it back down to zero the moment a
 * thread is opened.
 */
export function InboxProvider({ children }) {
  const { isAuthenticated, user } = useAuth();
  const [unread, setUnread] = useState(0);

  const refresh = useCallback(async () => {
    if (!isAuthenticated) {
      setUnread(0);
      return 0;
    }
    try {
      const data = await messageApi.unreadCount();
      const count = data.unread ?? 0;
      setUnread(count);
      return count;
    } catch {
      // A failed badge refresh is not worth a toast; the number simply stays.
      return 0;
    }
  }, [isAuthenticated]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  // Anything addressed to us bumps the badge straight away, ahead of the
  // server round trip.
  useSubscription(isAuthenticated ? '/user/queue/messages' : null, (message) => {
    if (message?.recipient?.id === user?.id) {
      setUnread((count) => count + 1);
    }
  });

  const value = useMemo(
    () => ({ unread, refresh, setUnread }),
    [unread, refresh],
  );

  return <InboxContext.Provider value={value}>{children}</InboxContext.Provider>;
}

export function useInbox() {
  const context = useContext(InboxContext);
  if (!context) {
    throw new Error('useInbox must be used inside <InboxProvider>');
  }
  return context;
}

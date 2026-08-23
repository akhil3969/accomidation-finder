import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { messageApi } from '../api/endpoints';
import { toMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useRealtime, useSubscription } from '../context/RealtimeContext';
import { useInbox } from '../context/InboxContext';
import { useToast } from '../context/ToastContext';
import useDocumentTitle from '../hooks/useDocumentTitle';
import PageTransition from '../components/PageTransition';
import Loader from '../components/Loader';
import EmptyState from '../components/EmptyState';
import { bubbleVariants } from '../motion/tokens';
import { formatDateTime, initials, timeAgo } from '../utils/format';

/** Same thread? Compares the pair that identifies one, not object identity. */
function sameThread(a, b) {
  return Boolean(a && b && a.roomId === b.roomId && a.userId === b.userId);
}

export default function Messages() {
  useDocumentTitle('Messages');

  const { user } = useAuth();
  const { publish, connected } = useRealtime();
  const { refresh: refreshUnread } = useInbox();
  const toast = useToast();
  const location = useLocation();

  const [threads, setThreads] = useState([]);
  const [active, setActive] = useState(null); // { roomId, userId, name, roomTitle }
  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadingThread, setLoadingThread] = useState(false);
  const [sending, setSending] = useState(false);
  const bodyRef = useRef(null);

  const loadThreads = useCallback(async () => {
    try {
      const inbox = await messageApi.inbox();
      setThreads(inbox);
      return inbox;
    } catch (error) {
      toast.error(toMessage(error));
      return [];
    } finally {
      setLoading(false);
    }
    // A new toast object every render would restart this on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Open the thread requested from a room page, otherwise the newest one.
  useEffect(() => {
    let cancelled = false;
    loadThreads().then((inbox) => {
      if (cancelled) return;
      const requested = location.state;
      if (requested?.roomId && requested?.userId) {
        const existing = inbox.find(
          (thread) => thread.roomId === requested.roomId && thread.otherUser?.id === requested.userId,
        );
        setActive({
          roomId: requested.roomId,
          userId: requested.userId,
          name: existing?.otherUser?.name || 'Landlord',
          roomTitle: existing?.roomTitle || requested.roomTitle || 'Listing',
        });
      } else if (inbox.length) {
        setActive({
          roomId: inbox[0].roomId,
          userId: inbox[0].otherUser.id,
          name: inbox[0].otherUser.name,
          roomTitle: inbox[0].roomTitle,
        });
      }
    });
    return () => {
      cancelled = true;
    };
  }, [loadThreads, location.state]);

  // Load the selected conversation. Fetching it also marks it read on the
  // server, so the navbar badge has to be told to catch up - it used to keep
  // counting messages the user had already read.
  useEffect(() => {
    if (!active) return undefined;
    let cancelled = false;
    setLoadingThread(true);
    messageApi
      .conversation(active.roomId, active.userId)
      .then((thread) => {
        if (cancelled) return;
        setMessages(thread);
        setThreads((current) =>
          current.map((item) =>
            item.roomId === active.roomId && item.otherUser?.id === active.userId
              ? { ...item, unreadCount: 0 }
              : item,
          ),
        );
        refreshUnread();
      })
      .catch((error) => {
        if (!cancelled) toast.error(toMessage(error));
      })
      .finally(() => {
        if (!cancelled) setLoadingThread(false);
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active?.roomId, active?.userId]);

  // Live delivery for whichever thread is open, plus an inbox refresh.
  useSubscription('/user/queue/messages', (message) => {
    const involvesActive =
      active &&
      message.roomId === active.roomId &&
      (message.sender?.id === active.userId || message.recipient?.id === active.userId);

    if (involvesActive) {
      setMessages((current) => {
        if (current.some((item) => item.id === message.id)) return current;
        // Replace our own optimistic copy rather than showing it twice.
        const withoutPending = current.filter(
          (item) => !(item.pending && item.content === message.content),
        );
        return [...withoutPending, message];
      });
    }
    loadThreads();
  });

  // useLayoutEffect so the jump to the bottom happens in the same frame the
  // message is painted, rather than as a visible scroll afterwards.
  useLayoutEffect(() => {
    const node = bodyRef.current;
    if (node) node.scrollTop = node.scrollHeight;
  }, [messages]);

  async function send(event) {
    event.preventDefault();
    const content = draft.trim();
    if (!content || !active || sending) return;

    const payload = { recipientId: active.userId, roomId: active.roomId, content };
    const optimistic = {
      id: `pending-${Date.now()}`,
      pending: true,
      content,
      sentAt: new Date().toISOString(),
      sender: { id: user?.id, name: user?.name },
      recipient: { id: active.userId },
      roomId: active.roomId,
    };

    setDraft('');
    setMessages((current) => [...current, optimistic]);
    setSending(true);

    try {
      // Prefer the socket; the server echoes the stored message back to both
      // sides, which is what replaces the optimistic copy above.
      if (connected && publish('/app/chat.send', payload)) return;

      const saved = await messageApi.send(payload);
      setMessages((current) => current.map((item) => (item.id === optimistic.id ? saved : item)));
      loadThreads();
    } catch (error) {
      setMessages((current) => current.filter((item) => item.id !== optimistic.id));
      setDraft(content);
      toast.error(toMessage(error));
    } finally {
      setSending(false);
    }
  }

  if (loading) {
    return (
      <PageTransition>
        <Loader label="Loading your messages…" />
      </PageTransition>
    );
  }

  return (
    <PageTransition className="page">
      <div className="page-header">
        <div>
          <h1>Messages</h1>
          <p>
            {connected
              ? 'Connected — messages arrive instantly.'
              : 'Offline — messages will be sent over HTTP.'}
          </p>
        </div>
        <div className="live-strip">
          <span className={`status-dot ${connected ? 'is-live' : 'is-down'}`} />
          {connected ? 'Live' : 'Reconnecting…'}
        </div>
      </div>

      {threads.length === 0 && !active ? (
        <EmptyState
          icon="&#128172;"
          title="No conversations yet"
          description="Open a listing and message the landlord to start one."
        />
      ) : (
        <div className="chat-layout">
          <aside className="thread-list">
            {threads.map((thread) => {
              const isActive = sameThread(active, {
                roomId: thread.roomId,
                userId: thread.otherUser?.id,
              });
              return (
                <button
                  key={`${thread.roomId}-${thread.otherUser?.id}`}
                  type="button"
                  className={`thread ${isActive ? 'is-active' : ''}`}
                  aria-current={isActive ? 'true' : undefined}
                  onClick={() =>
                    setActive({
                      roomId: thread.roomId,
                      userId: thread.otherUser.id,
                      name: thread.otherUser.name,
                      roomTitle: thread.roomTitle,
                    })
                  }
                >
                  <span className="avatar">{initials(thread.otherUser?.name)}</span>
                  <span className="thread__text">
                    <span className="thread__name">{thread.otherUser?.name}</span>
                    <span className="thread__preview">{thread.lastMessage}</span>
                    <span className="thread__preview muted">{thread.roomTitle}</span>
                  </span>
                  <span className="thread__side">
                    <span className="small muted">{timeAgo(thread.lastMessageAt)}</span>
                    <AnimatePresence>
                      {thread.unreadCount > 0 && (
                        <motion.span
                          className="badge badge--danger"
                          initial={{ scale: 0.5, opacity: 0 }}
                          animate={{ scale: 1, opacity: 1 }}
                          exit={{ scale: 0.5, opacity: 0 }}
                        >
                          {thread.unreadCount}
                        </motion.span>
                      )}
                    </AnimatePresence>
                  </span>
                </button>
              );
            })}
          </aside>

          <section className="chat-panel">
            {active ? (
              <>
                <header className="chat-panel__header">
                  <span className="avatar">{initials(active.name)}</span>
                  <div style={{ minWidth: 0 }}>
                    <strong>{active.name}</strong>
                    <div className="small muted">{active.roomTitle}</div>
                  </div>
                </header>

                <div className="chat-panel__body" ref={bodyRef}>
                  {loadingThread && messages.length === 0 && (
                    <p className="small muted center">Loading conversation…</p>
                  )}
                  {!loadingThread && messages.length === 0 && (
                    <p className="small muted center">No messages yet. Say hello.</p>
                  )}

                  <AnimatePresence initial={false}>
                    {messages.map((message) => {
                      const mine = message.sender?.id === user?.id;
                      return (
                        <motion.div
                          key={message.id}
                          layout="position"
                          variants={bubbleVariants}
                          initial="initial"
                          animate="animate"
                          exit="exit"
                          className={`bubble ${mine ? 'bubble--out' : 'bubble--in'} ${
                            message.pending ? 'bubble--pending' : ''
                          }`}
                        >
                          {message.content}
                          <span className="bubble__time">
                            {message.pending ? 'Sending…' : formatDateTime(message.sentAt)}
                          </span>
                        </motion.div>
                      );
                    })}
                  </AnimatePresence>
                </div>

                <form className="chat-panel__composer" onSubmit={send}>
                  <input
                    type="text"
                    placeholder="Write a message…"
                    value={draft}
                    maxLength={2000}
                    onChange={(event) => setDraft(event.target.value)}
                    aria-label="Message"
                  />
                  <button
                    type="submit"
                    className="btn btn--primary"
                    disabled={!draft.trim() || sending}
                  >
                    Send
                  </button>
                </form>
              </>
            ) : (
              <div className="loader">
                <span className="small muted">Pick a conversation on the left.</span>
              </div>
            )}
          </section>
        </div>
      )}
    </PageTransition>
  );
}

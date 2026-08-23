import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getToken } from '../api/client';
import { useAuth } from './AuthContext';

/**
 * Owns the single STOMP connection for the whole app.
 *
 * Components call `subscribe(destination, handler)` in an effect and get an
 * unsubscribe function back; the provider re-registers every handler after a
 * reconnect, so a dropped network never leaves a component deaf.
 */
const RealtimeContext = createContext(null);

const WS_URL = import.meta.env.VITE_WS_URL || '/ws';

export function RealtimeProvider({ children }) {
  const { user } = useAuth();
  const [connected, setConnected] = useState(false);
  const clientRef = useRef(null);
  const handlersRef = useRef(new Map());      // destination -> Set<handler>
  const subscriptionsRef = useRef(new Map()); // destination -> StompSubscription

  const bind = useCallback((destination) => {
    const client = clientRef.current;
    if (!client || !client.connected || subscriptionsRef.current.has(destination)) {
      return;
    }
    const subscription = client.subscribe(destination, (frame) => {
      let payload = frame.body;
      try {
        payload = JSON.parse(frame.body);
      } catch {
        /* a non-JSON frame is passed through untouched */
      }
      (handlersRef.current.get(destination) || new Set()).forEach((handler) => {
        try {
          handler(payload);
        } catch (error) {
          console.error('Realtime handler failed for', destination, error);
        }
      });
    });
    subscriptionsRef.current.set(destination, subscription);
  }, []);

  useEffect(() => {
    const token = getToken();
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {},
      onConnect: () => {
        setConnected(true);
        subscriptionsRef.current.clear();
        handlersRef.current.forEach((_handlers, destination) => bind(destination));
      },
      onWebSocketClose: () => setConnected(false),
      onStompError: (frame) => {
        setConnected(false);
        console.warn('STOMP error', frame.headers?.message);
      },
    });

    clientRef.current = client;
    client.activate();

    // Captured now: by the time this cleanup runs the ref may already point at
    // the next connection's map, and we would tear down the wrong sockets.
    const subscriptions = subscriptionsRef.current;

    return () => {
      subscriptions.forEach((subscription) => {
        try {
          subscription.unsubscribe();
        } catch {
          /* already gone */
        }
      });
      subscriptions.clear();
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
    // Reconnect with fresh credentials whenever the signed-in identity changes.
  }, [user?.id, bind]);

  const subscribe = useCallback(
    (destination, handler) => {
      if (!handlersRef.current.has(destination)) {
        handlersRef.current.set(destination, new Set());
      }
      handlersRef.current.get(destination).add(handler);
      bind(destination);

      return () => {
        const handlers = handlersRef.current.get(destination);
        if (!handlers) return;
        handlers.delete(handler);
        if (handlers.size === 0) {
          handlersRef.current.delete(destination);
          const subscription = subscriptionsRef.current.get(destination);
          if (subscription) {
            try {
              subscription.unsubscribe();
            } catch {
              /* connection already closed */
            }
            subscriptionsRef.current.delete(destination);
          }
        }
      };
    },
    [bind],
  );

  const publish = useCallback((destination, body) => {
    const client = clientRef.current;
    if (client && client.connected) {
      client.publish({ destination, body: JSON.stringify(body) });
      return true;
    }
    return false;
  }, []);

  const value = useMemo(() => ({ connected, subscribe, publish }), [connected, subscribe, publish]);

  return <RealtimeContext.Provider value={value}>{children}</RealtimeContext.Provider>;
}

export function useRealtime() {
  const context = useContext(RealtimeContext);
  if (!context) {
    throw new Error('useRealtime must be used inside <RealtimeProvider>');
  }
  return context;
}

/** Subscribe to one destination for the lifetime of a component. */
export function useSubscription(destination, handler, deps = []) {
  const { subscribe } = useRealtime();
  const handlerRef = useRef(handler);
  handlerRef.current = handler;

  useEffect(() => {
    if (!destination) return undefined;
    return subscribe(destination, (payload) => handlerRef.current(payload));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [destination, subscribe, ...deps]);
}
